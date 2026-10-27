package com.taskmanagement.adapter.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.domain.ai.AiCustomProviderConfig;
import com.taskmanagement.domain.ai.AiPromptConfig;
import com.taskmanagement.domain.ai.AiTaskBreakdown;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CustomAiAdapter Unit Tests")
class CustomAiAdapterTest {

    private HttpClient httpClient;
    private HttpResponse<String> httpResponse;
    private ObjectMapper objectMapper;
    private Project project;
    private Task task;
    private AiPromptConfig config;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);
        objectMapper = new ObjectMapper();
        project = Project.create("CustomProject", "Project for custom tests");
        task = Task.create("Custom Task", "Task description", null, Priority.MEDIUM, project.getId());
        config = AiPromptConfig.defaultConfig();
    }

    @Test
    @DisplayName("Routes correctly to OpenAI format with custom endpoint and headers")
    @SuppressWarnings("unchecked")
    void shouldRouteToOpenAiFormat() throws Exception {
        CustomAiAdapter adapter = new CustomAiAdapter(
            "openai",
            "https://my-gpu-cluster.internal/v1",
            "my-key",
            "llama-3.1-70b",
            30,
            4096,
            Map.of("X-Organization", "MyTeam"),
            objectMapper,
            httpClient
        );

        assertTrue(adapter.isConfigured());
        assertEquals("openai", adapter.getFormat());
        assertTrue(adapter.getDelegate() instanceof OpenAiCompatibleAiAdapter);

        String openAiResponse = """
            {
              "choices": [
                {
                  "message": {
                    "content": "{\\"subtasks\\": [{\\"title\\": \\"Step 1\\", \\"explanation\\": \\"Init\\"}], \\"reasoning\\": \\"Custom breakdown\\"}"
                  }
                }
              ]
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(openAiResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        AiTaskBreakdown breakdown = adapter.generateTaskBreakdown(task, project, config);
        assertNotNull(breakdown);
        assertEquals(1, breakdown.subtasks().size());
        assertEquals("Step 1", breakdown.subtasks().get(0).title());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any());
        HttpRequest sent = requestCaptor.getValue();
        assertEquals("https://my-gpu-cluster.internal/v1/chat/completions", sent.uri().toString());
        assertEquals("Bearer my-key", sent.headers().firstValue("Authorization").orElse(""));
        assertEquals("MyTeam", sent.headers().firstValue("X-Organization").orElse(""));
    }

    @Test
    @DisplayName("Routes correctly to Anthropic format with custom endpoint and headers")
    @SuppressWarnings("unchecked")
    void shouldRouteToAnthropicFormat() throws Exception {
        CustomAiAdapter adapter = new CustomAiAdapter(
            "anthropic",
            "https://my-anthropic-proxy.corp/v1",
            "sk-ant-custom",
            "claude-3-5-haiku",
            30,
            2048,
            Map.of("X-Proxy-Auth", "proxy-token"),
            objectMapper,
            httpClient
        );

        assertTrue(adapter.isConfigured());
        assertEquals("anthropic", adapter.getFormat());
        assertTrue(adapter.getDelegate() instanceof AnthropicCompatibleAiAdapter);

        String anthropicResponse = """
            {
              "content": [
                {
                  "type": "text",
                  "text": "{\\"subtasks\\": [{\\"title\\": \\"Claude Step\\", \\"explanation\\": \\"Done\\"}], \\"reasoning\\": \\"Anthropic custom breakdown\\"}"
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
        assertEquals("Claude Step", breakdown.subtasks().get(0).title());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any());
        HttpRequest sent = requestCaptor.getValue();
        assertEquals("https://my-anthropic-proxy.corp/v1/messages", sent.uri().toString());
        assertEquals("sk-ant-custom", sent.headers().firstValue("x-api-key").orElse(""));
        assertEquals("proxy-token", sent.headers().firstValue("X-Proxy-Auth").orElse(""));
    }

    @Test
    @DisplayName("fromConfig factory creates functional custom adapter")
    void shouldCreateFromDomainConfig() {
        AiCustomProviderConfig domainConfig = AiCustomProviderConfig.of(
            "anthropic",
            "https://custom-gateway.io/v1",
            "key123",
            "custom-model",
            Map.of("x-env", "staging")
        );

        CustomAiAdapter adapter = CustomAiAdapter.fromConfig(domainConfig, objectMapper, httpClient);
        assertTrue(adapter.isConfigured());
        assertEquals("anthropic", adapter.getFormat());
        assertEquals("https://custom-gateway.io/v1", adapter.getBaseUrl());
        assertEquals("custom-model", adapter.getModel());
    }

    @Test
    @DisplayName("isConfigured returns false when baseUrl is empty")
    void shouldReportUnconfiguredWhenEmpty() {
        CustomAiAdapter empty = new CustomAiAdapter("openai", "", "", "", 30, 4096, Map.of(), objectMapper, httpClient);
        assertFalse(empty.isConfigured());
    }
}
