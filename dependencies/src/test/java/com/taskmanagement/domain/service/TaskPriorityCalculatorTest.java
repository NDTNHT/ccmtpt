package com.taskmanagement.domain.service;

import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.project.enums.ProjectType;
import com.taskmanagement.domain.project.valueobject.ProjectId;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.valueobject.Deadline;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("TaskPriorityCalculator Domain Service")
class TaskPriorityCalculatorTest {
    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");

    private final TaskPriorityCalculator calculator = new TaskPriorityCalculator();
    private final Project regularProject =
        new Project(ProjectId.generate(), "Phát triển Core", "Dự án chính", ProjectType.REGULAR);
    private final Project inboxProject =
        new Project(ProjectId.generate(), "INBOX", "Hộp thư", ProjectType.INBOX);
    private final UUID taskProjectId = UUID.randomUUID();

    @Test
    @DisplayName("Overdue task in a REGULAR project -> HIGH")
    void shouldReturnHighForOverdueTask() {
        Deadline overdue = Deadline.of(NOW.minus(1, ChronoUnit.DAYS));
        Task task = Task.create("Task quá hạn", "Mô tả", overdue, Priority.LOW, taskProjectId);

        assertEquals(Priority.HIGH, calculator.calculatePriority(task, regularProject, NOW));
    }

    @Test
    @DisplayName("Task due within two days in a REGULAR project -> HIGH")
    void shouldReturnHighForApproachingTask() {
        Deadline approaching = Deadline.of(NOW.plus(1, ChronoUnit.DAYS));
        Task task = Task.create("Task sát hạn", "Mô tả", approaching, Priority.LOW, taskProjectId);

        assertEquals(Priority.HIGH, calculator.calculatePriority(task, regularProject, NOW));
    }

    @Test
    @DisplayName("Task due within a week but beyond two days -> MEDIUM")
    void shouldReturnMediumForWeekWindow() {
        Deadline soon = Deadline.of(NOW.plus(5, ChronoUnit.DAYS));
        Task task = Task.create("Task trong tuần", "Mô tả", soon, Priority.LOW, taskProjectId);

        assertEquals(Priority.MEDIUM, calculator.calculatePriority(task, regularProject, NOW));
    }

    @Test
    @DisplayName("Cross-aggregate rule: an urgent INBOX task is capped at MEDIUM")
    void shouldCapInboxTaskAtMedium() {
        Deadline approaching = Deadline.of(NOW.plus(1, ChronoUnit.DAYS));
        Task task = Task.create("Task chưa phân loại", "Mô tả", approaching, Priority.LOW, taskProjectId);

        assertEquals(Priority.MEDIUM, calculator.calculatePriority(task, inboxProject, NOW));
    }

    @Test
    @DisplayName("INBOX task that is not urgent keeps the user priority")
    void shouldKeepUserPriorityForCalmInboxTask() {
        Deadline far = Deadline.of(NOW.plus(20, ChronoUnit.DAYS));
        Task task = Task.create("Task inbox", "Mô tả", far, Priority.HIGH, taskProjectId);

        assertEquals(Priority.HIGH, calculator.calculatePriority(task, inboxProject, NOW));
    }

    @Test
    @DisplayName("Far deadline in a REGULAR project keeps the user priority")
    void shouldKeepUserPriorityForFarTask() {
        Deadline far = Deadline.of(NOW.plus(10, ChronoUnit.DAYS));
        Task task = Task.create("Task hạn xa", "Mô tả", far, Priority.LOW, taskProjectId);

        assertEquals(Priority.LOW, calculator.calculatePriority(task, regularProject, NOW));
    }

    @Test
    @DisplayName("No deadline and no user priority -> LOW")
    void shouldReturnLowWithoutDeadlineOrPriority() {
        assertEquals(Priority.LOW,
            calculator.calculatePriority((Deadline) null, null, regularProject, NOW));
    }

    @Test
    @DisplayName("A null project behaves like a regular project")
    void shouldTreatNullProjectAsRegular() {
        Deadline overdue = Deadline.of(NOW.minus(1, ChronoUnit.DAYS));
        Task task = Task.create("Task", "Mô tả", overdue, Priority.LOW, taskProjectId);

        assertEquals(Priority.HIGH, calculator.calculatePriority(task, null, NOW));
    }

    @Test
    @DisplayName("A null task yields the safest priority")
    void shouldReturnLowForNullTask() {
        assertEquals(Priority.LOW, calculator.calculatePriority((Task) null, regularProject, NOW));
    }

    @Test
    @DisplayName("An already HIGH task is not downgraded by a far deadline")
    void shouldNotDowngradeHighPriority() {
        Deadline far = Deadline.of(NOW.plus(10, ChronoUnit.DAYS));
        Task task = Task.create("Task quan trọng", "Mô tả", far, Priority.HIGH, taskProjectId);

        assertEquals(Priority.HIGH, calculator.calculatePriority(task, regularProject, NOW));
    }
}
