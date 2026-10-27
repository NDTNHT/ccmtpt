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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OpenAiCompatibleAiAdapter Unit Tests")
class OpenAiCompatibleAiAdapterTest {

    private HttpClient httpClient;
    private HttpResponse<String> httpResponse;
    private ObjectMapper objectMapper;
    private OpenAiCompatibleAiAdapter adapter;
    private Project project;
    private Task task;
    private AiPromptConfig config;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);
        objectMapper = new ObjectMapper();
        adapter = new OpenAiCompatibleAiAdapter(
            "test-api-key",
            "https://api.openai.com/v1",
            "gpt-4o-mini",
            15,
            objectMapper,
            httpClient
        );
        project = Project.create("Core", "Core project");
        task = Task.create("Implement feature", "Build the feature", null, Priority.MEDIUM, project.getId());
        config = AiPromptConfig.defaultConfig();
    }

    @Test
    @DisplayName("isConfigured returns true when apiKey is present or host is localhost")
    void testIsConfigured() {
        assertTrue(adapter.isConfigured());

        OpenAiCompatibleAiAdapter blankKey = new OpenAiCompatibleAiAdapter("", "https://api.openai.com/v1", "m", 10, objectMapper, httpClient);
        assertFalse(blankKey.isConfigured());

        OpenAiCompatibleAiAdapter localOllama = new OpenAiCompatibleAiAdapter("", "http://localhost:11434/v1", "llama3", 10, objectMapper, httpClient);
        assertTrue(localOllama.isConfigured());
    }

    @Test
    @DisplayName("generateTaskBreakdown sends valid request and parses choices response")
    @SuppressWarnings("unchecked")
    void shouldGenerateTaskBreakdown() throws Exception {
        String mockResponseJson = """
            {
              "choices": [
                {
                  "message": {
                    "content": "{\\"subtasks\\": [{\\"title\\": \\"Write unit tests\\", \\"explanation\\": \\"TDD step\\"}], \\"estimatedDeadline\\": \\"2026-11-01T12:00:00Z\\", \\"reasoning\\": \\"Clear modular work\\"}"
                  }
                }
              ]
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockResponseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        AiTaskBreakdown breakdown = adapter.generateTaskBreakdown(task, project, config);

        assertNotNull(breakdown);
        assertEquals(1, breakdown.subtasks().size());
        assertEquals("Write unit tests", breakdown.subtasks().get(0).title());
        assertEquals("TDD step", breakdown.subtasks().get(0).explanation());
        assertNotNull(breakdown.estimatedDeadline());
        assertEquals("Clear modular work", breakdown.reasoning());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        HttpRequest sent = captor.getValue();
        assertEquals("https://api.openai.com/v1/chat/completions", sent.uri().toString());
        assertEquals("Bearer test-api-key", sent.headers().firstValue("Authorization").orElse(""));
    }

    @Test
    @DisplayName("suggestPriority parses priority proposal correctly")
    @SuppressWarnings("unchecked")
    void shouldSuggestPriority() throws Exception {
        String mockResponseJson = """
            {
              "choices": [
                {
                  "message": {
                    "content": "{\\"priority\\": \\"HIGH\\", \\"reasoning\\": \\"Critical feature for release\\", \\"confidence\\": 0.95}"
                  }
                }
              ]
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockResponseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        AiPriorityProposal proposal = adapter.suggestPriority(task, project, config);

        assertNotNull(proposal);
        assertEquals(Priority.HIGH, proposal.priority());
        assertEquals("Critical feature for release", proposal.reasoning());
        assertEquals(0.95, proposal.confidence());
    }

    @Test
    @DisplayName("generateTaskSummary parses summary correctly")
    @SuppressWarnings("unchecked")
    void shouldGenerateTaskSummary() throws Exception {
        String mockResponseJson = """
            {
              "choices": [
                {
                  "message": {
                    "content": "{\\"summary\\": \\"Implement feature concisely\\", \\"keyHighlights\\": [\\"Feature ready\\"], \\"suggestedNextStep\\": \\"Deploy to staging\\"}"
                  }
                }
              ]
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockResponseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        AiTaskSummary summary = adapter.generateTaskSummary(task, project, config);

        assertNotNull(summary);
        assertEquals("Implement feature concisely", summary.summary());
        assertEquals(1, summary.keyHighlights().size());
        assertEquals("Deploy to staging", summary.suggestedNextStep());
    }

    @Test
    @DisplayName("Throws AiCommunicationException on HTTP error status")
    @SuppressWarnings("unchecked")
    void shouldThrowOnHttpError() throws Exception {
        when(httpResponse.statusCode()).thenReturn(401);
        when(httpResponse.body()).thenReturn("{\"error\": \"Invalid API key\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        assertThrows(AiCommunicationException.class, () -> adapter.generateTaskBreakdown(task, project, config));
    }
}
