package com.taskmanagement.application.dto;

import com.taskmanagement.domain.project.enums.ProjectType;

import java.util.UUID;

/**
 * Read model for a Project.
 */
public record ProjectOutputDTO(
    UUID id,
    String name,
    String description,
    ProjectType type
) {
}
