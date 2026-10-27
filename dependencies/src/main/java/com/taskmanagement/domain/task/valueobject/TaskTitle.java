package com.taskmanagement.domain.task.valueobject;

import com.taskmanagement.domain.task.exception.TaskValidationException;

/**
 * Task title value object.
 */
public record TaskTitle(String value) {
    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 100;

    public TaskTitle {
        if (value == null || value.trim().isEmpty()) {
            throw new TaskValidationException("Task title cannot be empty.");
        }
        String trimmed = value.trim();
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new TaskValidationException(
                "Task title must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters.");
        }
        value = trimmed;
    }

    public boolean sameValueAs(TaskTitle other) {
        return other != null && value.equalsIgnoreCase(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
