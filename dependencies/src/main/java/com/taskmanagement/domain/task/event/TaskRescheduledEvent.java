package com.taskmanagement.domain.task.event;

import com.taskmanagement.domain.common.event.DomainEvent;
import com.taskmanagement.domain.task.valueobject.Deadline;
import com.taskmanagement.domain.task.valueobject.TaskId;

import java.time.Instant;
import java.util.Objects;

/**
 * Raised when a deadline is changed. {@code previousDeadline} is null when the task
 * previously had no deadline.
 */
public record TaskRescheduledEvent(
    TaskId taskId,
    Deadline previousDeadline,
    Deadline newDeadline,
    Instant occurredOn
) implements DomainEvent {
    public TaskRescheduledEvent {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(occurredOn, "occurredOn");
    }

    public TaskRescheduledEvent(TaskId taskId, Deadline previousDeadline, Deadline newDeadline) {
        this(taskId, previousDeadline, newDeadline, Instant.now());
    }
}
