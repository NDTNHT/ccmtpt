package com.taskmanagement.adapter.api.dto;

import com.taskmanagement.domain.task.enums.Priority;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload to apply an AI-recommended priority.
 */
public record AiApplyPriorityRequest(
    @NotNull(message = "Priority cannot be null")
    Priority priority
) {
}
