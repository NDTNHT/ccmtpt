package com.taskmanagement.adapter.ai.dto;

import com.taskmanagement.application.ai.dto.AiPromptOverrideInputDTO;

import java.util.Map;

/**
 * REST request payload for overriding system prompt, adding specific instructions,
 * or specifying a custom AI provider (OpenAI or Anthropic format) on the fly.
 */
public record AiPromptOverrideRequest(
    String systemPrompt,
    String additionalInstructions,
    String provider,
    String customFormat,
    String customBaseUrl,
    String customApiKey,
    String customModel,
    Map<String, String> customHeaders
) {
    public AiPromptOverrideRequest(String systemPrompt, String additionalInstructions) {
        this(systemPrompt, additionalInstructions, null, null, null, null, null, null);
    }

    public AiPromptOverrideInputDTO toInputDTO() {
        return new AiPromptOverrideInputDTO(
            systemPrompt,
            additionalInstructions,
            provider,
            customFormat,
            customBaseUrl,
            customApiKey,
            customModel,
            customHeaders
        );
    }
}
