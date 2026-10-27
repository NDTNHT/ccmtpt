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
 * Adapter implementing {@link AiModelPort} for OpenAI-compatible chat endpoints
 * (supports OpenAI, OpenRouter, DeepSeek, Groq, and Ollama).
 */
public class OpenAiCompatibleAiAdapter implements AiModelPort {
    private static final Logger logger = LoggerFactory.getLogger(OpenAiCompatibleAiAdapter.class);

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int timeoutSeconds;
    private final Map<String, String> customHeaders;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleAiAdapter(String apiKey,
                                     String baseUrl,
                                     String model,
                                     int timeoutSeconds,
                                     ObjectMapper objectMapper) {
        this(apiKey, baseUrl, model, timeoutSeconds, Map.of(), objectMapper,
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build());
    }

    public OpenAiCompatibleAiAdapter(String apiKey,
                                     String baseUrl,
                                     String model,
                                     int timeoutSeconds,
                                     ObjectMapper objectMapper,
                                     HttpClient httpClient) {
        this(apiKey, baseUrl, model, timeoutSeconds, Map.of(), objectMapper, httpClient);
    }

    public OpenAiCompatibleAiAdapter(String apiKey,
                                     String baseUrl,
                                     String model,
                                     int timeoutSeconds,
                                     Map<String, String> customHeaders,
                                     ObjectMapper objectMapper,
                                     HttpClient httpClient) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.model = (model != null && !model.isBlank()) ? model.trim() : "gpt-4o-mini";
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
        String rawJson = callChatCompletion(promptConfig.systemPrompt(), userPrompt, promptConfig.temperature());
        return parseBreakdown(rawJson);
    }

    @Override
    public AiPriorityProposal suggestPriority(Task task, Project project, AiPromptConfig promptConfig) {
        String userPrompt = buildPriorityUserPrompt(task, project, promptConfig);
        String rawJson = callChatCompletion(promptConfig.systemPrompt(), userPrompt, promptConfig.temperature());
        return parsePriority(rawJson);
    }

    @Override
    public AiTaskSummary generateTaskSummary(Task task, Project project, AiPromptConfig promptConfig) {
        String userPrompt = buildSummaryUserPrompt(task, project, promptConfig);
        String rawJson = callChatCompletion(promptConfig.systemPrompt(), userPrompt, promptConfig.temperature());
        return parseSummary(rawJson);
    }

    protected String callChatCompletion(String systemPrompt, String userPrompt, double temperature) {
        String endpoint = resolveEndpoint();
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("temperature", temperature);

            List<Map<String, String>> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }
            messages.add(Map.of("role", "user", "content", userPrompt));
            payload.put("messages", messages);
            payload.put("response_format", Map.of("type", "json_object"));

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json");

            if (!apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }

            if (customHeaders != null && !customHeaders.isEmpty()) {
                customHeaders.forEach(builder::header);
            }

            HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8)).build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiCommunicationException(
                    "OpenAI API error: HTTP " + response.statusCode() + " - " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new AiCommunicationException("OpenAI API returned empty choices array: " + response.body());
            }

            String content = choices.get(0).path("message").path("content").asText("");
            return JsonExtractor.extractJson(content);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new AiCommunicationException("Failed to communicate with OpenAI-compatible API: " + e.getMessage(), e);
        }
    }

    private String resolveEndpoint() {
        if (baseUrl.endsWith("/chat/completions")) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/")) {
            return baseUrl + "chat/completions";
        }
        return baseUrl + "/chat/completions";
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "https://api.openai.com/v1";
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

    private AiTaskBreakdown parseBreakdown(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<AiSubTaskProposal> subtasks = new ArrayList<>();
            JsonNode subtasksNode = root.path("subtasks");
            if (subtasksNode.isArray()) {
                for (JsonNode item : subtasksNode) {
                    String title = item.path("title").asText("").trim();
                    if (!title.isEmpty()) {
                        String explanation = item.path("explanation").asText("").trim();
                        subtasks.add(new AiSubTaskProposal(title, explanation));
                    }
                }
            }

            Instant estimatedDeadline = null;
            String deadlineText = root.path("estimatedDeadline").asText(null);
            if (deadlineText != null && !deadlineText.isBlank() && !"null".equalsIgnoreCase(deadlineText)) {
                try {
                    estimatedDeadline = Instant.parse(deadlineText.trim());
                } catch (Exception e) {
                    logger.debug("Could not parse estimatedDeadline '{}' as Instant: {}", deadlineText, e.getMessage());
                }
            }

            String reasoning = root.path("reasoning").asText("Decomposed by AI assistant.");
            return new AiTaskBreakdown(subtasks, estimatedDeadline, reasoning);
        } catch (Exception e) {
            throw new AiCommunicationException("Failed to parse task breakdown JSON: " + json, e);
        }
    }

    private AiPriorityProposal parsePriority(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String priorityStr = root.path("priority").asText("MEDIUM").toUpperCase(Locale.ROOT).trim();
            Priority priority;
            try {
                priority = Priority.valueOf(priorityStr);
            } catch (Exception e) {
                priority = Priority.MEDIUM;
            }
            String reasoning = root.path("reasoning").asText("Priority determined by AI.");
            double confidence = root.path("confidence").asDouble(0.9);

            return new AiPriorityProposal(priority, reasoning, confidence);
        } catch (Exception e) {
            throw new AiCommunicationException("Failed to parse priority proposal JSON: " + json, e);
        }
    }

    private AiTaskSummary parseSummary(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String summary = root.path("summary").asText("Task summary generated by AI.");
            List<String> highlights = new ArrayList<>();
            JsonNode hlNode = root.path("keyHighlights");
            if (hlNode.isArray()) {
                for (JsonNode item : hlNode) {
                    String text = item.asText("").trim();
                    if (!text.isEmpty()) {
                        highlights.add(text);
                    }
                }
            }
            String nextStep = root.path("suggestedNextStep").asText("Proceed with initial task step.");
            return new AiTaskSummary(summary, highlights, nextStep);
        } catch (Exception e) {
            throw new AiCommunicationException("Failed to parse task summary JSON: " + json, e);
        }
    }
}
