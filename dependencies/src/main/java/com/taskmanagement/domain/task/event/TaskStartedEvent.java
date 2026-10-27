package com.taskmanagement.domain.task.event;

import com.taskmanagement.domain.common.event.DomainEvent;
import com.taskmanagement.domain.task.valueobject.TaskId;

import java.time.Instant;
import java.util.Objects;

public record TaskStartedEvent(
    TaskId taskId,
    Instant occurredOn
) implements DomainEvent {
    public TaskStartedEvent {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(occurredOn, "occurredOn");
    }

    public TaskStartedEvent(TaskId taskId) {
        this(taskId, Instant.now());
    }
}
