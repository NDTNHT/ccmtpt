package com.taskmanagement.domain.task.event;

import com.taskmanagement.domain.common.event.DomainEvent;
import com.taskmanagement.domain.task.valueobject.TaskId;

import java.time.Instant;
import java.util.Objects;

public record TaskCompletedEvent(
    TaskId taskId,
    Instant occurredOn
) implements DomainEvent {
    public TaskCompletedEvent {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(occurredOn, "occurredOn");
    }

    public TaskCompletedEvent(TaskId taskId) {
        this(taskId, Instant.now());
    }
}
