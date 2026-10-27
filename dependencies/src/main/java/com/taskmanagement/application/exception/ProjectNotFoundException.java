package com.taskmanagement.application.exception;

/**
 * Raised when a project does not exist.
 */
public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(String message) {
        super(message);
    }
}
