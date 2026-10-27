package com.taskmanagement.adapter.api.dto;

import com.taskmanagement.adapter.ai.AiProtocol;

/**
 * Request payload for verifying provider connectivity and key validity via /models endpoint.
 */
public record VerifyProviderRequest(
    String baseUrl,
    String apiKey,
    AiProtocol protocol,
    Integer timeoutSeconds
) {
    public VerifyProviderRequest {
        if (protocol == null) {
            protocol = AiProtocol.OPENAI;
        }
        if (timeoutSeconds == null || timeoutSeconds <= 0) {
            timeoutSeconds = 15;
        }
    }
}
