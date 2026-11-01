package com.taskmanagement.adapter.api.dto;

import com.taskmanagement.domain.task.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * REST request model for task creation.
 */
public record TaskCreateRequest(
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    String title,

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    String description,

    Instant dueDate,

    Priority priority,

    UUID projectId,

    @Size(max = 100)
    java.util.List<@NotBlank @Size(max = 200) String> subTasks
) {
    public TaskCreateRequest(String title, String description, Instant dueDate, Priority priority, UUID projectId) {
        this(title, description, dueDate, priority, projectId, java.util.List.of());
    }
}
