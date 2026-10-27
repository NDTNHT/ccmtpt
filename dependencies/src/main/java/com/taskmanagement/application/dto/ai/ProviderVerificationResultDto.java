package com.taskmanagement.application.dto.ai;

import java.util.Collections;
import java.util.List;

/**
 * Result of testing an AI Provider endpoint and API key against the standard /models endpoint,
 * containing validity status, HTTP status code, diagnostic message, and discovered model IDs.
 */
public record ProviderVerificationResultDto(
    boolean valid,
    int statusCode,
    String message,
    List<String> availableModels,
    String protocol
) {
    public ProviderVerificationResultDto {
        availableModels = availableModels != null ? List.copyOf(availableModels) : List.of();
        message = message != null ? message : "";
        protocol = protocol != null ? protocol : "UNKNOWN";
    }

    public static ProviderVerificationResultDto success(int statusCode, List<String> models, String protocol) {
        List<String> safeModels = models != null ? List.copyOf(models) : Collections.emptyList();
        String msg = "API Key va Provider xac minh thanh cong! Tim thay " + safeModels.size() + " models.";
        return new ProviderVerificationResultDto(true, statusCode, msg, safeModels, protocol);
    }

    public static ProviderVerificationResultDto failure(int statusCode, String message, String protocol) {
        return new ProviderVerificationResultDto(false, statusCode, message, Collections.emptyList(), protocol);
    }
}
