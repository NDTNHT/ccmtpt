package com.taskmanagement.domain.task.entity;

import com.taskmanagement.domain.common.entity.AggregateRoot;
import com.taskmanagement.domain.project.valueobject.ProjectId;
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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Task aggregate root. Encapsulates lifecycle transitions, subtasks and domain events.
 * References its project by {@link ProjectId}.
 */
public class Task extends AggregateRoot<TaskId> {
    private static final Duration URGENT_WINDOW = Duration.ofDays(2);
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private TaskTitle title;
    private String description;
    private Deadline dueDate;
    private Priority priority;
    private TaskStatus status;
    private final ProjectId projectId;
    private final List<SubTask> subTasks;

    private Task(TaskId id,
                 TaskTitle title,
                 String description,
                 Deadline dueDate,
                 Priority priority,
                 TaskStatus status,
                 ProjectId projectId,
                 List<SubTask> subTasks) {
        super(Objects.requireNonNull(id, "Task id cannot be null"));
        this.title = Objects.requireNonNull(title, "Task title cannot be null");
        this.description = validateDescription(description);
        this.dueDate = dueDate;
        this.priority = Objects.requireNonNull(priority, "Priority cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.projectId = Objects.requireNonNull(projectId, "ProjectId cannot be null");
        this.subTasks = subTasks == null ? new ArrayList<>() : new ArrayList<>(subTasks);
        ensureUniqueSubTaskTitles(this.subTasks);
    }

    // Factories

    /**
     * Creates a new task in TODO state. A task always belongs to a project; the
     * application layer resolves a missing project to the Inbox before calling this.
     */
    public static Task create(TaskTitle title,
                              String description,
                              Deadline dueDate,
                              Priority priority,
                              ProjectId projectId) {
        if (projectId == null) {
            throw new TaskValidationException("A task must belong to a project.");
        }
        Priority effectivePriority = priority != null ? priority : Priority.MEDIUM;
        Task task = new Task(
            TaskId.generate(),
            title,
            description,
            dueDate,
            effectivePriority,
            TaskStatus.TODO,
            projectId,
            List.of());
        task.registerEvent(new TaskCreatedEvent(task.getId(), task.getTitle(), task.getProjectId(), Instant.now()));
        return task;
    }

    public static Task create(String title,
                              String description,
                              Deadline dueDate,
                              Priority priority,
                              UUID projectId) {
        if (projectId == null) {
            throw new TaskValidationException("A task must belong to a project.");
        }
        return create(new TaskTitle(title), description, dueDate, priority, ProjectId.of(projectId));
    }

    public static Task create(String title,
                              String description,
                              Deadline dueDate,
                              Priority priority,
                              ProjectId projectId) {
        return create(new TaskTitle(title), description, dueDate, priority, projectId);
    }

    /**
     * Creates an urgent task: it must already be overdue or due within two days.
     * Urgency is a domain rule; priority is only one of its consequences.
     */
    public static Task createUrgentTask(TaskTitle title,
                                        String description,
                                        Deadline dueDate,
                                        ProjectId projectId) {
        if (dueDate == null) {
            throw new TaskValidationException("An urgent task must have a deadline.");
        }
        if (!dueDate.isOverdue() && !dueDate.isApproaching(URGENT_WINDOW)) {
            throw new TaskValidationException("An urgent task must be overdue or due within two days.");
        }
        if (projectId == null) {
            throw new TaskValidationException("A task must belong to a project.");
        }
        Task task = new Task(
            TaskId.generate(),
            title,
            description,
            dueDate,
            Priority.HIGH,
            TaskStatus.TODO,
            projectId,
            List.of());
        task.registerEvent(new TaskCreatedEvent(task.getId(), task.getTitle(), task.getProjectId(), Instant.now()));
        return task;
    }

    public static Task createUrgentTask(String title,
                                        String description,
                                        Deadline dueDate,
                                        UUID projectId) {
        if (projectId == null) {
            throw new TaskValidationException("A task must belong to a project.");
        }
        return createUrgentTask(new TaskTitle(title), description, dueDate, ProjectId.of(projectId));
    }

    public static Task createUrgentTask(String title,
                                        String description,
                                        Deadline dueDate,
                                        ProjectId projectId) {
        return createUrgentTask(new TaskTitle(title), description, dueDate, projectId);
    }

    /**
     * Rehydration factory: restores persisted state without re-running creation rules
     * and without emitting a {@link TaskCreatedEvent}.
     */
    public static Task reconstitute(TaskId id,
                                    TaskTitle title,
                                    String description,
                                    Deadline dueDate,
                                    Priority priority,
                                    TaskStatus status,
                                    ProjectId projectId,
                                    List<SubTask> subTasks) {
        return new Task(id, title, description, dueDate, priority, status, projectId, subTasks);
    }

    public static Task reconstitute(TaskId id,
                                    TaskTitle title,
                                    String description,
                                    Deadline dueDate,
                                    Priority priority,
                                    TaskStatus status,
                                    ProjectId projectId) {
        return reconstitute(id, title, description, dueDate, priority, status, projectId, List.of());
    }

    public static Task reconstitute(UUID id,
                                    TaskTitle title,
                                    String description,
                                    Deadline dueDate,
                                    Priority priority,
                                    TaskStatus status,
                                    UUID projectId) {
        if (projectId == null) {
            throw new TaskValidationException("A persisted task must reference a project.");
        }
        return reconstitute(
            TaskId.of(id), title, description, dueDate, priority, status, ProjectId.of(projectId), List.of());
    }

    // State transitions

    public void start() {
        if (status != TaskStatus.TODO) {
            throw new InvalidTaskStateTransitionException(
                "Cannot start task from status " + status + ". Only TODO tasks can be started.");
        }
        status = TaskStatus.IN_PROGRESS;
        registerEvent(new TaskStartedEvent(id, Instant.now()));
    }

    public void complete() {
        if (status != TaskStatus.IN_PROGRESS) {
            throw new InvalidTaskStateTransitionException(
                "Cannot complete task from status " + status + ". Only IN_PROGRESS tasks can be completed.");
        }
        status = TaskStatus.DONE;
        registerEvent(new TaskCompletedEvent(id, Instant.now()));
    }

    public void cancel() {
        if (status == TaskStatus.DONE) {
            throw new InvalidTaskStateTransitionException("Cannot cancel a completed task.");
        }
        if (status == TaskStatus.CANCELLED) {
            throw new InvalidTaskStateTransitionException("Task is already cancelled.");
        }
        status = TaskStatus.CANCELLED;
        registerEvent(new TaskCancelledEvent(id, Instant.now()));
    }

    public void reschedule(Deadline newDeadline) {
        ensureMutable("reschedule");
        if (newDeadline == null) {
            throw new TaskValidationException("New deadline cannot be null.");
        }
        if (newDeadline.equals(dueDate)) {
            return;
        }
        Deadline previous = dueDate;
        dueDate = newDeadline;
        registerEvent(new TaskRescheduledEvent(id, previous, newDeadline, Instant.now()));
    }

    public void clearDeadline() {
        ensureMutable("clear the deadline of");
        if (dueDate == null) {
            return;
        }
        Deadline previous = dueDate;
        dueDate = null;
        registerEvent(new TaskRescheduledEvent(id, previous, null, Instant.now()));
    }

    public void updateDetails(TaskTitle newTitle, String newDescription) {
        ensureMutable("update");
        this.title = Objects.requireNonNull(newTitle, "Task title cannot be null");
        this.description = validateDescription(newDescription);
    }

    public void changePriority(Priority newPriority) {
        ensureMutable("change the priority of");
        Objects.requireNonNull(newPriority, "Priority cannot be null");
        if (priority == newPriority) {
            return;
        }
        Priority previous = priority;
        priority = newPriority;
        registerEvent(new TaskPriorityChangedEvent(id, previous, newPriority, Instant.now()));
    }

    public SubTask addSubTask(String title) {
        ensureMutable("add a subtask to");
        String candidate = title == null ? "" : title.trim();
        boolean duplicated = subTasks.stream()
            .anyMatch(subTask -> subTask.getTitle().equalsIgnoreCase(candidate));
        if (duplicated) {
            throw new TaskValidationException("A subtask with title '" + candidate + "' already exists.");
        }
        SubTask subTask = SubTask.create(title);
        subTasks.add(subTask);
        return subTask;
    }

    public void completeSubTask(UUID subTaskId) {
        ensureMutable("complete a subtask of");
        SubTask subTask = findSubTask(subTaskId)
            .orElseThrow(() -> new TaskValidationException("SubTask not found: " + subTaskId));
        subTask.complete();
    }

    public void removeSubTask(UUID subTaskId) {
        ensureMutable("remove a subtask from");
        SubTask subTask = findSubTask(subTaskId)
            .orElseThrow(() -> new TaskValidationException("SubTask not found: " + subTaskId));
        subTasks.remove(subTask);
    }

    public boolean areAllSubTasksCompleted() {
        return subTasks.stream().allMatch(SubTask::isCompleted);
    }

    public boolean isOverdue() {
        return isOverdue(Instant.now());
    }

    public boolean isOverdue(Instant currentInstant) {
        return dueDate != null && dueDate.isOverdue(currentInstant);
    }

    private Optional<SubTask> findSubTask(UUID subTaskId) {
        if (subTaskId == null) {
            return Optional.empty();
        }
        return subTasks.stream()
            .filter(subTask -> subTask.getId().equals(subTaskId))
            .findFirst();
    }

    private void ensureMutable(String action) {
        if (status == TaskStatus.DONE || status == TaskStatus.CANCELLED) {
            throw new InvalidTaskStateTransitionException(
                "Cannot " + action + " a " + status.name().toLowerCase() + " task.");
        }
    }

    private static String validateDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new TaskValidationException(
                "Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters.");
        }
        return description;
    }

    private static void ensureUniqueSubTaskTitles(List<SubTask> subTasks) {
        for (int index = 0; index < subTasks.size(); index++) {
            for (int other = index + 1; other < subTasks.size(); other++) {
                if (subTasks.get(index).getTitle()
                    .equalsIgnoreCase(subTasks.get(other).getTitle())) {
                    throw new TaskValidationException(
                        "Duplicate subtask title: '" + subTasks.get(index).getTitle() + "'.");
                }
            }
        }
    }

    // Accessors

    public TaskTitle getTitle() { return title; }
    public String getDescription() { return description; }
    public Deadline getDueDate() { return dueDate; }
    public Priority getPriority() { return priority; }
    public TaskStatus getStatus() { return status; }
    public ProjectId getProjectId() { return projectId; }
    public List<SubTask> getSubTasks() { return List.copyOf(subTasks); }
}
