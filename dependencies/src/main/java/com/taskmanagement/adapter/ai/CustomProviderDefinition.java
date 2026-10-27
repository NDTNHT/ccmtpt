package com.taskmanagement.adapter.ai;

import java.util.List;
import java.util.Objects;

/**
 * Definition for a user-configured custom AI provider specifying endpoint, credentials,
 * protocol (OpenAI or Anthropic), default model, and discovered available models.
 */
public record CustomProviderDefinition(
    String id,
    String name,
    AiProtocol protocol,
    String baseUrl,
    String apiKey,
    String defaultModel,
    int timeoutSeconds,
    List<String> availableModels
) {
    public CustomProviderDefinition(
        String id,
        String name,
        AiProtocol protocol,
        String baseUrl,
        String apiKey,
        String defaultModel,
        int timeoutSeconds
    ) {
        this(id, name, protocol, baseUrl, apiKey, defaultModel, timeoutSeconds, List.of());
    }

    public CustomProviderDefinition {
        Objects.requireNonNull(id, "Provider id cannot be null");
        id = id.trim().toLowerCase();
        name = name != null && !name.isBlank() ? name.trim() : id;
        protocol = protocol != null ? protocol : AiProtocol.OPENAI;
        baseUrl = baseUrl != null ? baseUrl.replaceAll("/+$", "") : "https://api.openai.com/v1";
        apiKey = apiKey != null ? apiKey.trim() : "";
        defaultModel = defaultModel != null && !defaultModel.isBlank() ? defaultModel.trim() : "default";
        timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 30;
        availableModels = availableModels != null ? List.copyOf(availableModels) : List.of();
    }

    public static CustomProviderDefinition ofOpenAi(String id, String name, String baseUrl, String apiKey, String defaultModel) {
        return new CustomProviderDefinition(id, name, AiProtocol.OPENAI, baseUrl, apiKey, defaultModel, 30, List.of());
    }

    public static CustomProviderDefinition ofAnthropic(String id, String name, String baseUrl, String apiKey, String defaultModel) {
        return new CustomProviderDefinition(id, name, AiProtocol.ANTHROPIC, baseUrl, apiKey, defaultModel, 30, List.of());
    }

    public boolean hasValidApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String maskedApiKey() {
        if (!hasValidApiKey()) {
            return "(None)";
        }
        if (apiKey.length() > 6) {
            return apiKey.substring(0, 3) + "..." + apiKey.substring(apiKey.length() - 3);
        }
        return "***";
    }

    public CustomProviderDefinition withAvailableModels(List<String> models) {
        return new CustomProviderDefinition(id, name, protocol, baseUrl, apiKey, defaultModel, timeoutSeconds, models);
    }

    public CustomProviderDefinition withDefaultModel(String newDefaultModel) {
        return new CustomProviderDefinition(id, name, protocol, baseUrl, apiKey, newDefaultModel, timeoutSeconds, availableModels);
    }

    public CustomProviderDefinition withApiKey(String newApiKey) {
        return new CustomProviderDefinition(id, name, protocol, baseUrl, newApiKey, defaultModel, timeoutSeconds, availableModels);
    }
}
