package com.taskmanagement.application.dto.ai;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Result DTO containing checklist items (subtasks) extracted or suggested by AI for a task.
 */
public record AiTaskDecompositionDto(
    UUID taskId,
    List<String> subTasks,
    String rationale
) {
    public AiTaskDecompositionDto {
        Objects.requireNonNull(taskId, "taskId cannot be null");
        subTasks = subTasks == null ? List.of() : List.copyOf(subTasks);
        rationale = rationale == null ? "" : rationale;
    }
}
