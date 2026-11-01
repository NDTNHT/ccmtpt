package com.taskmanagement.application.control;

import com.taskmanagement.application.boundary.TaskInputBoundary;
import com.taskmanagement.application.boundary.TaskShowing;
import com.taskmanagement.application.dto.SubTaskOutputDTO;
import com.taskmanagement.application.dto.TaskInputDTO;
import com.taskmanagement.application.dto.TaskOutputDTO;
import com.taskmanagement.application.exception.ProjectNotFoundException;
import com.taskmanagement.application.exception.TaskNotFoundException;
import com.taskmanagement.application.port.DomainEventDispatcher;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.project.repository.ProjectRepository;
import com.taskmanagement.domain.project.valueobject.ProjectId;
import com.taskmanagement.domain.service.TaskPriorityCalculator;
import com.taskmanagement.domain.task.entity.SubTask;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.exception.TaskValidationException;
import com.taskmanagement.domain.task.repository.TaskRepository;
import com.taskmanagement.domain.task.service.TaskTitleUniquenessChecker;
import com.taskmanagement.domain.task.valueobject.Deadline;
import com.taskmanagement.domain.task.valueobject.TaskId;
import com.taskmanagement.domain.task.valueobject.TaskTitle;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Task application service.
 *
 * <p>Coordinates task use cases: validation, project resolution, priority calculation,
 * persistence, domain event dispatch and mapping to the read model.</p>
 */
public class TaskControl implements TaskInputBoundary {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final DomainEventDispatcher eventDispatcher;
    private final TaskShowing taskShowing;
    private final TaskPriorityCalculator priorityCalculator;
    private final TaskTitleUniquenessChecker titleUniquenessChecker;

    public TaskControl(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       DomainEventDispatcher eventDispatcher) {
        this(taskRepository, projectRepository, eventDispatcher, null);
    }

    public TaskControl(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       DomainEventDispatcher eventDispatcher,
                       TaskShowing taskShowing) {
        this(taskRepository, projectRepository, eventDispatcher, taskShowing,
            new TaskPriorityCalculator(), null);
    }

    public TaskControl(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       DomainEventDispatcher eventDispatcher,
                       TaskShowing taskShowing,
                       TaskPriorityCalculator priorityCalculator,
                       TaskTitleUniquenessChecker titleUniquenessChecker) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "TaskRepository cannot be null");
        this.projectRepository = Objects.requireNonNull(projectRepository, "ProjectRepository cannot be null");
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher, "DomainEventDispatcher cannot be null");
        this.taskShowing = taskShowing;
        this.priorityCalculator = priorityCalculator != null ? priorityCalculator : new TaskPriorityCalculator();
        this.titleUniquenessChecker = titleUniquenessChecker != null
            ? titleUniquenessChecker
            : (projectId, title) -> !taskRepository.existsByProjectIdAndTitle(projectId, title);
    }

    // Task use cases

    @Override
    public TaskOutputDTO createTask(TaskInputDTO input) {
        return createTask(input, List.of());
    }

    @Override
    public TaskOutputDTO createTask(TaskInputDTO input, List<String> subTasks) {
        Objects.requireNonNull(input, "Task input cannot be null");

        TaskTitle title = new TaskTitle(input.title());
        Project project = resolveProject(input.projectId());

        if (!titleUniquenessChecker.isUnique(project.getId(), title)) {
            throw new TaskValidationException(
                "A task with title '" + title.value() + "' already exists in this project.");
        }

        Deadline deadline = input.dueDate() != null ? Deadline.of(input.dueDate()) : null;
        Priority priority = input.priority() != null
            ? input.priority()
            : priorityCalculator.calculatePriority(deadline, null, project);

        Task task = Task.create(title, input.description(), deadline, priority, project.getId());
        if (subTasks != null) subTasks.forEach(task::addSubTask);
        saveAndDispatch(task);
        return present(task);
    }

    @Override
    public TaskOutputDTO setSubTaskCompleted(UUID taskId, UUID subTaskId, boolean completed) {
        return mutate(taskId, task -> task.setSubTaskCompleted(subTaskId, completed));
    }

    @Override
    public TaskOutputDTO startTask(UUID taskId) {
        return mutate(taskId, Task::start);
    }

    @Override
    public TaskOutputDTO completeTask(UUID taskId) {
        return mutate(taskId, Task::complete);
    }

    @Override
    public TaskOutputDTO cancelTask(UUID taskId) {
        return mutate(taskId, Task::cancel);
    }

    @Override
    public TaskOutputDTO refreshTaskPriority(UUID taskId) {
        return mutate(taskId, task -> {
            Project project = findProject(task.getProjectId().value(),
                "Task " + taskId + " references a project that no longer exists: " + task.getProjectId());
            Priority recalculated = priorityCalculator.calculatePriority(task, project);
            task.changePriority(recalculated);
        });
    }

    @Override
    public TaskOutputDTO rescheduleTask(UUID taskId, Instant newDueDate) {
        if (newDueDate == null) {
            throw new TaskValidationException("New deadline cannot be null.");
        }
        return mutate(taskId, task -> task.reschedule(Deadline.of(newDueDate)));
    }

    @Override
    public TaskOutputDTO getTask(UUID taskId) {
        return present(getTaskOrThrow(TaskId.of(taskId)));
    }

    @Override
    public List<TaskOutputDTO> getTasks() {
        return taskRepository.findAll().stream()
            .map(this::present)
            .toList();
    }

    @Override
    public List<TaskOutputDTO> getTasksByProject(UUID projectId) {
        Objects.requireNonNull(projectId, "Project id cannot be null");
        return taskRepository.findByProjectId(ProjectId.of(projectId)).stream()
            .map(this::present)
            .toList();
    }

    // Internal helpers

    private TaskOutputDTO mutate(UUID taskId, Consumer<Task> action) {
        Task task = getTaskOrThrow(TaskId.of(taskId));
        action.accept(task);
        saveAndDispatch(task);
        return present(task);
    }

    private void saveAndDispatch(Task task) {
        taskRepository.save(task);
        eventDispatcher.dispatch(task.pollDomainEvents());
    }

    private Task getTaskOrThrow(TaskId taskId) {
        return taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId));
    }

    /**
     * A null project id means "put it in the Inbox". An explicit but unknown project id is
     * an error: silently substituting the Inbox would persist a task under a non-existent
     * project id and corrupt the model.
     */
    private Project resolveProject(UUID projectId) {
        if (projectId == null) {
            return projectRepository.getInbox();
        }
        return findProject(projectId, "Project not found with ID: " + projectId);
    }

    private Project findProject(UUID projectId, String errorMessage) {
        return projectRepository.findById(ProjectId.of(projectId))
            .orElseThrow(() -> new ProjectNotFoundException(errorMessage));
    }

    private TaskOutputDTO present(Task task) {
        TaskOutputDTO output = mapToOutputDTO(task);
        if (taskShowing != null) {
            taskShowing.showResult(output);
        }
        return output;
    }

    private TaskOutputDTO mapToOutputDTO(Task task) {
        List<SubTaskOutputDTO> subTasks = task.getSubTasks().stream()
            .map(this::toSubTaskOutput)
            .toList();
        return new TaskOutputDTO(
            task.getId().value(),
            task.getTitle().value(),
            task.getDescription(),
            task.getStatus(),
            task.getPriority(),
            task.getDueDate() != null ? task.getDueDate().dueDate() : null,
            task.isOverdue(),
            task.getProjectId().value(),
            subTasks);
    }

    private SubTaskOutputDTO toSubTaskOutput(SubTask subTask) {
        return new SubTaskOutputDTO(subTask.getId(), subTask.getTitle(), subTask.isCompleted());
    }
}
