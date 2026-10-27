package com.taskmanagement.domain.project.entity;

import com.taskmanagement.domain.common.entity.AggregateRoot;
import com.taskmanagement.domain.project.enums.ProjectType;
import com.taskmanagement.domain.project.exception.ProjectValidationException;
import com.taskmanagement.domain.project.valueobject.ProjectId;
import com.taskmanagement.domain.project.valueobject.ProjectName;

import java.util.UUID;

/**
 * Project aggregate root. Tasks reference a project by {@link ProjectId}.
 */
public class Project extends AggregateRoot<ProjectId> {
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    public static final String INBOX_NAME = "INBOX";

    private ProjectName name;
    private String description;
    private final ProjectType projectType;

    public Project(ProjectId id, String name, String description, ProjectType projectType) {
        super(id != null ? id : ProjectId.generate());
        this.name = new ProjectName(name);
        this.description = validateDescription(description);
        this.projectType = projectType != null ? projectType : ProjectType.REGULAR;
    }

    public Project(UUID id, String name, String description, ProjectType projectType) {
        this(id != null ? ProjectId.of(id) : ProjectId.generate(), name, description, projectType);
    }

    public static Project createInbox() {
        return new Project(
            ProjectId.generate(),
            INBOX_NAME,
            "Default inbox for tasks that have not been classified yet.",
            ProjectType.INBOX);
    }

    public static Project create(String name, String description) {
        return new Project(ProjectId.generate(), name, description, ProjectType.REGULAR);
    }

    public void rename(String newName) {
        ProjectName candidate = new ProjectName(newName);
        if (candidate.equals(this.name)) {
            return;
        }
        this.name = candidate;
    }

    public void changeDescription(String newDescription) {
        this.description = validateDescription(newDescription);
    }

    public boolean isInbox() {
        return projectType == ProjectType.INBOX;
    }

    private static String validateDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ProjectValidationException(
                "Project description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters.");
        }
        return description;
    }

    public ProjectName getName() { return name; }
    public String getDescription() { return description; }
    public ProjectType getProjectType() { return projectType; }
    public ProjectId getProjectId() { return id; }
}
