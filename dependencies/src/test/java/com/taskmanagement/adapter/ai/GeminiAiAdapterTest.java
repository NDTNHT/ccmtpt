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

@DisplayName("GeminiAiAdapter Unit Tests")
class GeminiAiAdapterTest {

    private HttpClient httpClient;
    private HttpResponse<String> httpResponse;
    private ObjectMapper objectMapper;
    private GeminiAiAdapter adapter;
    private Project project;
    private Task task;
    private AiPromptConfig config;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);
        objectMapper = new ObjectMapper();
        adapter = new GeminiAiAdapter(
            "gemini-key-123",
            "https://generativelanguage.googleapis.com/v1beta",
            "gemini-1.5-flash",
            15,
            objectMapper,
            httpClient
        );
        project = Project.create("Ops", "Operations");
        task = Task.create("Setup alerting", "Monitor service latency", null, Priority.HIGH, project.getId());
        config = AiPromptConfig.defaultConfig();
    }

    @Test
    @DisplayName("isConfigured returns true when apiKey is present")
    void testIsConfigured() {
        assertTrue(adapter.isConfigured());

        GeminiAiAdapter empty = new GeminiAiAdapter("", "https://generativelanguage.googleapis.com/v1beta", "m", 10, objectMapper, httpClient);
        assertFalse(empty.isConfigured());
    }

    @Test
    @DisplayName("generateTaskBreakdown sends Gemini REST request and parses candidates")
    @SuppressWarnings("unchecked")
    void shouldGenerateTaskBreakdown() throws Exception {
        String mockGeminiJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\\"subtasks\\": [{\\"title\\": \\"Configure Prometheus alerts\\", \\"explanation\\": \\"Set SLO thresholds\\"}], \\"estimatedDeadline\\": \\"2026-11-05T09:00:00Z\\", \\"reasoning\\": \\"Monitoring plan\\"}"
                      }
                    ]
                  }
                }
              ]
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockGeminiJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        AiTaskBreakdown breakdown = adapter.generateTaskBreakdown(task, project, config);

        assertNotNull(breakdown);
        assertEquals(1, breakdown.subtasks().size());
        assertEquals("Configure Prometheus alerts", breakdown.subtasks().get(0).title());
        assertEquals("Monitoring plan", breakdown.reasoning());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        HttpRequest sent = captor.getValue();
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent", sent.uri().toString());
        assertEquals("gemini-key-123", sent.headers().firstValue("x-goog-api-key").orElse(""));
    }

    @Test
    @DisplayName("suggestPriority parses priority from Gemini JSON response")
    @SuppressWarnings("unchecked")
    void shouldSuggestPriority() throws Exception {
        String mockGeminiJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\\"priority\\": \\"HIGH\\", \\"reasoning\\": \\"Critical alerting infrastructure\\", \\"confidence\\": 0.92}"
                      }
                    ]
                  }
                }
              ]
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockGeminiJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        AiPriorityProposal proposal = adapter.suggestPriority(task, project, config);

        assertNotNull(proposal);
        assertEquals(Priority.HIGH, proposal.priority());
        assertEquals("Critical alerting infrastructure", proposal.reasoning());
        assertEquals(0.92, proposal.confidence());
    }

    @Test
    @DisplayName("generateTaskSummary parses summary from Gemini response")
    @SuppressWarnings("unchecked")
    void shouldGenerateTaskSummary() throws Exception {
        String mockGeminiJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\\"summary\\": \\"Setup alerting on latency\\", \\"keyHighlights\\": [\\"SLO target defined\\"], \\"suggestedNextStep\\": \\"Deploy Alertmanager rules\\"}"
                      }
                    ]
                  }
                }
              ]
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockGeminiJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        AiTaskSummary summary = adapter.generateTaskSummary(task, project, config);

        assertNotNull(summary);
        assertEquals("Setup alerting on latency", summary.summary());
        assertEquals(1, summary.keyHighlights().size());
        assertEquals("Deploy Alertmanager rules", summary.suggestedNextStep());
    }

    @Test
    @DisplayName("Throws AiCommunicationException on HTTP error")
    @SuppressWarnings("unchecked")
    void shouldThrowOnHttpError() throws Exception {
        when(httpResponse.statusCode()).thenReturn(400);
        when(httpResponse.body()).thenReturn("{\"error\": {\"message\": \"API key invalid\"}}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        assertThrows(AiCommunicationException.class, () -> adapter.generateTaskBreakdown(task, project, config));
    }
}
