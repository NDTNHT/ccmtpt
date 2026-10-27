package com.taskmanagement.domain.project.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique project identifier.
 */
public record ProjectId(UUID value) {
    public ProjectId {
        Objects.requireNonNull(value, "ProjectId cannot be null");
    }

    public static ProjectId generate() {
        return new ProjectId(UUID.randomUUID());
    }

    public static ProjectId of(UUID value) {
        return new ProjectId(value);
    }

    public static ProjectId of(String value) {
        Objects.requireNonNull(value, "ProjectId string cannot be null");
        return new ProjectId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
