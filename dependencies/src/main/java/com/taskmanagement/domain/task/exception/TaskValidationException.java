package com.taskmanagement.domain.task.exception;

import com.taskmanagement.domain.common.exception.DomainException;

/**
 * Raised when a Task invariant or input validation rule is violated.
 */
public class TaskValidationException extends DomainException {
    public TaskValidationException(String message) {
        super(message);
    }
}
