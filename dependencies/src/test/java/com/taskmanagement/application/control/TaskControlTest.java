package com.taskmanagement.application.control;

import com.taskmanagement.adapter.persistence.IOSaveToMemory;
import com.taskmanagement.adapter.persistence.IOSaveToMemoryProject;
import com.taskmanagement.application.boundary.TaskInputBoundary;
import com.taskmanagement.application.dto.TaskInputDTO;
import com.taskmanagement.application.dto.TaskOutputDTO;
import com.taskmanagement.application.exception.ProjectNotFoundException;
import com.taskmanagement.application.exception.TaskNotFoundException;
import com.taskmanagement.domain.common.event.DomainEvent;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.project.repository.ProjectRepository;
import com.taskmanagement.domain.service.TaskPriorityCalculator;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.enums.TaskStatus;
import com.taskmanagement.domain.task.event.TaskCreatedEvent;
import com.taskmanagement.domain.task.event.TaskPriorityChangedEvent;
import com.taskmanagement.domain.task.event.TaskStartedEvent;
import com.taskmanagement.domain.task.exception.TaskValidationException;
import com.taskmanagement.domain.task.repository.TaskRepository;
import com.taskmanagement.infrastructure.event.InMemoryDomainEventDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TaskControl Application Service")
class TaskControlTest {
    private TaskControl control;
    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private InMemoryDomainEventDispatcher dispatcher;
    private List<DomainEvent> capturedEvents;

    @BeforeEach
    void setUp() {
        taskRepository = new IOSaveToMemory();
        projectRepository = new IOSaveToMemoryProject();
        dispatcher = new InMemoryDomainEventDispatcher();
        capturedEvents = new ArrayList<>();
        dispatcher.subscribe(capturedEvents::add);
        control = new TaskControl(
            taskRepository, projectRepository, dispatcher, null,
            new TaskPriorityCalculator(), null);
    }

    private UUID newProject() {
        Project project = Project.create("Dự án " + UUID.randomUUID(), "Mô tả");
        projectRepository.save(project);
        return project.getId().value();
    }

    private TaskInputDTO input(String title, Instant dueDate, Priority priority, UUID projectId) {
        return new TaskInputDTO(title, "Mô tả", dueDate, priority, projectId);
    }

    @Test
    @DisplayName("Create: persists the task and dispatches exactly one TaskCreatedEvent")
    void shouldCreateTaskAndDispatchEventOnce() {
        UUID projectId = newProject();

        TaskOutputDTO output = control.createTask(input(
            "Triển khai quản lý công việc", Instant.now().plus(2, ChronoUnit.DAYS),
            Priority.MEDIUM, projectId));

        assertNotNull(output.taskId());
        assertEquals("Triển khai quản lý công việc", output.title());
        assertEquals(TaskStatus.TODO, output.status());
        assertEquals(projectId, output.projectId());
        assertEquals(1, control.getTasks().size());
        assertEquals(1, capturedEvents.size());
        assertInstanceOf(TaskCreatedEvent.class, capturedEvents.get(0));
    }

    @Test
    @DisplayName("HIGH priority with a far deadline succeeds")
    void shouldCreateHighPriorityTaskWithFarDeadline() {
        UUID projectId = newProject();

        TaskOutputDTO output = control.createTask(input(
            "Task ưu tiên cao", Instant.now().plus(10, ChronoUnit.DAYS),
            Priority.HIGH, projectId));

        assertEquals(Priority.HIGH, output.priority());
    }

    @Test
    @DisplayName("An overdue HIGH priority task succeeds")
    void shouldCreateOverdueHighPriorityTask() {
        UUID projectId = newProject();

        TaskOutputDTO output = control.createTask(input(
            "Task quá hạn", Instant.now().minus(1, ChronoUnit.DAYS),
            Priority.HIGH, projectId));

        assertEquals(Priority.HIGH, output.priority());
        assertTrue(output.isOverdue());
    }

    @Test
    @DisplayName("Project resolution: a null project id falls back to the persisted Inbox")
    void shouldUseInboxWhenProjectIsNull() {
        UUID inboxId = projectRepository.getInbox().getId().value();

        TaskOutputDTO output = control.createTask(
            input("Task chưa phân loại", null, Priority.LOW, null));

        assertEquals(inboxId, output.projectId());
    }

    @Test
    @DisplayName("An unknown project id is rejected")
    void shouldRejectUnknownProject() {
        assertThrows(ProjectNotFoundException.class, () -> control.createTask(
            input("Task mồ côi", null, Priority.LOW, UUID.randomUUID())));
    }

    @Test
    @DisplayName("Invariant: duplicate title in the same project is rejected")
    void shouldRejectDuplicateTitleInSameProject() {
        UUID projectId = newProject();
        control.createTask(input("Họp báo cáo đồ án", null, Priority.LOW, projectId));

        assertThrows(TaskValidationException.class, () -> control.createTask(
            input("họp BÁO CÁO đồ án", null, Priority.MEDIUM, projectId)));
    }

    @Test
    @DisplayName("Invariant: the same title in a different project is allowed")
    void shouldAllowSameTitleInDifferentProject() {
        control.createTask(input("Họp nhóm", null, Priority.LOW, newProject()));
        control.createTask(input("Họp nhóm", null, Priority.LOW, newProject()));

        assertEquals(2, control.getTasks().size());
    }

    @Test
    @DisplayName("Auto priority: null priority is calculated from the deadline")
    void shouldCalculatePriorityWhenInputIsNull() {
        UUID projectId = newProject();

        TaskOutputDTO output = control.createTask(
            input("Task sát hạn", Instant.now().plus(1, ChronoUnit.DAYS), null, projectId));

        assertEquals(Priority.HIGH, output.priority());
    }

    @Test
    @DisplayName("Auto priority: INBOX caps the calculated priority at MEDIUM")
    void shouldCapInboxAutoPriority() {
        UUID inboxId = projectRepository.getInbox().getId().value();

        TaskOutputDTO output = control.createTask(
            input("Ghi chú vãng lai", Instant.now().plus(1, ChronoUnit.DAYS), null, inboxId));

        assertEquals(Priority.MEDIUM, output.priority());
    }

    @Test
    @DisplayName("Workflow: start then complete")
    void shouldExecuteLifecycle() {
        TaskOutputDTO created = control.createTask(
            input("Viết tài liệu kỹ thuật", Instant.now().plus(2, ChronoUnit.DAYS),
                Priority.MEDIUM, newProject()));

        TaskOutputDTO started = control.startTask(created.taskId());
        assertEquals(TaskStatus.IN_PROGRESS, started.status());

        TaskOutputDTO completed = control.completeTask(created.taskId());
        assertEquals(TaskStatus.DONE, completed.status());
    }

    @Test
    @DisplayName("Refresh: recalculates priority and dispatches a change event")
    void shouldRefreshPriority() {
        UUID projectId = newProject();
        TaskOutputDTO created = control.createTask(input(
            "Fix bug critical", Instant.now().plus(1, ChronoUnit.DAYS),
            Priority.LOW, projectId));
        assertEquals(Priority.LOW, created.priority());

        capturedEvents.clear();
        TaskOutputDTO refreshed = control.refreshTaskPriority(created.taskId());

        assertEquals(Priority.HIGH, refreshed.priority());
        assertInstanceOf(TaskPriorityChangedEvent.class, capturedEvents.get(0));
    }

    @Test
    @DisplayName("Lifecycle: starting a missing task fails")
    void shouldFailForMissingTask() {
        assertThrows(TaskNotFoundException.class,
            () -> control.startTask(UUID.randomUUID()));
    }

    @Test
    @DisplayName("Reschedule: updates the deadline and persists it")
    void shouldRescheduleTask() {
        TaskOutputDTO created = control.createTask(
            input("Task cần gia hạn", Instant.now().plus(1, ChronoUnit.DAYS),
                Priority.LOW, newProject()));
        Instant newDeadline = Instant.now().plus(5, ChronoUnit.DAYS);

        TaskOutputDTO updated = control.rescheduleTask(created.taskId(), newDeadline);

        assertEquals(newDeadline, updated.dueDate());
        assertEquals(newDeadline, control.getTask(created.taskId()).dueDate());
    }

    @Test
    @DisplayName("Events: each state change dispatches only the new event")
    void shouldDispatchOnlyNewEvents() {
        TaskOutputDTO created = control.createTask(
            input("Task", null, Priority.LOW, newProject()));
        assertEquals(1, capturedEvents.size());

        control.startTask(created.taskId());
        assertEquals(2, capturedEvents.size());
        assertInstanceOf(TaskStartedEvent.class, capturedEvents.get(1));
    }

    @Test
    @DisplayName("Filtering: tasks can be listed by project")
    void shouldListTasksByProject() {
        UUID first = newProject();
        UUID second = newProject();
        control.createTask(input("Task A", null, Priority.LOW, first));
        control.createTask(input("Task B", null, Priority.LOW, second));

        List<TaskOutputDTO> tasks = control.getTasksByProject(first);

        assertEquals(1, tasks.size());
        assertEquals(first, tasks.get(0).projectId());
    }

    @Test
    @DisplayName("DIP: null collaborators are rejected eagerly")
    void shouldRejectNullCollaborators() {
        assertThrows(NullPointerException.class,
            () -> new TaskControl(null, projectRepository, dispatcher));
        assertThrows(NullPointerException.class,
            () -> new TaskControl(taskRepository, null, dispatcher));
        assertThrows(NullPointerException.class,
            () -> new TaskControl(taskRepository, projectRepository, null));
    }

    @Test
    @DisplayName("DIP: TaskControl implements the TaskInputBoundary port")
    void shouldImplementTaskInputBoundary() {
        assertInstanceOf(TaskInputBoundary.class, control);
    }
}
