package com.taskmanagement.application.exception;

/**
 * Raised when a task does not exist.
 */
public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(String message) {
        super(message);
    }
}
