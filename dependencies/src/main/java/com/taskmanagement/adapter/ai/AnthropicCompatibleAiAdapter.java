package com.taskmanagement.adapter.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.application.dto.ai.AiAnalysisResultDto;
import com.taskmanagement.application.dto.ai.AiDeadlineEstimationDto;
import com.taskmanagement.application.dto.ai.AiPriorityRecommendationDto;
import com.taskmanagement.application.dto.ai.AiTaskDecompositionDto;
import com.taskmanagement.application.dto.ai.AiTaskSummaryDto;
import com.taskmanagement.application.dto.ai.ProviderVerificationResultDto;
import com.taskmanagement.application.port.AiAssistantPort;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Connects to Anthropic Messages API standard (Claude 3.5 Sonnet, Haiku, Opus, or compatible gateways)
 * via HTTP JSON requests, parsing structured responses.
 */
public class AnthropicCompatibleAiAdapter implements AiAssistantPort {
    private static final Logger LOGGER = Logger.getLogger(AnthropicCompatibleAiAdapter.class.getName());
    public static final String ANTHROPIC_VERSION = "2023-06-01";

    private final CustomProviderDefinition providerDefinition;
    private final AiConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AnthropicCompatibleAiAdapter(CustomProviderDefinition providerDefinition, AiConfig config) {
        this(providerDefinition, config, HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(providerDefinition != null ? providerDefinition.timeoutSeconds() : 30))
            .build(), new ObjectMapper());
    }

    public AnthropicCompatibleAiAdapter(CustomProviderDefinition providerDefinition,
                                        AiConfig config,
                                        HttpClient httpClient,
                                        ObjectMapper objectMapper) {
        this.providerDefinition = providerDefinition != null
            ? providerDefinition
            : CustomProviderDefinition.ofAnthropic("anthropic", "Anthropic Claude", "https://api.anthropic.com/v1", "", "claude-3-5-sonnet-20241022");
        this.config = config != null ? config : AiConfig.defaults();
        this.httpClient = httpClient != null ? httpClient : HttpClient.newHttpClient();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public AiConfig getConfig() {
        return config;
    }

    public CustomProviderDefinition getProviderDefinition() {
        return providerDefinition;
    }

    @Override
    public ProviderVerificationResultDto verifyAndDiscoverModels(String baseUrl, String apiKey, AiProtocol protocol, int timeoutSeconds) {
        String effectiveBaseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : providerDefinition.baseUrl();
        String effectiveApiKey = (apiKey != null) ? apiKey : providerDefinition.apiKey();
        int effectiveTimeout = timeoutSeconds > 0 ? timeoutSeconds : providerDefinition.timeoutSeconds();
        return verifyAnthropicModels(this.httpClient, this.objectMapper, effectiveBaseUrl, effectiveApiKey, effectiveTimeout);
    }

    public static ProviderVerificationResultDto verifyAnthropicModels(
        HttpClient client,
        ObjectMapper mapper,
        String baseUrl,
        String apiKey,
        int timeoutSeconds
    ) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return ProviderVerificationResultDto.failure(400, "Base URL cannot be blank", "ANTHROPIC");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return ProviderVerificationResultDto.failure(401, "API Key is required to verify Anthropic provider models", "ANTHROPIC");
        }
        String cleanUrl = baseUrl.replaceAll("/+$", "");
        String modelsEndpoint = cleanUrl.endsWith("/models") ? cleanUrl : cleanUrl + "/models";

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(modelsEndpoint))
                .header("Content-Type", "application/json")
                .header("anthropic-version", ANTHROPIC_VERSION)
                .timeout(Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 15))
                .GET();

            requestBuilder.header("x-api-key", apiKey.trim());

            HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status >= 200 && status < 300) {
                List<String> models = new ArrayList<>();
                try {
                    JsonNode root = mapper.readTree(response.body());
                    JsonNode dataNode = root.has("data") ? root.get("data") : root;
                    if (dataNode.isArray()) {
                        for (JsonNode item : dataNode) {
                            if (item.has("id")) {
                                models.add(item.get("id").asText());
                            } else if (item.isTextual()) {
                                models.add(item.asText());
                            }
                        }
                    }
                } catch (Exception parseEx) {
                    LOGGER.log(Level.WARNING, "Failed to parse Anthropic models JSON: " + parseEx.getMessage());
                }
                if (models.isEmpty()) {
                    models = List.of(
                        "claude-3-5-sonnet-20241022",
                        "claude-3-5-haiku-20241022",
                        "claude-3-opus-20240229"
                    );
                }
                return ProviderVerificationResultDto.success(status, models, "ANTHROPIC");
            }

            if (status == 401 || status == 403) {
                String detail = extractErrorMessage(response.body(), mapper);
                return ProviderVerificationResultDto.failure(status, "Invalid API key or access denied (HTTP " + status + "): " + detail, "ANTHROPIC");
            }

            return ProviderVerificationResultDto.failure(status, "Server responded with HTTP " + status + ": " + extractErrorMessage(response.body(), mapper), "ANTHROPIC");
        } catch (Exception ex) {
            return ProviderVerificationResultDto.failure(0, "Connection error to " + modelsEndpoint + ": " + ex.getMessage(), "ANTHROPIC");
        }
    }

    private static String extractErrorMessage(String body, ObjectMapper mapper) {
        if (body == null || body.isBlank()) {
            return "No error details from server";
        }
        try {
            JsonNode root = mapper.readTree(body);
            if (root.has("error")) {
                JsonNode err = root.get("error");
                if (err.isObject() && err.has("message")) {
                    return err.get("message").asText();
                }
                return err.asText();
            }
            if (root.has("message")) {
                return root.get("message").asText();
            }
        } catch (Exception ignored) {
        }
        return body.length() > 200 ? body.substring(0, 197) + "..." : body.trim();
    }

    @Override
    public AiTaskDecompositionDto decomposeTask(Task task, Project project, String customPrompt) {
        Objects.requireNonNull(task, "Task cannot be null");
        if (!providerDefinition.hasValidApiKey()) {
            throw new IllegalStateException("No active AI provider connected. Please configure and verify an API key in AI Settings.");
        }

        String userPrompt = buildTaskContextPrompt(task, project) +
            "\nPlease break down this task into 3 to 6 actionable checklist subtasks. " +
            "Respond strictly in JSON format matching this schema:\n" +
            "{\n  \"subtasks\": [\"string\"],\n  \"rationale\": \"string\"\n}";

        try {
            JsonNode root = callAnthropic(userPrompt, customPrompt);
            List<String> subTasks = new ArrayList<>();
            if (root.has("subtasks") && root.get("subtasks").isArray()) {
                for (JsonNode item : root.get("subtasks")) {
                    subTasks.add(item.asText());
                }
            }
            String rationale = root.has("rationale") ? root.get("rationale").asText() : "Anthropic decomposed checklist.";
            return new AiTaskDecompositionDto(task.getId().value(), subTasks, rationale);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to decompose task via Anthropic API: " + exception.getMessage());
            throw new IllegalStateException("Anthropic task decomposition failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public AiTaskDecompositionDto suggestSubTasks(Task task, Project project, String customPrompt) {
        Objects.requireNonNull(task, "Task cannot be null");
        if (!providerDefinition.hasValidApiKey()) {
            throw new IllegalStateException("No active AI provider connected. Please configure and verify an API key in AI Settings.");
        }

        String userPrompt = buildTaskContextPrompt(task, project) +
            "\nGiven the current subtasks above, suggest 2 to 4 additional missing or recommended subtasks. " +
            "Respond strictly in JSON format matching this schema:\n" +
            "{\n  \"subtasks\": [\"string\"],\n  \"rationale\": \"string\"\n}";

        try {
            JsonNode root = callAnthropic(userPrompt, customPrompt);
            List<String> subTasks = new ArrayList<>();
            if (root.has("subtasks") && root.get("subtasks").isArray()) {
                for (JsonNode item : root.get("subtasks")) {
                    subTasks.add(item.asText());
                }
            }
            String rationale = root.has("rationale") ? root.get("rationale").asText() : "Anthropic suggested subtasks.";
            return new AiTaskDecompositionDto(task.getId().value(), subTasks, rationale);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to suggest subtasks via Anthropic API: " + exception.getMessage());
            throw new IllegalStateException("Anthropic subtask suggestion failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public AiDeadlineEstimationDto estimateDeadline(Task task, Project project, String customPrompt) {
        Objects.requireNonNull(task, "Task cannot be null");
        if (!providerDefinition.hasValidApiKey()) {
            throw new IllegalStateException("No active AI provider connected. Please configure and verify an API key in AI Settings.");
        }

        String userPrompt = buildTaskContextPrompt(task, project) +
            "\nEstimate the required working hours and recommended completion deadline (in ISO-8601 instant string). " +
            "Respond strictly in JSON format matching this schema:\n" +
            "{\n  \"estimatedHours\": 16,\n  \"recommendedDeadline\": \"" + Instant.now().plus(Duration.ofDays(2)) + "\",\n  \"confidence\": \"HIGH|MEDIUM|LOW\",\n  \"explanation\": \"string\"\n}";

        try {
            JsonNode root = callAnthropic(userPrompt, customPrompt);
            int estimatedHours = root.has("estimatedHours") ? root.get("estimatedHours").asInt(24) : 24;
            String deadlineStr = root.has("recommendedDeadline") ? root.get("recommendedDeadline").asText() : null;
            Instant deadline = deadlineStr != null ? Instant.parse(deadlineStr) : Instant.now().plus(Duration.ofHours(estimatedHours));
            String confidence = root.has("confidence") ? root.get("confidence").asText("MEDIUM") : "MEDIUM";
            String explanation = root.has("explanation") ? root.get("explanation").asText() : "Estimated by Anthropic API";

            return new AiDeadlineEstimationDto(task.getId().value(), estimatedHours, deadline, confidence, explanation);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to estimate deadline via Anthropic API: " + exception.getMessage());
            throw new IllegalStateException("Anthropic deadline estimation failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public AiPriorityRecommendationDto analyzePriority(Task task, Project project, Priority domainPriority, String customPrompt) {
        Objects.requireNonNull(task, "Task cannot be null");
        if (!providerDefinition.hasValidApiKey()) {
            throw new IllegalStateException("No active AI provider connected. Please configure and verify an API key in AI Settings.");
        }

        String userPrompt = buildTaskContextPrompt(task, project) +
            "\nDomain rule calculated priority is: " + domainPriority + ".\n" +
            "Evaluate urgency and scope. Propose a priority (LOW, MEDIUM, HIGH, URGENT) and provide reasoning.\n" +
            "Respond strictly in JSON format matching this schema:\n" +
            "{\n  \"proposedPriority\": \"LOW|MEDIUM|HIGH\",\n  \"reasoning\": \"string\"\n}";

        try {
            JsonNode root = callAnthropic(userPrompt, customPrompt);
            String proposedStr = root.has("proposedPriority") ? root.get("proposedPriority").asText("MEDIUM") : "MEDIUM";
            Priority aiProposed;
            try {
                aiProposed = Priority.valueOf(proposedStr.toUpperCase());
            } catch (Exception ex) {
                aiProposed = Priority.MEDIUM;
            }
            String reasoning = root.has("reasoning") ? root.get("reasoning").asText() : "Evaluated by Anthropic";

            // Enforce domain invariant: domain rule escalation takes precedence
            Priority effective = aiProposed;
            if (domainPriority == Priority.HIGH) {
                effective = Priority.HIGH;
                reasoning += " (Domain deadline rule takes precedence: HIGH).";
            }

            return new AiPriorityRecommendationDto(task.getId().value(), aiProposed, domainPriority, effective, reasoning);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to analyze priority via Anthropic API: " + exception.getMessage());
            throw new IllegalStateException("Anthropic priority evaluation failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public AiTaskSummaryDto generateSummary(Task task, Project project, String customPrompt) {
        Objects.requireNonNull(task, "Task cannot be null");
        if (!providerDefinition.hasValidApiKey()) {
            throw new IllegalStateException("No active AI provider connected. Please configure and verify an API key in AI Settings.");
        }

        String userPrompt = buildTaskContextPrompt(task, project) +
            "\nGenerate an executive progress summary and key highlights for this task.\n" +
            "Respond strictly in JSON format matching this schema:\n" +
            "{\n  \"summary\": \"string\",\n  \"highlights\": [\"string\"]\n}";

        try {
            JsonNode root = callAnthropic(userPrompt, customPrompt);
            String summary = root.has("summary") ? root.get("summary").asText() : "";
            List<String> highlights = new ArrayList<>();
            if (root.has("highlights") && root.get("highlights").isArray()) {
                for (JsonNode item : root.get("highlights")) {
                    highlights.add(item.asText());
                }
            }
            long completed = task.getSubTasks().stream().filter(s -> s.isCompleted()).count();
            double rate = task.getSubTasks().isEmpty() ? 0.0 : ((double) completed / task.getSubTasks().size()) * 100.0;

            return new AiTaskSummaryDto(task.getId().value(), task.getTitle().value(), summary, rate, highlights);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to generate summary via Anthropic API: " + exception.getMessage());
            throw new IllegalStateException("Anthropic summary generation failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public AiAnalysisResultDto analyzeTaskComprehensive(Task task, Project project, Priority domainPriority, String customPrompt) {
        AiTaskDecompositionDto decomposition = decomposeTask(task, project, customPrompt);
        AiDeadlineEstimationDto deadlineEstimation = estimateDeadline(task, project, customPrompt);
        AiPriorityRecommendationDto priorityRecommendation = analyzePriority(task, project, domainPriority, customPrompt);
        AiTaskSummaryDto summary = generateSummary(task, project, customPrompt);

        return new AiAnalysisResultDto(
            task.getId().value(),
            decomposition,
            deadlineEstimation,
            priorityRecommendation,
            summary
        );
    }

    // Anthropic HTTP & JSON Helpers

    private JsonNode callAnthropic(String userPrompt, String customPrompt) throws Exception {
        String effectiveSystemPrompt = config.resolveEffectiveSystemPrompt(customPrompt);
        String endpoint = providerDefinition.baseUrl() + "/messages";

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", providerDefinition.defaultModel());
        payload.put("max_tokens", 4096);
        payload.put("system", effectiveSystemPrompt);
        payload.put("temperature", config.getTemperature());

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", userPrompt));
        payload.put("messages", messages);

        String requestBody = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .header("x-api-key", providerDefinition.apiKey())
            .header("anthropic-version", ANTHROPIC_VERSION)
            .timeout(Duration.ofSeconds(providerDefinition.timeoutSeconds()))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Anthropic provider returned HTTP " + response.statusCode() + ": " + extractErrorMessage(response.body(), objectMapper));
        }

        JsonNode responseJson = objectMapper.readTree(response.body());
        JsonNode contentBlocks = responseJson.path("content");
        String contentText = "";
        if (contentBlocks.isArray() && contentBlocks.size() > 0) {
            contentText = contentBlocks.get(0).path("text").asText();
        }

        return parseCleanJson(contentText);
    }

    private JsonNode parseCleanJson(String rawContent) throws Exception {
        String cleaned = rawContent.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();
        return objectMapper.readTree(cleaned);
    }

    private String buildTaskContextPrompt(Task task, Project project) {
        StringBuilder sb = new StringBuilder();
        sb.append("Task Information:\n");
        sb.append("- Title: ").append(task.getTitle().value()).append("\n");
        sb.append("- Description: ").append(task.getDescription() != null ? task.getDescription() : "None").append("\n");
        sb.append("- Current Status: ").append(task.getStatus()).append("\n");
        sb.append("- Current Priority: ").append(task.getPriority()).append("\n");
        if (task.getDueDate() != null) {
            sb.append("- Due Date: ").append(task.getDueDate().dueDate()).append("\n");
        }
        if (project != null) {
            sb.append("- Project Name: ").append(project.getName().value()).append("\n");
            sb.append("- Project Type: ").append(project.getProjectType()).append("\n");
        }
        if (!task.getSubTasks().isEmpty()) {
            sb.append("- Current Subtasks:\n");
            for (var st : task.getSubTasks()) {
                sb.append("  * ").append(st.getTitle()).append(st.isCompleted() ? " [DONE]" : " [PENDING]").append("\n");
            }
        }
        return sb.toString();
    }
}
