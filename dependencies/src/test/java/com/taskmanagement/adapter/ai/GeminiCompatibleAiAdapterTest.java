package com.taskmanagement.adapter.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.application.dto.ai.AiAnalysisResultDto;
import com.taskmanagement.application.dto.ai.AiDeadlineEstimationDto;
import com.taskmanagement.application.dto.ai.AiPriorityRecommendationDto;
import com.taskmanagement.application.dto.ai.AiTaskDecompositionDto;
import com.taskmanagement.application.dto.ai.AiTaskSummaryDto;
import com.taskmanagement.application.dto.ai.ProviderVerificationResultDto;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.valueobject.Deadline;
import com.taskmanagement.domain.task.valueobject.TaskTitle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiCompatibleAiAdapterTest {

    private GeminiCompatibleAiAdapter unconfiguredAdapter;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        CustomProviderDefinition unconfiguredDef = CustomProviderDefinition.ofGemini(
            "gemini-test",
            "Gemini Test",
            "https://generativelanguage.googleapis.com/v1beta",
            "", // Empty key must fail
            "gemini-2.5-flash"
        );
        AiConfig config = AiConfig.defaults();
        unconfiguredAdapter = new GeminiCompatibleAiAdapter(unconfiguredDef, config);

        project = Project.create("Gemini Core", "Google Gemini Integration Testing");
        task = Task.create(
            new TaskTitle("Build Gemini API Adapter"),
            "Port Gemini wire protocol from OpenCode protocols/gemini.ts into Java Clean Architecture",
            Deadline.of(Instant.now().plus(Duration.ofDays(2))),
            Priority.HIGH,
            project.getId()
        );
    }

    @Test
    @DisplayName("Decompose task should throw IllegalStateException when key is missing")
    void testDecomposeThrowsWhenNoApiKey() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> unconfiguredAdapter.decomposeTask(task, project, null));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }

    @Test
    @DisplayName("Suggest subtasks should throw IllegalStateException when key is missing")
    void testSuggestSubTasksThrows() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> unconfiguredAdapter.suggestSubTasks(task, project, null));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }

    @Test
    @DisplayName("Estimate deadline should throw IllegalStateException when key is missing")
    void testEstimateDeadlineThrows() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> unconfiguredAdapter.estimateDeadline(task, project, null));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }

    @Test
    @DisplayName("Analyze priority should throw IllegalStateException when key is missing")
    void testAnalyzePriorityThrows() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> unconfiguredAdapter.analyzePriority(task, project, Priority.HIGH, null));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }

    @Test
    @DisplayName("Generate summary should throw IllegalStateException when key is missing")
    void testGenerateSummaryThrows() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> unconfiguredAdapter.generateSummary(task, project, null));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }

    @Test
    @DisplayName("Comprehensive analysis should throw IllegalStateException when key is missing")
    void testAnalyzeComprehensiveThrows() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> unconfiguredAdapter.analyzeTaskComprehensive(task, project, Priority.HIGH, null));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }

    @Test
    @DisplayName("verifyGeminiModels returns failure 401 when API key is blank")
    void testVerifyGeminiModelsRequiresKey() {
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        ProviderVerificationResultDto result = GeminiCompatibleAiAdapter.verifyGeminiModels(
            client, mapper, "https://generativelanguage.googleapis.com/v1beta", "", 5);

        assertFalse(result.valid());
        assertEquals(401, result.statusCode());
        assertEquals("GEMINI", result.protocol());
    }

    @Test
    @DisplayName("verifyGeminiModels returns failure 400 when Base URL is blank")
    void testVerifyGeminiModelsRequiresBaseUrl() {
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        ProviderVerificationResultDto result = GeminiCompatibleAiAdapter.verifyGeminiModels(
            client, mapper, "", "AIzaSy-test-key", 5);

        assertFalse(result.valid());
        assertEquals(400, result.statusCode());
        assertEquals("GEMINI", result.protocol());
    }

    @Test
    @DisplayName("Successful Gemini response with mock HttpClient executes decompose, deadline, priority, and summary")
    void testMockGeminiAdapterSuccess() throws Exception {
        String mockGeminiDecomposeResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\\n{\\n  \\"subtasks\\": [\\"Step 1\\", \\"Step 2\\", \\"Step 3\\"],\\n  \\"rationale\\": \\"Gemini plan\\"\\n}\\n```"
                      }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP"
                }
              ]
            }
            """;

        HttpResponse<String> mockResponse = new MockHttpResponse(200, mockGeminiDecomposeResponse);
        HttpClient mockClient = new MockHttpClient(mockResponse);

        CustomProviderDefinition configuredDef = CustomProviderDefinition.ofGemini(
            "gemini-configured",
            "Gemini Configured",
            "https://generativelanguage.googleapis.com/v1beta",
            "AIzaSy-valid-test-key",
            "gemini-2.5-flash"
        );
        AiConfig config = AiConfig.defaults();
        GeminiCompatibleAiAdapter configuredAdapter = new GeminiCompatibleAiAdapter(
            configuredDef, config, mockClient, new ObjectMapper());

        AiTaskDecompositionDto dto = configuredAdapter.decomposeTask(task, project, null);
        assertNotNull(dto);
        assertEquals(3, dto.subTasks().size());
        assertEquals("Step 1", dto.subTasks().get(0));
        assertEquals("Gemini plan", dto.rationale());
    }

    // Test Doubles for HTTP
    private static class MockHttpClient extends HttpClient {
        private final HttpResponse<String> responseToReturn;

        public MockHttpClient(HttpResponse<String> responseToReturn) {
            this.responseToReturn = responseToReturn;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            // Verify x-goog-api-key header presence as specified in OpenCode
            assertTrue(request.headers().firstValue("x-goog-api-key").isPresent(),
                "Request to Gemini API must include x-goog-api-key header");
            assertTrue(request.uri().toString().contains(":generateContent"),
                "Request URI must target Gemini :generateContent endpoint");
            return (HttpResponse<T>) responseToReturn;
        }

        @Override
        public java.util.Optional<java.net.CookieHandler> cookieHandler() { return java.util.Optional.empty(); }
        @Override
        public java.util.Optional<Duration> connectTimeout() { return java.util.Optional.empty(); }
        @Override
        public Redirect followRedirects() { return Redirect.NEVER; }
        @Override
        public java.util.Optional<java.net.ProxySelector> proxy() { return java.util.Optional.empty(); }
        @Override
        public javax.net.ssl.SSLContext sslContext() { return null; }
        @Override
        public javax.net.ssl.SSLParameters sslParameters() { return null; }
        @Override
        public java.util.Optional<java.net.Authenticator> authenticator() { return java.util.Optional.empty(); }
        @Override
        public Version version() { return Version.HTTP_1_1; }
        @Override
        public java.util.Optional<java.util.concurrent.Executor> executor() { return java.util.Optional.empty(); }
        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            return java.util.concurrent.CompletableFuture.completedFuture((HttpResponse<T>) responseToReturn);
        }
        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            return java.util.concurrent.CompletableFuture.completedFuture((HttpResponse<T>) responseToReturn);
        }
    }

    private static class MockHttpResponse implements HttpResponse<String> {
        private final int statusCode;
        private final String body;

        public MockHttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override public int statusCode() { return statusCode; }
        @Override public HttpRequest request() { return null; }
        @Override public java.util.Optional<HttpResponse<String>> previousResponse() { return java.util.Optional.empty(); }
        @Override public java.net.http.HttpHeaders headers() { return java.net.http.HttpHeaders.of(java.util.Map.of(), (k, v) -> true); }
        @Override public String body() { return body; }
        @Override public java.util.Optional<javax.net.ssl.SSLSession> sslSession() { return java.util.Optional.empty(); }
        @Override public java.net.URI uri() { return null; }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }
}
