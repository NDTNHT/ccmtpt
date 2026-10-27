package com.taskmanagement.adapter.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * REST request model for changing a task deadline.
 */
public record TaskRescheduleRequest(
    @NotNull(message = "New due date is required")
    Instant dueDate
) {
}
