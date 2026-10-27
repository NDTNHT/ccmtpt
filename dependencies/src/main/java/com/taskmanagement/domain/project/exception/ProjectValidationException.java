package com.taskmanagement.domain.project.exception;

import com.taskmanagement.domain.common.exception.DomainException;

/**
 * Raised when a Project invariant is violated.
 */
public class ProjectValidationException extends DomainException {
    public ProjectValidationException(String message) {
        super(message);
    }
}
