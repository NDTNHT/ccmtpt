package com.taskmanagement.domain.ai;

import java.util.Objects;

/**
 * Value object encapsulating system prompts, runtime instructions, temperature,
 * and optional custom provider overrides for AI operations.
 */
public record AiPromptConfig(
    String systemPrompt,
    String additionalInstructions,
    Double temperature,
    String providerOverride,
    AiCustomProviderConfig customProviderConfig
) {
    public static final String DEFAULT_SYSTEM_PROMPT =
        "You are an expert AI task management assistant. Analyze tasks, decompose them into actionable subtasks, " +
        "estimate realistic completion deadlines, evaluate priorities, and produce clear summaries. " +
        "Always output strictly valid JSON matching the requested structure.";

    public static final double DEFAULT_TEMPERATURE = 0.3;

    public AiPromptConfig {
        systemPrompt = (systemPrompt == null || systemPrompt.isBlank())
            ? DEFAULT_SYSTEM_PROMPT
            : systemPrompt.trim();
        additionalInstructions = (additionalInstructions == null)
            ? ""
            : additionalInstructions.trim();
        temperature = (temperature == null || temperature < 0.0 || temperature > 2.0)
            ? DEFAULT_TEMPERATURE
            : temperature;
        providerOverride = (providerOverride != null && !providerOverride.isBlank())
            ? providerOverride.trim().toLowerCase()
            : null;
    }

    public AiPromptConfig(String systemPrompt, String additionalInstructions, Double temperature) {
        this(systemPrompt, additionalInstructions, temperature, null, null);
    }

    public static AiPromptConfig defaultConfig() {
        return new AiPromptConfig(DEFAULT_SYSTEM_PROMPT, "", DEFAULT_TEMPERATURE, null, null);
    }

    public static AiPromptConfig of(String systemPrompt, String additionalInstructions) {
        return new AiPromptConfig(systemPrompt, additionalInstructions, DEFAULT_TEMPERATURE, null, null);
    }

    public static AiPromptConfig of(String systemPrompt, String additionalInstructions, Double temperature) {
        return new AiPromptConfig(systemPrompt, additionalInstructions, temperature, null, null);
    }

    public AiPromptConfig withAdditionalInstructions(String instructions) {
        return new AiPromptConfig(this.systemPrompt, instructions, this.temperature, this.providerOverride, this.customProviderConfig);
    }

    public AiPromptConfig withSystemPrompt(String newSystemPrompt) {
        return new AiPromptConfig(newSystemPrompt, this.additionalInstructions, this.temperature, this.providerOverride, this.customProviderConfig);
    }

    public AiPromptConfig withCustomProvider(AiCustomProviderConfig customProviderConfig) {
        return new AiPromptConfig(this.systemPrompt, this.additionalInstructions, this.temperature, "custom", customProviderConfig);
    }

    public AiPromptConfig withProviderOverride(String provider) {
        return new AiPromptConfig(this.systemPrompt, this.additionalInstructions, this.temperature, provider, this.customProviderConfig);
    }

    public boolean hasCustomProvider() {
        return customProviderConfig != null && customProviderConfig.isConfigured();
    }
}
