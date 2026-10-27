package com.taskmanagement.adapter.presenter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.application.dto.TaskOutputDTO;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.enums.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Web presenter")
class TaskWebPresenterTest {

    @Test
    @DisplayName("Produces valid JSON even when the title contains quotes and newlines")
    void shouldEscapeJsonProperly() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        TaskWebPresenter presenter = new TaskWebPresenter(
            new PrintStream(output, true, StandardCharsets.UTF_8));
        String trickyTitle = "He said \"hello\"\nsecond line";
        TaskOutputDTO dto = new TaskOutputDTO(
            UUID.randomUUID(),
            trickyTitle,
            "Mô tả",
            TaskStatus.TODO,
            Priority.HIGH,
            Instant.parse("2026-03-01T12:00:00Z"),
            false,
            UUID.randomUUID(),
            List.of());

        presenter.showResult(dto);

        JsonNode json = new ObjectMapper().readTree(presenter.getJsonOutput());
        assertNotNull(json);
        assertEquals(trickyTitle, json.get("title").asText());
        assertEquals("TODO", json.get("status").asText());
        assertEquals("HIGH", json.get("priority").asText());
    }
}
