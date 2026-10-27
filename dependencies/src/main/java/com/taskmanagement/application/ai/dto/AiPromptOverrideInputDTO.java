package com.taskmanagement.application.ai.dto;

import com.taskmanagement.domain.ai.AiCustomProviderConfig;

import java.util.Map;

/**
 * Input DTO to override system prompt, add specific instructions,
 * or provide on-the-fly custom provider configuration per request.
 */
public record AiPromptOverrideInputDTO(
    String systemPrompt,
    String additionalInstructions,
    String provider,
    String customFormat,
    String customBaseUrl,
    String customApiKey,
    String customModel,
    Map<String, String> customHeaders
) {
    public AiPromptOverrideInputDTO(String systemPrompt, String additionalInstructions) {
        this(systemPrompt, additionalInstructions, null, null, null, null, null, null);
    }

    public boolean hasCustomProvider() {
        return (customBaseUrl != null && !customBaseUrl.isBlank()) ||
               (provider != null && "custom".equalsIgnoreCase(provider.trim()) && customBaseUrl != null && !customBaseUrl.isBlank());
    }

    public AiCustomProviderConfig toCustomProviderConfig() {
        if (!hasCustomProvider()) {
            return null;
        }
        return new AiCustomProviderConfig(
            customFormat,
            customBaseUrl,
            customApiKey,
            customModel,
            customHeaders
        );
    }
}
