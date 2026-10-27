package com.taskmanagement.application.dto;

import com.taskmanagement.domain.task.enums.Priority;

import java.time.Instant;
import java.util.UUID;

/**
 * Input data for the create-task use case. A null priority is calculated from the
 * deadline and project context.
 */
public record TaskInputDTO(
    String title,
    String description,
    Instant dueDate,
    Priority priority,
    UUID projectId
) {
}
