package com.taskmanagement.adapter.persistence;

import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.enums.TaskStatus;
import com.taskmanagement.domain.task.valueobject.Deadline;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SQLite persistence adapters")
class IOSaveToSQLiteTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Round-trip: task, status transitions and subtasks survive a reopen")
    void shouldPersistAggregateAcrossInstances() {
        Path database = tempDir.resolve("tasks.db");
        IOSaveToSQLiteProject projectRepository = new IOSaveToSQLiteProject(database);
        Project inbox = projectRepository.getInbox();
        IOSaveToSQLite repository = new IOSaveToSQLite(database);

        Task task = Task.create(
            "Task SQLite",
            "Mô tả",
            Deadline.of(Instant.now().plus(1, ChronoUnit.DAYS)),
            Priority.MEDIUM,
            inbox.getId());
        task.addSubTask("Viết phần 1");
        task.addSubTask("Viết phần 2");
        repository.save(task);

        task.start();
        repository.save(task);

        IOSaveToSQLite reopened = new IOSaveToSQLite(database);
        Task loaded = reopened.findById(task.getId()).orElseThrow();

        assertEquals(TaskStatus.IN_PROGRESS, loaded.getStatus());
        assertEquals(Priority.MEDIUM, loaded.getPriority());
        assertEquals(2, loaded.getSubTasks().size());
        assertEquals("Viết phần 1", loaded.getSubTasks().get(0).getTitle());
        assertEquals(inbox.getId(), loaded.getProjectId());
        assertTrue(reopened.existsByProjectIdAndTitle(inbox.getId(), loaded.getTitle()));
    }

    @Test
    @DisplayName("Inbox: the same project row is reused across adapter instances")
    void shouldReuseInboxAcrossInstances() {
        Path database = tempDir.resolve("tasks.db");
        Project first = new IOSaveToSQLiteProject(database).getInbox();
        Project second = new IOSaveToSQLiteProject(database).getInbox();

        assertEquals(first.getId(), second.getId());
        assertEquals(Project.INBOX_NAME, second.getName().value());
    }

    @Test
    @DisplayName("Delete: removes the task and its subtasks")
    void shouldDeleteTaskAndSubTasks() {
        Path database = tempDir.resolve("tasks.db");
        IOSaveToSQLiteProject projectRepository = new IOSaveToSQLiteProject(database);
        IOSaveToSQLite repository = new IOSaveToSQLite(database);

        Task task = Task.create(
            "Task cần xóa", null, null, Priority.LOW, projectRepository.getInbox().getId());
        task.addSubTask("Con");
        repository.save(task);

        repository.delete(task.getId());

        Optional<Task> loaded = new IOSaveToSQLite(database).findById(task.getId());
        assertTrue(loaded.isEmpty());
    }

    @Test
    @DisplayName("Filtering: findByProjectId only returns matching tasks")
    void shouldFilterByProject() {
        Path database = tempDir.resolve("tasks.db");
        IOSaveToSQLiteProject projectRepository = new IOSaveToSQLiteProject(database);
        IOSaveToSQLite repository = new IOSaveToSQLite(database);
        Project inbox = projectRepository.getInbox();
        Project other = Project.create("Dự án khác", "Mô tả");
        projectRepository.save(other);

        Task inboxTask = Task.create("Task inbox", null, null, Priority.LOW, inbox.getId());
        Task otherTask = Task.create("Task khác", null, null, Priority.LOW, other.getId());
        repository.save(inboxTask);
        repository.save(otherTask);

        assertEquals(1, repository.findByProjectId(inbox.getId()).size());
        assertEquals(1, repository.findByProjectId(other.getId()).size());
        assertFalse(repository.findAll().isEmpty());
    }
}
