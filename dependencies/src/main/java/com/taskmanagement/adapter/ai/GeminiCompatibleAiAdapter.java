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
 * Connects to Google Gemini API standard (Gemini 2.5 Flash, Gemini 1.5 Pro, etc.)
 * using the Google Generative Language wire protocol (:generateContent and /models).
 * Reference architecture ported from OpenCode protocols/gemini.ts and providers/google.ts.
 */
public class GeminiCompatibleAiAdapter implements AiAssistantPort {
    private static final Logger LOGGER = Logger.getLogger(GeminiCompatibleAiAdapter.class.getName());
    public static final String DEFAULT_GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private final CustomProviderDefinition providerDefinition;
    private final AiConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiCompatibleAiAdapter(CustomProviderDefinition providerDefinition, AiConfig config) {
        this(providerDefinition, config, HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(providerDefinition != null ? providerDefinition.timeoutSeconds() : 30))
            .build(), new ObjectMapper());
    }

    public GeminiCompatibleAiAdapter(CustomProviderDefinition providerDefinition,
                                     AiConfig config,
                                     HttpClient httpClient,
                                     ObjectMapper objectMapper) {
        this.providerDefinition = providerDefinition != null
            ? providerDefinition
            : CustomProviderDefinition.ofGemini("gemini", "Google Gemini", DEFAULT_GEMINI_BASE_URL, "", "gemini-2.5-flash");
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
        return verifyGeminiModels(this.httpClient, this.objectMapper, effectiveBaseUrl, effectiveApiKey, effectiveTimeout);
    }

    public static ProviderVerificationResultDto verifyGeminiModels(
        HttpClient client,
        ObjectMapper mapper,
        String baseUrl,
        String apiKey,
        int timeoutSeconds
    ) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return ProviderVerificationResultDto.failure(400, "Base URL cannot be blank", "GEMINI");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return ProviderVerificationResultDto.failure(401, "API Key is required to verify Gemini provider models", "GEMINI");
        }
        String cleanUrl = baseUrl.replaceAll("/+$", "");
        String modelsEndpoint = cleanUrl.endsWith("/models") ? cleanUrl : cleanUrl + "/models";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(modelsEndpoint))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey.trim())
                .timeout(Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 15))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status >= 200 && status < 300) {
                List<String> models = new ArrayList<>();
                try {
                    JsonNode root = mapper.readTree(response.body());
                    JsonNode modelsNode = root.has("models") ? root.get("models") : (root.has("data") ? root.get("data") : root);
                    if (modelsNode.isArray()) {
                        for (JsonNode item : modelsNode) {
                            boolean supportsGenerate = true;
                            if (item.has("supportedGenerationMethods") && item.get("supportedGenerationMethods").isArray()) {
                                supportsGenerate = false;
                                for (JsonNode method : item.get("supportedGenerationMethods")) {
                                    if ("generateContent".equalsIgnoreCase(method.asText())) {
                                        supportsGenerate = true;
                                        break;
                                    }
                                }
                            }
                            if (supportsGenerate) {
                                if (item.has("name")) {
                                    String name = item.get("name").asText().replaceFirst("^models/", "");
                                    models.add(name);
                                } else if (item.has("id")) {
                                    models.add(item.get("id").asText());
                                }
                            }
                        }
                    }
                } catch (Exception parseEx) {
                    LOGGER.log(Level.WARNING, "Failed to parse Gemini models JSON: " + parseEx.getMessage());
                }

                if (models.isEmpty()) {
                    models = List.of(
                        "gemini-2.5-flash",
                        "gemini-2.5-pro",
                        "gemini-2.0-flash",
                        "gemini-1.5-flash",
                        "gemini-1.5-pro"
                    );
                }
                return ProviderVerificationResultDto.success(status, models, "GEMINI");
            }

            if (status == 400 || status == 401 || status == 403) {
                String detail = extractErrorMessage(response.body(), mapper);
                return ProviderVerificationResultDto.failure(status, "Invalid API key or access denied (HTTP " + status + "): " + detail, "GEMINI");
            }

            return ProviderVerificationResultDto.failure(status, "Server responded with HTTP " + status + ": " + extractErrorMessage(response.body(), mapper), "GEMINI");
        } catch (Exception ex) {
            return ProviderVerificationResultDto.failure(0, "Connection error to " + modelsEndpoint + ": " + ex.getMessage(), "GEMINI");
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
            JsonNode root = callGemini(userPrompt, customPrompt);
            List<String> subTasks = new ArrayList<>();
            JsonNode subtasksNode = root.has("subtasks") ? root.get("subtasks") : (root.has("subTasks") ? root.get("subTasks") : null);
            if (subtasksNode != null && subtasksNode.isArray()) {
                for (JsonNode item : subtasksNode) {
                    subTasks.add(item.asText());
                }
            }
            String rationale = root.has("rationale") ? root.get("rationale").asText() : "Gemini decomposed task checkpoints.";
            return new AiTaskDecompositionDto(task.getId().value(), subTasks, rationale);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to decompose task via Gemini API: " + exception.getMessage());
            throw new IllegalStateException("Gemini task decomposition failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public AiTaskDecompositionDto suggestSubTasks(Task task, Project project, String customPrompt) {
        Objects.requireNonNull(task, "Task cannot be null");
        if (!providerDefinition.hasValidApiKey()) {
            throw new IllegalStateException("No active AI provider connected. Please configure and verify an API key in AI Settings.");
        }

        String userPrompt = buildTaskContextPrompt(task, project) +
            "\nPlease analyze current subtasks and propose 2 to 4 complementary subtasks. " +
            "Respond strictly in JSON format matching this schema:\n" +
            "{\n  \"subtasks\": [\"string\"],\n  \"rationale\": \"string\"\n}";

        try {
            JsonNode root = callGemini(userPrompt, customPrompt);
            List<String> subTasks = new ArrayList<>();
            JsonNode subtasksNode = root.has("subtasks") ? root.get("subtasks") : (root.has("subTasks") ? root.get("subTasks") : null);
            if (subtasksNode != null && subtasksNode.isArray()) {
                for (JsonNode item : subtasksNode) {
                    subTasks.add(item.asText());
                }
            }
            String rationale = root.has("rationale") ? root.get("rationale").asText() : "Gemini suggested subtasks.";
            return new AiTaskDecompositionDto(task.getId().value(), subTasks, rationale);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to suggest subtasks via Gemini API: " + exception.getMessage());
            throw new IllegalStateException("Gemini subtask suggestion failed: " + exception.getMessage(), exception);
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
            JsonNode root = callGemini(userPrompt, customPrompt);
            int estimatedHours = root.has("estimatedHours") ? root.get("estimatedHours").asInt(24) : 24;
            String deadlineStr = root.has("recommendedDeadline") ? root.get("recommendedDeadline").asText() : null;
            Instant deadline = deadlineStr != null ? Instant.parse(deadlineStr) : Instant.now().plus(Duration.ofHours(estimatedHours));
            String confidence = root.has("confidence") ? root.get("confidence").asText("MEDIUM") : "MEDIUM";
            String explanation = root.has("explanation") ? root.get("explanation").asText() : "Estimated by Gemini API";

            return new AiDeadlineEstimationDto(task.getId().value(), estimatedHours, deadline, confidence, explanation);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to estimate deadline via Gemini API: " + exception.getMessage());
            throw new IllegalStateException("Gemini deadline estimation failed: " + exception.getMessage(), exception);
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
            JsonNode root = callGemini(userPrompt, customPrompt);
            String proposedStr = root.has("proposedPriority") ? root.get("proposedPriority").asText("MEDIUM") :
                (root.has("recommendedPriority") ? root.get("recommendedPriority").asText("MEDIUM") : "MEDIUM");
            Priority aiProposed;
            try {
                aiProposed = Priority.valueOf(proposedStr.toUpperCase());
            } catch (Exception ex) {
                aiProposed = Priority.MEDIUM;
            }
            String reasoning = root.has("reasoning") ? root.get("reasoning").asText() : "Evaluated by Gemini";

            // Enforce domain invariant: domain rule escalation takes precedence
            Priority effective = aiProposed;
            if (domainPriority == Priority.HIGH) {
                effective = Priority.HIGH;
                reasoning += " (Domain deadline rule takes precedence: HIGH).";
            }

            return new AiPriorityRecommendationDto(task.getId().value(), aiProposed, domainPriority, effective, reasoning);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to analyze priority via Gemini API: " + exception.getMessage());
            throw new IllegalStateException("Gemini priority evaluation failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public AiTaskSummaryDto generateSummary(Task task, Project project, String customPrompt) {
        Objects.requireNonNull(task, "Task cannot be null");
        if (!providerDefinition.hasValidApiKey()) {
            throw new IllegalStateException("No active AI provider connected. Please configure and verify an API key in AI Settings.");
        }

        String userPrompt = buildTaskContextPrompt(task, project) +
            "\nGenerate an executive progress and health summary of this task. " +
            "Respond strictly in JSON format matching this schema:\n" +
            "{\n  \"summary\": \"string\",\n  \"highlights\": [\"string\"]\n}";

        try {
            JsonNode root = callGemini(userPrompt, customPrompt);
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
            LOGGER.log(Level.WARNING, "Failed to generate summary via Gemini API: " + exception.getMessage());
            throw new IllegalStateException("Gemini summary generation failed: " + exception.getMessage(), exception);
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

    private JsonNode callGemini(String userPrompt, String customPrompt) throws Exception {
        String effectiveSystemPrompt = config.resolveEffectiveSystemPrompt(customPrompt);
        String cleanBaseUrl = providerDefinition.baseUrl().replaceAll("/+$", "");
        String rawModel = providerDefinition.defaultModel();
        String model = (rawModel != null && !rawModel.isBlank()) ? rawModel.trim() : "gemini-2.5-flash";
        String endpoint = cleanBaseUrl + (model.startsWith("models/") ? "/" + model : "/models/" + model) + ":generateContent";

        Map<String, Object> payload = new HashMap<>();

        // 1. contents
        Map<String, Object> userContent = new HashMap<>();
        userContent.put("role", "user");
        userContent.put("parts", List.of(Map.of("text", userPrompt)));
        payload.put("contents", List.of(userContent));

        // 2. systemInstruction (Gemini v1beta)
        if (effectiveSystemPrompt != null && !effectiveSystemPrompt.isBlank()) {
            Map<String, Object> systemInstruction = new HashMap<>();
            systemInstruction.put("parts", List.of(Map.of("text", effectiveSystemPrompt)));
            payload.put("systemInstruction", systemInstruction);
        }

        // 3. generationConfig
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", config.getTemperature());
        generationConfig.put("maxOutputTokens", 4096);
        payload.put("generationConfig", generationConfig);

        String requestBody = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .header("x-goog-api-key", providerDefinition.apiKey().trim())
            .timeout(Duration.ofSeconds(providerDefinition.timeoutSeconds()))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Gemini provider returned HTTP " + response.statusCode() + ": " + extractErrorMessage(response.body(), objectMapper));
        }

        JsonNode responseJson = objectMapper.readTree(response.body());
        JsonNode candidates = responseJson.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IllegalStateException("Gemini provider response contains no candidates: " + extractErrorMessage(response.body(), objectMapper));
        }
        JsonNode parts = candidates.get(0).path("content").path("parts");
        StringBuilder contentBuilder = new StringBuilder();
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                if (part.has("text")) {
                    contentBuilder.append(part.get("text").asText());
                }
            }
        }
        String contentText = contentBuilder.toString();
        return parseCleanJson(contentText);
    }

    private JsonNode parseCleanJson(String rawContent) throws Exception {
        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalStateException("Empty content from Gemini provider");
        }
        String cleaned = rawContent.trim();
        int jsonBlockStart = cleaned.indexOf("```json");
        if (jsonBlockStart != -1) {
            int blockEnd = cleaned.indexOf("```", jsonBlockStart + 7);
            if (blockEnd != -1) {
                cleaned = cleaned.substring(jsonBlockStart + 7, blockEnd).trim();
            }
        } else {
            int genericBlockStart = cleaned.indexOf("```");
            if (genericBlockStart != -1) {
                int blockEnd = cleaned.indexOf("```", genericBlockStart + 3);
                if (blockEnd != -1) {
                    cleaned = cleaned.substring(genericBlockStart + 3, blockEnd).trim();
                }
            }
        }
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1).trim();
        }
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
