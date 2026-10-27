package com.taskmanagement.domain.project.repository;

import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.project.valueobject.ProjectId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence contract for projects.
 */
public interface ProjectRepository {
    void save(Project project);

    Optional<Project> findById(ProjectId id);

    default Optional<Project> findById(UUID id) {
        return findById(ProjectId.of(id));
    }

    Project getInbox();

    List<Project> findAll();
}
