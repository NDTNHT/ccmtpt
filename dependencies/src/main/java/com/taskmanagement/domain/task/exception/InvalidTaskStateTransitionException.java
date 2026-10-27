package com.taskmanagement.domain.task.exception;

import com.taskmanagement.domain.common.exception.DomainException;

/**
 * Raised when a behaviour is not allowed from the current Task lifecycle state.
 */
public class InvalidTaskStateTransitionException extends DomainException {
    public InvalidTaskStateTransitionException(String message) {
        super(message);
    }
}
