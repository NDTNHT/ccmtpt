package com.taskmanagement.domain.task.entity;

import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.enums.TaskStatus;
import com.taskmanagement.domain.task.event.TaskCancelledEvent;
import com.taskmanagement.domain.task.event.TaskCompletedEvent;
import com.taskmanagement.domain.task.event.TaskCreatedEvent;
import com.taskmanagement.domain.task.event.TaskPriorityChangedEvent;
import com.taskmanagement.domain.task.event.TaskRescheduledEvent;
import com.taskmanagement.domain.task.event.TaskStartedEvent;
import com.taskmanagement.domain.task.exception.InvalidTaskStateTransitionException;
import com.taskmanagement.domain.task.exception.TaskValidationException;
import com.taskmanagement.domain.task.valueobject.Deadline;
import com.taskmanagement.domain.task.valueobject.TaskId;
import com.taskmanagement.domain.task.valueobject.TaskTitle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Task Aggregate Root")
class TaskTest {
    private final UUID projectId = UUID.randomUUID();

    @Test
    @DisplayName("Factory: creates a TODO task and emits TaskCreatedEvent")
    void shouldCreateTaskWithFactory() {
        Task task = Task.create(
            "Báo cáo môn học", "Hoàn thành báo cáo", null, Priority.MEDIUM, projectId);

        assertNotNull(task.getId());
        assertEquals("Báo cáo môn học", task.getTitle().value());
        assertEquals(TaskStatus.TODO, task.getStatus());
        assertEquals(Priority.MEDIUM, task.getPriority());
        assertEquals(projectId, task.getProjectId().value());
        assertEquals(1, task.getDomainEvents().size());
        assertInstanceOf(TaskCreatedEvent.class, task.getDomainEvents().get(0));
    }

    @Test
    @DisplayName("Factory: null priority defaults to MEDIUM")
    void shouldDefaultNullPriorityToMedium() {
        Task task = Task.create("Task mặc định", null, null, null, projectId);
        assertEquals(Priority.MEDIUM, task.getPriority());
    }

    @Test
    @DisplayName("Factory: a task must belong to a project")
    void shouldRejectTaskWithoutProject() {
        assertThrows(TaskValidationException.class,
            () -> Task.create("Task mồ côi", "Mô tả", null, Priority.LOW, (UUID) null));
    }

    @Test
    @DisplayName("Factory: HIGH priority is allowed with a far deadline")
    void shouldAllowHighPriorityWithFarDeadline() {
        Task task = Task.create(
            "Task ưu tiên cao", "Không nhất thiết sát hạn",
            Deadline.inDays(10), Priority.HIGH, projectId);

        assertEquals(Priority.HIGH, task.getPriority());
        assertEquals(TaskStatus.TODO, task.getStatus());
    }

    @Test
    @DisplayName("Urgent factory: accepts an already overdue deadline")
    void shouldCreateUrgentTaskForOverdueDeadline() {
        Deadline overdue = Deadline.of(Instant.now().minus(1, ChronoUnit.DAYS));

        Task task = Task.createUrgentTask("Task quá hạn", "Mô tả", overdue, projectId);

        assertEquals(Priority.HIGH, task.getPriority());
    }

    @Test
    @DisplayName("Urgent factory: accepts a deadline within two days")
    void shouldCreateUrgentTaskWithinWindow() {
        Task task = Task.createUrgentTask(
            "Task khẩn cấp", "Mô tả", Deadline.inDays(1), projectId);
        assertEquals(Priority.HIGH, task.getPriority());
    }

    @Test
    @DisplayName("Urgent factory: rejects a deadline beyond two days")
    void shouldRejectUrgentTaskWithFarDeadline() {
        assertThrows(TaskValidationException.class,
            () -> Task.createUrgentTask("Task không gấp", "Mô tả", Deadline.inDays(5), projectId));
    }

    @Test
    @DisplayName("Urgent factory: rejects a missing deadline")
    void shouldRejectUrgentTaskWithoutDeadline() {
        assertThrows(TaskValidationException.class,
            () -> Task.createUrgentTask("Task không hạn", "Mô tả", null, projectId));
    }

    @Test
    @DisplayName("Reconstitution: restores state without emitting TaskCreatedEvent")
    void shouldReconstituteWithoutEvents() {
        UUID id = UUID.randomUUID();
        Task reconstituted = Task.reconstitute(
            id,
            new TaskTitle("Task đã hoàn thành"),
            "Nạp từ SQLite",
            Deadline.of(Instant.now().minus(2, ChronoUnit.DAYS)),
            Priority.HIGH,
            TaskStatus.DONE,
            projectId);

        assertEquals(id, reconstituted.getId().value());
        assertEquals(TaskStatus.DONE, reconstituted.getStatus());
        assertTrue(reconstituted.getDomainEvents().isEmpty());
    }

    @Test
    @DisplayName("Reconstitution: restores child subtasks")
    void shouldReconstituteSubTasks() {
        UUID subTaskId = UUID.randomUUID();
        SubTask persisted = SubTask.reconstitute(subTaskId, "Viết chương 1", true);

        Task task = Task.reconstitute(
            TaskId.generate(),
            new TaskTitle("Báo cáo"),
            "Mô tả",
            null,
            Priority.LOW,
            TaskStatus.IN_PROGRESS,
            com.taskmanagement.domain.project.valueobject.ProjectId.of(projectId),
            List.of(persisted));

        assertEquals(1, task.getSubTasks().size());
        assertEquals(subTaskId, task.getSubTasks().get(0).getId());
        assertTrue(task.getSubTasks().get(0).isCompleted());
    }

    @Test
    @DisplayName("State machine: TODO -> IN_PROGRESS -> DONE with matching events")
    void shouldTransitionTodoToDone() {
        Task task = Task.create("Lập trình Java", "Mô tả", null, Priority.LOW, projectId);
        task.clearDomainEvents();

        task.start();
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        assertEquals(1, task.getDomainEvents().size());
        assertInstanceOf(TaskStartedEvent.class, task.getDomainEvents().get(0));

        task.complete();
        assertEquals(TaskStatus.DONE, task.getStatus());
        assertEquals(2, task.getDomainEvents().size());
        assertInstanceOf(TaskCompletedEvent.class, task.getDomainEvents().get(1));
    }

    @Test
    @DisplayName("State machine: cannot complete directly from TODO")
    void shouldRejectCompleteFromTodo() {
        Task task = Task.create("Lập trình Java", "Mô tả", null, Priority.LOW, projectId);
        assertThrows(InvalidTaskStateTransitionException.class, task::complete);
    }

    @Test
    @DisplayName("State machine: cancel emits TaskCancelledEvent")
    void shouldCancelTask() {
        Task task = Task.create("Nhiệm vụ bị hủy", "Mô tả", null, Priority.LOW, projectId);
        task.clearDomainEvents();

        task.cancel();
        assertEquals(TaskStatus.CANCELLED, task.getStatus());
        assertInstanceOf(TaskCancelledEvent.class, task.getDomainEvents().get(0));
    }

    @Test
    @DisplayName("State machine: cancelling twice is rejected")
    void shouldRejectCancellingTwice() {
        Task task = Task.create("Nhiệm vụ", "Mô tả", null, Priority.LOW, projectId);
        task.cancel();
        assertThrows(InvalidTaskStateTransitionException.class, task::cancel);
    }

    @Test
    @DisplayName("State machine: completed tasks are immutable")
    void shouldRejectMutationsOnCompletedTask() {
        Task task = Task.create("Task hoàn thành", "Mô tả", Deadline.inDays(1), Priority.MEDIUM, projectId);
        task.start();
        task.complete();

        assertThrows(InvalidTaskStateTransitionException.class,
            () -> task.changePriority(Priority.HIGH));
        assertThrows(InvalidTaskStateTransitionException.class,
            () -> task.updateDetails(new TaskTitle("Tên mới"), "Mô tả mới"));
        assertThrows(InvalidTaskStateTransitionException.class,
            () -> task.reschedule(Deadline.inDays(5)));
        assertThrows(InvalidTaskStateTransitionException.class,
            () -> task.addSubTask("Không được thêm"));
    }

    @Test
    @DisplayName("Reschedule: changes deadline and emits TaskRescheduledEvent")
    void shouldRescheduleTask() {
        Task task = Task.create("Cần gia hạn", "Mô tả", Deadline.inDays(1), Priority.MEDIUM, projectId);
        task.clearDomainEvents();
        Deadline newDeadline = Deadline.inDays(4);

        task.reschedule(newDeadline);

        assertEquals(newDeadline, task.getDueDate());
        TaskRescheduledEvent event = assertInstanceOf(
            TaskRescheduledEvent.class, task.getDomainEvents().get(0));
        assertEquals(newDeadline, event.newDeadline());
    }

    @Test
    @DisplayName("Reschedule: clearing a deadline emits an event with a null new deadline")
    void shouldClearDeadline() {
        Task task = Task.create("Có hạn", "Mô tả", Deadline.inDays(1), Priority.MEDIUM, projectId);
        task.clearDomainEvents();

        task.clearDeadline();

        assertEquals(null, task.getDueDate());
        TaskRescheduledEvent event = assertInstanceOf(
            TaskRescheduledEvent.class, task.getDomainEvents().get(0));
        assertEquals(null, event.newDeadline());
    }

    @Test
    @DisplayName("Priority: change is recorded once and identical changes are no-ops")
    void shouldChangePriorityWithEvent() {
        Task task = Task.create("Task", "Mô tả", null, Priority.LOW, projectId);
        task.clearDomainEvents();

        task.changePriority(Priority.HIGH);
        assertEquals(Priority.HIGH, task.getPriority());
        TaskPriorityChangedEvent event = assertInstanceOf(
            TaskPriorityChangedEvent.class, task.getDomainEvents().get(0));
        assertEquals(Priority.LOW, event.previousPriority());
        assertEquals(Priority.HIGH, event.newPriority());

        task.clearDomainEvents();
        task.changePriority(Priority.HIGH);
        assertTrue(task.getDomainEvents().isEmpty());
    }

    @Test
    @DisplayName("Details: title and description can be updated while in progress")
    void shouldUpdateDetails() {
        Task task = Task.create("Tên cũ", "Mô tả cũ", null, Priority.LOW, projectId);
        task.start();

        task.updateDetails(new TaskTitle("Tên mới"), "Mô tả mới");

        assertEquals("Tên mới", task.getTitle().value());
        assertEquals("Mô tả mới", task.getDescription());
    }

    @Test
    @DisplayName("SubTask: managed only through the aggregate root")
    void shouldManageSubTasks() {
        Task task = Task.create("Viết báo cáo lớn", "Mô tả", null, Priority.HIGH, projectId);
        assertTrue(task.getSubTasks().isEmpty());

        SubTask subTask = task.addSubTask("Viết phần 1: Giới thiệu");
        assertNotNull(subTask.getId());
        assertEquals(1, task.getSubTasks().size());

        task.completeSubTask(subTask.getId());
        assertTrue(task.getSubTasks().get(0).isCompleted());
        assertTrue(task.areAllSubTasksCompleted());
    }

    @Test
    @DisplayName("SubTask: duplicate titles are rejected")
    void shouldRejectDuplicateSubTaskTitles() {
        Task task = Task.create("Viết báo cáo", "Mô tả", null, Priority.HIGH, projectId);
        task.addSubTask("Phần 1");

        assertThrows(TaskValidationException.class, () -> task.addSubTask("  phần 1  "));
    }

    @Test
    @DisplayName("SubTask: completing an unknown id fails")
    void shouldRejectUnknownSubTask() {
        Task task = Task.create("Viết báo cáo", "Mô tả", null, Priority.HIGH, projectId);

        assertThrows(TaskValidationException.class, () -> task.completeSubTask(UUID.randomUUID()));
        assertThrows(TaskValidationException.class, () -> task.removeSubTask(UUID.randomUUID()));
    }

    @Test
    @DisplayName("Value Object: TaskTitle trims, validates length and compares case-insensitively")
    void shouldValidateTaskTitle() {
        assertThrows(TaskValidationException.class, () -> new TaskTitle("ab"));
        assertThrows(TaskValidationException.class, () -> new TaskTitle("   "));

        TaskTitle title = new TaskTitle("  Viết báo cáo  ");
        assertEquals("Viết báo cáo", title.value());
        assertTrue(title.sameValueAs(new TaskTitle("viết BÁO CÁO")));
        assertFalse(title.sameValueAs(null));
    }

    @Test
    @DisplayName("Overdue: uses the supplied clock for deterministic checks")
    void shouldCheckOverdueWithExplicitClock() {
        Instant now = Instant.parse("2026-01-10T00:00:00Z");
        Deadline deadline = Deadline.of(now.minus(Duration.ofHours(1)));
        Task task = Task.create("Task", "Mô tả", deadline, Priority.LOW, projectId);

        assertTrue(task.isOverdue(now));
        assertFalse(task.isOverdue(now.minus(Duration.ofDays(1))));
    }

    @Test
    @DisplayName("Identity: equality is based on the TaskId")
    void shouldUseIdentityEquality() {
        TaskId id = TaskId.generate();
        Task first = Task.reconstitute(
            id, new TaskTitle("Một"), null, null, Priority.LOW, TaskStatus.TODO,
            com.taskmanagement.domain.project.valueobject.ProjectId.of(projectId));
        Task second = Task.reconstitute(
            id, new TaskTitle("Hai"), "Khác", null, Priority.HIGH, TaskStatus.DONE,
            com.taskmanagement.domain.project.valueobject.ProjectId.of(projectId));

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertSame(first, first);
    }
}
