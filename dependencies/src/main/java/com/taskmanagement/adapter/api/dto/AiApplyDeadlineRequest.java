package com.taskmanagement.adapter.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request payload to apply an AI-estimated completion deadline.
 */
public record AiApplyDeadlineRequest(
    @NotNull(message = "Recommended deadline cannot be null")
    Instant recommendedDeadline
) {
}
