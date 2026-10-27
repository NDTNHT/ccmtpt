package com.taskmanagement.domain.ai;

import java.util.Collections;
import java.util.Map;

/**
 * Value object representing configuration for a user-specified custom AI provider,
 * supporting either OpenAI-compatible or Anthropic-compatible wire protocols.
 */
public record AiCustomProviderConfig(
    String format,
    String baseUrl,
    String apiKey,
    String model,
    Map<String, String> headers
) {
    public static final String FORMAT_OPENAI = "openai";
    public static final String FORMAT_ANTHROPIC = "anthropic";

    public AiCustomProviderConfig {
        format = (format != null && !format.isBlank())
            ? format.trim().toLowerCase()
            : FORMAT_OPENAI;
        baseUrl = (baseUrl != null) ? baseUrl.trim() : "";
        apiKey = (apiKey != null) ? apiKey.trim() : "";
        model = (model != null) ? model.trim() : "";
        headers = (headers != null) ? Map.copyOf(headers) : Collections.emptyMap();
    }

    public static AiCustomProviderConfig openAi(String baseUrl, String apiKey, String model) {
        return new AiCustomProviderConfig(FORMAT_OPENAI, baseUrl, apiKey, model, Collections.emptyMap());
    }

    public static AiCustomProviderConfig anthropic(String baseUrl, String apiKey, String model) {
        return new AiCustomProviderConfig(FORMAT_ANTHROPIC, baseUrl, apiKey, model, Collections.emptyMap());
    }

    public static AiCustomProviderConfig of(String format, String baseUrl, String apiKey, String model, Map<String, String> headers) {
        return new AiCustomProviderConfig(format, baseUrl, apiKey, model, headers);
    }

    public boolean isConfigured() {
        return !baseUrl.isBlank();
    }

    public boolean isAnthropicFormat() {
        return FORMAT_ANTHROPIC.equalsIgnoreCase(format);
    }

    public boolean isOpenAiFormat() {
        return !isAnthropicFormat();
    }
}
