package com.taskmanagement.adapter.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.domain.ai.AiPriorityProposal;
import com.taskmanagement.domain.ai.AiPromptConfig;
import com.taskmanagement.domain.ai.AiTaskBreakdown;
import com.taskmanagement.domain.ai.AiTaskSummary;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AnthropicCompatibleAiAdapter Unit Tests")
class AnthropicCompatibleAiAdapterTest {

    private HttpClient httpClient;
    private HttpResponse<String> httpResponse;
    private ObjectMapper objectMapper;
    private AnthropicCompatibleAiAdapter adapter;
    private Project project;
    private Task task;
    private AiPromptConfig config;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);
        objectMapper = new ObjectMapper();
        adapter = new AnthropicCompatibleAiAdapter(
            "sk-ant-test-key",
            "https://api.anthropic.com/v1",
            "claude-3-5-sonnet-20241022",
            "2023-06-01",
            4096,
            15,
            Map.of("X-Custom-Tracking", "track-123"),
            objectMapper,
            httpClient
        );
        project = Project.create("DevOps", "Infrastructure tasks");
        task = Task.create("Deploy k8s cluster", "Setup production cluster", null, Priority.HIGH, project.getId());
        config = AiPromptConfig.defaultConfig();
    }

    @Test
    @DisplayName("isConfigured returns true when apiKey is present")
    void shouldBeConfiguredWithApiKey() {
        assertTrue(adapter.isConfigured());
        AnthropicCompatibleAiAdapter unconfigured = new AnthropicCompatibleAiAdapter("", "https://api.anthropic.com/v1", null, 10, objectMapper, httpClient);
        assertFalse(unconfigured.isConfigured());
    }

    @Test
    @DisplayName("Successfully parses Anthropic task breakdown response")
    @SuppressWarnings("unchecked")
    void shouldGenerateTaskBreakdownSuccessfully() throws Exception {
        String anthropicResponse = """
            {
              "id": "msg_01X9z",
              "type": "message",
              "role": "assistant",
              "model": "claude-3-5-sonnet-20241022",
              "content": [
                {
                  "type": "text",
                  "text": "```json\\n{\\n  \\"subtasks\\": [\\n    {\\"title\\": \\"Provision VPC\\", \\"explanation\\": \\"Network setup\\"}\\n  ],\\n  \\"estimatedDeadline\\": \\"2026-11-01T10:00:00Z\\",\\n  \\"reasoning\\": \\"Standard infrastructure deployment\\"\\n}\\n```"
                }
              ]
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(anthropicResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        AiTaskBreakdown breakdown = adapter.generateTaskBreakdown(task, project, config);

        assertNotNull(breakdown);
        assertEquals(1, breakdown.subtasks().size());
        assertEquals("Provision VPC", breakdown.subtasks().get(0).title());
        assertNotNull(breakdown.estimatedDeadline());
        assertEquals("Standard infrastructure deployment", breakdown.reasoning());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any());
        HttpRequest sent = requestCaptor.getValue();
        assertEquals("https://api.anthropic.com/v1/messages", sent.uri().toString());
        assertEquals("sk-ant-test-key", sent.headers().firstValue("x-api-key").orElse(""));
        assertEquals("2023-06-01", sent.headers().firstValue("anthropic-version").orElse(""));
        assertEquals("track-123", sent.headers().firstValue("X-Custom-Tracking").orElse(""));
    }

    @Test
    @DisplayName("Successfully parses Anthropic priority suggestion")
    @SuppressWarnings("unchecked")
    void shouldSuggestPrioritySuccessfully() throws Exception {
        String anthropicResponse = """
            {
              "id": "msg_02",
              "content": [
                {
                  "type": "text",
                  "text": "{\\"priority\\": \\"HIGH\\", \\"reasoning\\": \\"Production deployment\\", \\"confidence\\": 0.95}"
                }
              ]
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(anthropicResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        AiPriorityProposal proposal = adapter.suggestPriority(task, project, config);
        assertEquals(Priority.HIGH, proposal.priority());
        assertEquals("Production deployment", proposal.reasoning());
        assertEquals(0.95, proposal.confidence());
    }

    @Test
    @DisplayName("Successfully parses Anthropic task summary")
    @SuppressWarnings("unchecked")
    void shouldGenerateTaskSummarySuccessfully() throws Exception {
        String anthropicResponse = """
            {
              "id": "msg_03",
              "content": [
                {
                  "type": "text",
                  "text": "{\\"summary\\": \\"Cluster setup in progress\\", \\"keyHighlights\\": [\\"VPC created\\"], \\"suggestedNextStep\\": \\"Apply terraform\\"}"
                }
              ]
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(anthropicResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        AiTaskSummary summary = adapter.generateTaskSummary(task, project, config);
        assertEquals("Cluster setup in progress", summary.summary());
        assertEquals(1, summary.keyHighlights().size());
        assertEquals("Apply terraform", summary.suggestedNextStep());
    }

    @Test
    @DisplayName("Throws AiCommunicationException on HTTP error status")
    @SuppressWarnings("unchecked")
    void shouldThrowExceptionOnHttpError() throws Exception {
        when(httpResponse.statusCode()).thenReturn(401);
        when(httpResponse.body()).thenReturn("{\"error\": {\"type\": \"authentication_error\", \"message\": \"invalid x-api-key\"}}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        assertThrows(AiCommunicationException.class, () ->
            adapter.generateTaskBreakdown(task, project, config)
        );
    }
}
