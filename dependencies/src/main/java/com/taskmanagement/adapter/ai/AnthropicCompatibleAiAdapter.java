package com.taskmanagement.adapter.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.domain.ai.AiModelPort;
import com.taskmanagement.domain.ai.AiPriorityProposal;
import com.taskmanagement.domain.ai.AiPromptConfig;
import com.taskmanagement.domain.ai.AiSubTaskProposal;
import com.taskmanagement.domain.ai.AiTaskBreakdown;
import com.taskmanagement.domain.ai.AiTaskSummary;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Adapter implementing {@link AiModelPort} for Anthropic-compatible Messages endpoints
 * (supports Anthropic Claude models, Claude proxies, and Bedrock/OpenRouter Anthropic-formatted routes).
 */
public class AnthropicCompatibleAiAdapter implements AiModelPort {
    private static final Logger logger = LoggerFactory.getLogger(AnthropicCompatibleAiAdapter.class);

    public static final String DEFAULT_BASE_URL = "https://api.anthropic.com/v1";
    public static final String DEFAULT_MODEL = "claude-3-5-sonnet-20241022";
    public static final String DEFAULT_VERSION = "2023-06-01";
    public static final int DEFAULT_MAX_TOKENS = 4096;

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final String anthropicVersion;
    private final int maxTokens;
    private final int timeoutSeconds;
    private final Map<String, String> customHeaders;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AnthropicCompatibleAiAdapter(String apiKey,
                                        String baseUrl,
                                        String model,
                                        int timeoutSeconds,
                                        ObjectMapper objectMapper) {
        this(apiKey, baseUrl, model, DEFAULT_VERSION, DEFAULT_MAX_TOKENS, timeoutSeconds, Map.of(), objectMapper,
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build());
    }

    public AnthropicCompatibleAiAdapter(String apiKey,
                                        String baseUrl,
                                        String model,
                                        int timeoutSeconds,
                                        ObjectMapper objectMapper,
                                        HttpClient httpClient) {
        this(apiKey, baseUrl, model, DEFAULT_VERSION, DEFAULT_MAX_TOKENS, timeoutSeconds, Map.of(), objectMapper, httpClient);
    }

    public AnthropicCompatibleAiAdapter(String apiKey,
                                        String baseUrl,
                                        String model,
                                        String anthropicVersion,
                                        int maxTokens,
                                        int timeoutSeconds,
                                        Map<String, String> customHeaders,
                                        ObjectMapper objectMapper,
                                        HttpClient httpClient) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.model = (model != null && !model.isBlank()) ? model.trim() : DEFAULT_MODEL;
        this.anthropicVersion = (anthropicVersion != null && !anthropicVersion.isBlank()) ? anthropicVersion.trim() : DEFAULT_VERSION;
        this.maxTokens = maxTokens > 0 ? maxTokens : DEFAULT_MAX_TOKENS;
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 30;
        this.customHeaders = customHeaders != null ? Map.copyOf(customHeaders) : Map.of();
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
        this.httpClient = Objects.requireNonNull(httpClient, "HttpClient cannot be null");
    }

    public boolean isConfigured() {
        return !apiKey.isBlank() || baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1");
    }

    @Override
    public AiTaskBreakdown generateTaskBreakdown(Task task, Project project, AiPromptConfig promptConfig) {
        String userPrompt = buildBreakdownUserPrompt(task, project, promptConfig);
        String rawJson = callAnthropicMessages(promptConfig.systemPrompt(), userPrompt, promptConfig.temperature());
        return parseBreakdown(rawJson);
    }

    @Override
    public AiPriorityProposal suggestPriority(Task task, Project project, AiPromptConfig promptConfig) {
        String userPrompt = buildPriorityUserPrompt(task, project, promptConfig);
        String rawJson = callAnthropicMessages(promptConfig.systemPrompt(), userPrompt, promptConfig.temperature());
        return parsePriority(rawJson);
    }

    @Override
    public AiTaskSummary generateTaskSummary(Task task, Project project, AiPromptConfig promptConfig) {
        String userPrompt = buildSummaryUserPrompt(task, project, promptConfig);
        String rawJson = callAnthropicMessages(promptConfig.systemPrompt(), userPrompt, promptConfig.temperature());
        return parseSummary(rawJson);
    }

    protected String callAnthropicMessages(String systemPrompt, String userPrompt, double temperature) {
        String endpoint = resolveEndpoint();
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("max_tokens", maxTokens);
            payload.put("temperature", Math.min(1.0, Math.max(0.0, temperature)));

            if (systemPrompt != null && !systemPrompt.isBlank()) {
                payload.put("system", systemPrompt);
            }

            List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", userPrompt)
            );
            payload.put("messages", messages);

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .header("anthropic-version", anthropicVersion);

            if (!apiKey.isBlank()) {
                builder.header("x-api-key", apiKey);
            }

            if (customHeaders != null && !customHeaders.isEmpty()) {
                customHeaders.forEach(builder::header);
            }

            HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8)).build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiCommunicationException(
                    "Anthropic API error: HTTP " + response.statusCode() + " - " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentArray = root.path("content");
            if (!contentArray.isArray() || contentArray.isEmpty()) {
                throw new AiCommunicationException("Anthropic API returned empty content array: " + response.body());
            }

            StringBuilder sb = new StringBuilder();
            for (JsonNode block : contentArray) {
                if ("text".equals(block.path("type").asText(""))) {
                    sb.append(block.path("text").asText(""));
                }
            }

            if (sb.isEmpty()) {
                throw new AiCommunicationException("Anthropic API returned no text blocks in content: " + response.body());
            }

            return JsonExtractor.extractJson(sb.toString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new AiCommunicationException("Failed to communicate with Anthropic-compatible API: " + e.getMessage(), e);
        }
    }

    private String resolveEndpoint() {
        if (baseUrl.endsWith("/messages")) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/")) {
            return baseUrl + "messages";
        }
        return baseUrl + "/messages";
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        return url.trim();
    }

    private String buildBreakdownUserPrompt(Task task, Project project, AiPromptConfig config) {
        String projectName = project != null ? project.getName().value() : "Inbox";
        String existingSubtasks = task.getSubTasks().isEmpty() ? "None" :
            String.join(", ", task.getSubTasks().stream().map(s -> s.getTitle()).toList());

        return """
            Analyze the following task and decompose it into actionable subtasks with an estimated deadline.
            Output strictly valid JSON with this schema:
            {
              "subtasks": [
                {"title": "string", "explanation": "string"}
              ],
              "estimatedDeadline": "ISO-8601 UTC timestamp or null",
              "reasoning": "string"
            }

            Task Information:
            - Title: %s
            - Description: %s
            - Project: %s
            - Current Priority: %s
            - Current Deadline: %s
            - Existing Subtasks: %s
            %s
            """.formatted(
            task.getTitle().value(),
            task.getDescription() != null ? task.getDescription() : "None",
            projectName,
            task.getPriority(),
            task.getDueDate() != null ? task.getDueDate().dueDate() : "None",
            existingSubtasks,
            config.additionalInstructions().isBlank() ? "" : "- Additional Instructions: " + config.additionalInstructions()
        );
    }

    private String buildPriorityUserPrompt(Task task, Project project, AiPromptConfig config) {
        String projectName = project != null ? project.getName().value() : "Inbox";
        return """
            Evaluate the following task and suggest its optimal priority (LOW, MEDIUM, or HIGH).
            Output strictly valid JSON with this schema:
            {
              "priority": "LOW" | "MEDIUM" | "HIGH",
              "reasoning": "string",
              "confidence": 0.0 to 1.0
            }

            Task Information:
            - Title: %s
            - Description: %s
            - Project: %s
            - Status: %s
            - Current Priority: %s
            - Deadline: %s
            - Is Overdue: %s
            %s
            """.formatted(
            task.getTitle().value(),
            task.getDescription() != null ? task.getDescription() : "None",
            projectName,
            task.getStatus(),
            task.getPriority(),
            task.getDueDate() != null ? task.getDueDate().dueDate() : "None",
            task.isOverdue(),
            config.additionalInstructions().isBlank() ? "" : "- Additional Instructions: " + config.additionalInstructions()
        );
    }

    private String buildSummaryUserPrompt(Task task, Project project, AiPromptConfig config) {
        String projectName = project != null ? project.getName().value() : "Inbox";
        String subtasks = task.getSubTasks().isEmpty() ? "None" :
            String.join(", ", task.getSubTasks().stream().map(s -> s.getTitle() + (s.isCompleted() ? " (Done)" : "")).toList());

        return """
            Summarize the following task, listing key highlights and the recommended next step.
            Output strictly valid JSON with this schema:
            {
              "summary": "string",
              "keyHighlights": ["string"],
              "suggestedNextStep": "string"
            }

            Task Information:
            - Title: %s
            - Description: %s
            - Project: %s
            - Status: %s
            - Priority: %s
            - Deadline: %s
            - Subtasks: %s
            %s
            """.formatted(
            task.getTitle().value(),
            task.getDescription() != null ? task.getDescription() : "None",
            projectName,
            task.getStatus(),
            task.getPriority(),
            task.getDueDate() != null ? task.getDueDate().dueDate() : "None",
            subtasks,
            config.additionalInstructions().isBlank() ? "" : "- Additional Instructions: " + config.additionalInstructions()
        );
    }

    private AiTaskBreakdown parseBreakdown(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            List<AiSubTaskProposal> subtasks = new ArrayList<>();
            JsonNode subtasksNode = root.path("subtasks");
            if (subtasksNode.isArray()) {
                for (JsonNode item : subtasksNode) {
                    String title = item.path("title").asText("").trim();
                    String explanation = item.path("explanation").asText("").trim();
                    if (!title.isBlank()) {
                        subtasks.add(new AiSubTaskProposal(title, explanation));
                    }
                }
            }

            Instant estimatedDeadline = null;
            String deadlineStr = root.path("estimatedDeadline").asText("");
            if (!deadlineStr.isBlank() && !"null".equalsIgnoreCase(deadlineStr)) {
                try {
                    estimatedDeadline = Instant.parse(deadlineStr);
                } catch (Exception e) {
                    logger.debug("Could not parse estimated deadline '{}': {}", deadlineStr, e.getMessage());
                }
            }

            String reasoning = root.path("reasoning").asText("Anthropic breakdown completed.");
            return new AiTaskBreakdown(subtasks, estimatedDeadline, reasoning);
        } catch (Exception e) {
            throw new AiCommunicationException("Failed to parse task breakdown JSON from Anthropic: " + e.getMessage(), e);
        }
    }

    private AiPriorityProposal parsePriority(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            String priorityStr = root.path("priority").asText("MEDIUM").trim().toUpperCase(Locale.ROOT);
            Priority priority = switch (priorityStr) {
                case "HIGH" -> Priority.HIGH;
                case "LOW" -> Priority.LOW;
                default -> Priority.MEDIUM;
            };

            String reasoning = root.path("reasoning").asText("Anthropic priority evaluation.");
            double confidence = root.path("confidence").asDouble(0.8);

            return new AiPriorityProposal(priority, reasoning, confidence);
        } catch (Exception e) {
            throw new AiCommunicationException("Failed to parse priority JSON from Anthropic: " + e.getMessage(), e);
        }
    }

    private AiTaskSummary parseSummary(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            String summary = root.path("summary").asText("");
            List<String> highlights = new ArrayList<>();
            JsonNode highlightsNode = root.path("keyHighlights");
            if (highlightsNode.isArray()) {
                for (JsonNode h : highlightsNode) {
                    String text = h.asText("").trim();
                    if (!text.isBlank()) {
                        highlights.add(text);
                    }
                }
            }
            String nextStep = root.path("suggestedNextStep").asText("");

            return new AiTaskSummary(summary, highlights, nextStep);
        } catch (Exception e) {
            throw new AiCommunicationException("Failed to parse task summary JSON from Anthropic: " + e.getMessage(), e);
        }
    }
}
