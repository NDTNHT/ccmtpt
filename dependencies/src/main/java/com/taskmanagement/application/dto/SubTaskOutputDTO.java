package com.taskmanagement.application.dto;

import java.util.UUID;

/**
 * Read model for a child SubTask.
 */
public record SubTaskOutputDTO(
    UUID id,
    String title,
    boolean completed
) {
}
