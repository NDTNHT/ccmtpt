package com.taskmanagement.adapter.persistence;

import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.project.repository.ProjectRepository;
import com.taskmanagement.domain.project.valueobject.ProjectId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory project repository. The Inbox project is created on startup.
 */
public class IOSaveToMemoryProject implements ProjectRepository {
    private final Map<ProjectId, Project> projectDb = new LinkedHashMap<>();
    private final Project inbox;

    public IOSaveToMemoryProject() {
        this.inbox = Project.createInbox();
        projectDb.put(inbox.getId(), inbox);
    }

    @Override
    public synchronized void save(Project project) {
        Objects.requireNonNull(project, "Project cannot be null");
        projectDb.put(project.getId(), project);
    }

    @Override
    public synchronized Optional<Project> findById(ProjectId id) {
        return Optional.ofNullable(projectDb.get(id));
    }

    @Override
    public synchronized Project getInbox() {
        return inbox;
    }

    @Override
    public synchronized List<Project> findAll() {
        return new ArrayList<>(projectDb.values());
    }
}
