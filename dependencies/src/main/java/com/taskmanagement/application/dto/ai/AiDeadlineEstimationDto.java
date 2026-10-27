package com.taskmanagement.application.dto.ai;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Result DTO containing AI deadline estimation and duration analysis.
 */
public record AiDeadlineEstimationDto(
    UUID taskId,
    int estimatedHours,
    Instant recommendedDeadline,
    String confidence,
    String explanation
) {
    public AiDeadlineEstimationDto {
        Objects.requireNonNull(taskId, "taskId cannot be null");
        confidence = confidence == null ? "MEDIUM" : confidence;
        explanation = explanation == null ? "" : explanation;
    }
}
