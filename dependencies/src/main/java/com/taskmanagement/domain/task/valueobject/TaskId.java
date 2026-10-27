package com.taskmanagement.domain.task.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique task identifier.
 */
public record TaskId(UUID value) {
    public TaskId {
        Objects.requireNonNull(value, "TaskId cannot be null");
    }

    public static TaskId generate() {
        return new TaskId(UUID.randomUUID());
    }

    public static TaskId of(UUID value) {
        return new TaskId(value);
    }

    public static TaskId of(String value) {
        Objects.requireNonNull(value, "TaskId string cannot be null");
        return new TaskId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
