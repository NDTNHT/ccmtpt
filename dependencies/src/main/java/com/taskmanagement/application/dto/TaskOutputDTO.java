package com.taskmanagement.application.dto;

import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.enums.TaskStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read model returned by task use cases.
 */
public record TaskOutputDTO(
    UUID taskId,
    String title,
    String description,
    TaskStatus status,
    Priority priority,
    Instant dueDate,
    boolean isOverdue,
    UUID projectId,
    List<SubTaskOutputDTO> subTasks
) {
    public TaskOutputDTO {
        subTasks = subTasks == null ? List.of() : List.copyOf(subTasks);
    }
}
