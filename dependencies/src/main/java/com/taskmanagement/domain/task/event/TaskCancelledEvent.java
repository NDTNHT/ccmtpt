package com.taskmanagement.domain.task.event;

import com.taskmanagement.domain.common.event.DomainEvent;
import com.taskmanagement.domain.task.valueobject.TaskId;

import java.time.Instant;
import java.util.Objects;

public record TaskCancelledEvent(
    TaskId taskId,
    Instant occurredOn
) implements DomainEvent {
    public TaskCancelledEvent {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(occurredOn, "occurredOn");
    }

    public TaskCancelledEvent(TaskId taskId) {
        this(taskId, Instant.now());
    }
}
