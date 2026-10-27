package com.taskmanagement.domain.task.event;

import com.taskmanagement.domain.common.event.DomainEvent;
import com.taskmanagement.domain.project.valueobject.ProjectId;
import com.taskmanagement.domain.task.valueobject.TaskId;
import com.taskmanagement.domain.task.valueobject.TaskTitle;

import java.time.Instant;
import java.util.Objects;

/**
 * Raised when a new Task is created. Never raised when rehydrating from storage.
 */
public record TaskCreatedEvent(
    TaskId taskId,
    TaskTitle title,
    ProjectId projectId,
    Instant occurredOn
) implements DomainEvent {
    public TaskCreatedEvent {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(occurredOn, "occurredOn");
    }

    public TaskCreatedEvent(TaskId taskId, TaskTitle title, ProjectId projectId) {
        this(taskId, title, projectId, Instant.now());
    }
}
