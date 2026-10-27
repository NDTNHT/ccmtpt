package com.taskmanagement.adapter.api.dto;

/**
 * Carries AI provider and authentication configuration details.
 */
public record AiConfigDto(
    String provider,
    String baseUrl,
    String model,
    String apiKey,
    int timeoutSeconds
) {
}
