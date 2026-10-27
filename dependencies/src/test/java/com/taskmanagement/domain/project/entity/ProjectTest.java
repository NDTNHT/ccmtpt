package com.taskmanagement.domain.project.entity;

import com.taskmanagement.domain.project.enums.ProjectType;
import com.taskmanagement.domain.project.exception.ProjectValidationException;
import com.taskmanagement.domain.project.valueobject.ProjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Project Aggregate Root")
class ProjectTest {

    @Test
    @DisplayName("Factory: creates a regular project with a trimmed name")
    void shouldCreateRegularProject() {
        Project project = Project.create("  Dự án Alpha  ", "Mô tả");

        assertEquals("Dự án Alpha", project.getName().value());
        assertEquals(ProjectType.REGULAR, project.getProjectType());
        assertFalse(project.isInbox());
    }

    @Test
    @DisplayName("Factory: creates the Inbox with INBOX type")
    void shouldCreateInbox() {
        Project inbox = Project.createInbox();

        assertTrue(inbox.isInbox());
        assertEquals(Project.INBOX_NAME, inbox.getName().value());
        assertEquals(ProjectType.INBOX, inbox.getProjectType());
    }

    @Test
    @DisplayName("Invariant: project name must be 3..100 characters")
    void shouldValidateProjectName() {
        assertThrows(ProjectValidationException.class, () -> Project.create("ab", null));
        assertThrows(ProjectValidationException.class, () -> Project.create("   ", null));
        assertThrows(ProjectValidationException.class, () -> Project.create("x".repeat(101), null));
    }

    @Test
    @DisplayName("Invariant: project description cannot exceed 500 characters")
    void shouldValidateDescription() {
        assertThrows(ProjectValidationException.class,
            () -> Project.create("Dự án", "x".repeat(501)));
    }

    @Test
    @DisplayName("Behaviour: rename and change description")
    void shouldRenameAndChangeDescription() {
        Project project = Project.create("Tên cũ", "Mô tả cũ");

        project.rename("Tên mới");
        project.changeDescription("Mô tả mới");

        assertEquals("Tên mới", project.getName().value());
        assertEquals("Mô tả mới", project.getDescription());
    }

    @Test
    @DisplayName("Identity: two instances with the same ProjectId are equal")
    void shouldUseIdentityEquality() {
        UUID id = UUID.randomUUID();
        Project first = new Project(ProjectId.of(id), "Một", "Mô tả", ProjectType.REGULAR);
        Project second = new Project(ProjectId.of(id), "Hai", "Khác", ProjectType.INBOX);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}
