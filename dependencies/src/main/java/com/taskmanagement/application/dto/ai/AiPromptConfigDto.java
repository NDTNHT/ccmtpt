package com.taskmanagement.application.dto.ai;

import java.util.Objects;

/**
 * Carries runtime AI prompt configuration including custom system prompts and model hyperparameters.
 */
public record AiPromptConfigDto(
    String model,
    String customSystemPrompt,
    Double temperature
) {
    public static AiPromptConfigDto defaults() {
        return new AiPromptConfigDto(null, null, null);
    }

    public static AiPromptConfigDto withCustomPrompt(String customSystemPrompt) {
        return new AiPromptConfigDto(null, customSystemPrompt, null);
    }

    public boolean hasCustomPrompt() {
        return customSystemPrompt != null && !customSystemPrompt.isBlank();
    }
}
