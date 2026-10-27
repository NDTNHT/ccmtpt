package com.taskmanagement.application.control;

import com.taskmanagement.adapter.persistence.IOSaveToMemoryProject;
import com.taskmanagement.application.dto.ProjectOutputDTO;
import com.taskmanagement.domain.project.enums.ProjectType;
import com.taskmanagement.domain.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProjectControl Application Service")
class ProjectControlTest {
    private ProjectRepository projectRepository;
    private ProjectControl control;

    @BeforeEach
    void setUp() {
        projectRepository = new IOSaveToMemoryProject();
        control = new ProjectControl(projectRepository);
    }

    @Test
    @DisplayName("Read: the Inbox is always present")
    void shouldExposeInbox() {
        assertTrue(control.getProjects().stream()
            .anyMatch(project -> project.type() == ProjectType.INBOX));
        assertEquals(ProjectType.INBOX, control.getInboxProject().type());
    }

    @Test
    @DisplayName("Create: persists a regular project and returns its read model")
    void shouldCreateProject() {
        ProjectOutputDTO created = control.createProject("  Dự án mới  ", "Mô tả");

        assertEquals("Dự án mới", created.name());
        assertEquals(ProjectType.REGULAR, created.type());
        assertEquals(2, control.getProjects().size());
    }

    @Test
    @DisplayName("DIP: a null repository is rejected eagerly")
    void shouldRejectNullRepository() {
        assertThrows(NullPointerException.class, () -> new ProjectControl(null));
    }
}
