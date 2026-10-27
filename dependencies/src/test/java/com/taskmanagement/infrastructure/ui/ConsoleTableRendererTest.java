package com.taskmanagement.infrastructure.ui;

import com.taskmanagement.application.dto.ProjectOutputDTO;
import com.taskmanagement.application.dto.TaskOutputDTO;
import com.taskmanagement.domain.project.enums.ProjectType;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ConsoleTableRenderer")
class ConsoleTableRendererTest {
    private ByteArrayOutputStream outContent;
    private ConsoleTableRenderer renderer;

    @BeforeEach
    void setUp() {
        outContent = new ByteArrayOutputStream();
        renderer = new ConsoleTableRenderer(new PrintStream(outContent, true, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Renders the menu without a current task")
    void shouldRenderMenuWithoutCurrentTaskId() {
        renderer.renderMenu(null);
        String output = outContent.toString(StandardCharsets.UTF_8);

        assertTrue(output.contains("HE THONG QUAN LY TAC VU"));
        assertFalse(output.contains("Task hien tai:"));
    }

    @Test
    @DisplayName("Renders the menu with a current task")
    void shouldRenderMenuWithCurrentTaskId() {
        UUID taskId = UUID.randomUUID();
        renderer.renderMenu(taskId);
        String output = outContent.toString(StandardCharsets.UTF_8);

        assertTrue(output.contains("Task hien tai: " + taskId));
    }

    @Test
    @DisplayName("Renders the empty state")
    void shouldRenderEmptyTaskTableMessage() {
        renderer.renderTaskTable(Collections.emptyList());
        String output = outContent.toString(StandardCharsets.UTF_8);

        assertTrue(output.contains("Chua co task nao trong he thong."));
    }

    @Test
    @DisplayName("Renders task rows using adapter-level localized labels")
    void shouldRenderTaskTableWithData() {
        UUID taskId = UUID.randomUUID();
        TaskOutputDTO dto = new TaskOutputDTO(
            taskId,
            "Bao cao kien truc",
            "Mo ta chi tiet",
            TaskStatus.TODO,
            Priority.HIGH,
            Instant.now(),
            false,
            UUID.randomUUID(),
            List.of());
        renderer.renderTaskTable(List.of(dto));
        String output = outContent.toString(StandardCharsets.UTF_8);

        assertTrue(output.contains(taskId.toString()));
        assertTrue(output.contains("Bao cao kien truc"));
        assertTrue(output.contains("Cao"));
        assertTrue(output.contains("Cần làm"));
    }

    @Test
    @DisplayName("Renders the project list and the create-new option")
    void shouldRenderProjectList() {
        UUID projectId = UUID.randomUUID();
        ProjectOutputDTO dto = new ProjectOutputDTO(
            projectId, "Inbox", "Hòm thư tạm", ProjectType.INBOX);
        renderer.renderProjectList(List.of(dto));
        String output = outContent.toString(StandardCharsets.UTF_8);

        assertTrue(output.contains("[INBOX] Inbox"));
        assertTrue(output.contains("Tao Project moi..."));
    }
}
