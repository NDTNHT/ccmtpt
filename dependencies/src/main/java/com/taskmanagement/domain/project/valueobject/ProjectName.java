package com.taskmanagement.domain.project.valueobject;

import com.taskmanagement.domain.project.exception.ProjectValidationException;

/**
 * Project name value object. Guarantees a non-blank name of 3 to 100 characters.
 */
public record ProjectName(String value) {
    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 100;

    public ProjectName {
        if (value == null || value.trim().isEmpty()) {
            throw new ProjectValidationException("Project name cannot be empty.");
        }
        String trimmed = value.trim();
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new ProjectValidationException(
                "Project name must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters.");
        }
        value = trimmed;
    }

    public boolean sameValueAs(ProjectName other) {
        return other != null && value.equalsIgnoreCase(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
