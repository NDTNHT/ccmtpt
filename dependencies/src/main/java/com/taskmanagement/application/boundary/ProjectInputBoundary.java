package com.taskmanagement.application.boundary;

import com.taskmanagement.application.dto.ProjectOutputDTO;

import java.util.List;

/**
 * Project use cases.
 */
public interface ProjectInputBoundary {
    List<ProjectOutputDTO> getProjects();

    ProjectOutputDTO getInboxProject();

    ProjectOutputDTO createProject(String name, String description);
}
