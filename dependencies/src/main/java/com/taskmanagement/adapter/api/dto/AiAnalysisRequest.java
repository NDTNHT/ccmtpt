package com.taskmanagement.adapter.api.dto;

/**
 * Request payload for AI operations supporting optional write-in custom system prompts.
 */
public record AiAnalysisRequest(
    String customSystemPrompt
) {
    public AiAnalysisRequest() {
        this(null);
    }
}
