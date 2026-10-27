package com.taskmanagement.domain.task.event;

import com.taskmanagement.domain.common.event.DomainEvent;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.valueobject.TaskId;

import java.time.Instant;
import java.util.Objects;

/**
 * Raised when the priority of a Task changes.
 */
public record TaskPriorityChangedEvent(
    TaskId taskId,
    Priority previousPriority,
    Priority newPriority,
    Instant occurredOn
) implements DomainEvent {
    public TaskPriorityChangedEvent {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(previousPriority, "previousPriority");
        Objects.requireNonNull(newPriority, "newPriority");
        Objects.requireNonNull(occurredOn, "occurredOn");
    }

    public TaskPriorityChangedEvent(TaskId taskId, Priority previousPriority, Priority newPriority) {
        this(taskId, previousPriority, newPriority, Instant.now());
    }
}
