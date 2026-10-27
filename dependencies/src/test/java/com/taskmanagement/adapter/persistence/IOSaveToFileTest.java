package com.taskmanagement.adapter.persistence;

import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.enums.TaskStatus;
import com.taskmanagement.domain.task.valueobject.Deadline;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("JSON file persistence adapter")
class IOSaveToFileTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Round-trip: writes real JSON and reloads the full aggregate")
    void shouldPersistTaskWithSubTasks() throws Exception {
        Path file = tempDir.resolve("tasks.json");
        Project inbox = Project.createInbox();
        IOSaveToFile repository = new IOSaveToFile(file);

        Task task = Task.create(
            "Task file",
            "Mô tả có \"ngoặc kép\"",
            Deadline.of(Instant.now().plus(2, ChronoUnit.DAYS)),
            Priority.HIGH,
            inbox.getId());
        task.addSubTask("Bước 1");
        task.addSubTask("Bước 2");
        task.start();
        repository.save(task);

        assertTrue(Files.exists(file));
        assertTrue(Files.size(file) > 0);

        IOSaveToFile reopened = new IOSaveToFile(file);
        Task loaded = reopened.findById(task.getId()).orElseThrow();

        assertEquals(TaskStatus.IN_PROGRESS, loaded.getStatus());
        assertEquals(Priority.HIGH, loaded.getPriority());
        assertEquals("Mô tả có \"ngoặc kép\"", loaded.getDescription());
        assertEquals(2, loaded.getSubTasks().size());
        assertTrue(reopened.existsByProjectIdAndTitle(inbox.getId(), loaded.getTitle()));
    }

    @Test
    @DisplayName("Delete: removes the record and rewrites the file")
    void shouldDeleteTask() {
        Path file = tempDir.resolve("tasks.json");
        IOSaveToFile repository = new IOSaveToFile(file);
        Task task = Task.create(
            "Task xóa", null, null, Priority.LOW, Project.createInbox().getId());
        repository.save(task);

        repository.delete(task.getId());

        assertTrue(new IOSaveToFile(file).findById(task.getId()).isEmpty());
    }

    @Test
    @DisplayName("Missing file: starts empty")
    void shouldStartEmptyWhenFileDoesNotExist() {
        IOSaveToFile repository = new IOSaveToFile(tempDir.resolve("missing.json"));

        assertTrue(repository.findAll().isEmpty());
    }
}
