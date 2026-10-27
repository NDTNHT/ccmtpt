package com.taskmanagement.adapter.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * REST request model for project creation.
 */
public record ProjectCreateRequest(
    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 100, message = "Project name must be between 3 and 100 characters")
    String name,

    @Size(max = 500, message = "Project description cannot exceed 500 characters")
    String description
) {
}
