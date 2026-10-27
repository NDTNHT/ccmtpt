package com.taskmanagement.adapter.api.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request payload to apply an AI-generated checklist (subtasks) to a task.
 */
public record AiApplyDecompositionRequest(
    @NotEmpty(message = "SubTasks list cannot be empty")
    List<String> subTasks
) {
}
