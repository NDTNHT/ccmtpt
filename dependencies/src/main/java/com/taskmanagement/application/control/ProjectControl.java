package com.taskmanagement.application.control;

import com.taskmanagement.application.boundary.ProjectInputBoundary;
import com.taskmanagement.application.dto.ProjectOutputDTO;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.project.repository.ProjectRepository;

import java.util.List;
import java.util.Objects;

/**
 * Project application service.
 */
public class ProjectControl implements ProjectInputBoundary {
    private final ProjectRepository projectRepository;

    public ProjectControl(ProjectRepository projectRepository) {
        this.projectRepository = Objects.requireNonNull(
            projectRepository, "ProjectRepository cannot be null");
    }

    @Override
    public List<ProjectOutputDTO> getProjects() {
        return projectRepository.findAll().stream()
            .map(ProjectControl::toOutput)
            .toList();
    }

    @Override
    public ProjectOutputDTO getInboxProject() {
        return toOutput(projectRepository.getInbox());
    }

    @Override
    public ProjectOutputDTO createProject(String name, String description) {
        Project project = Project.create(name, description);
        projectRepository.save(project);
        return toOutput(project);
    }

    private static ProjectOutputDTO toOutput(Project project) {
        return new ProjectOutputDTO(
            project.getId().value(),
            project.getName().value(),
            project.getDescription(),
            project.getProjectType());
    }
}
