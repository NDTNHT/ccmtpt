package com.taskmanagement.adapter.persistence;

import com.taskmanagement.domain.project.valueobject.ProjectId;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.repository.TaskRepository;
import com.taskmanagement.domain.task.valueobject.TaskId;
import com.taskmanagement.domain.task.valueobject.TaskTitle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory task repository, intended for local runs and tests.
 */
public class IOSaveToMemory implements TaskRepository {
    private final Map<TaskId, Task> taskDb = new LinkedHashMap<>();

    @Override
    public synchronized void save(Task task) {
        Objects.requireNonNull(task, "Task cannot be null");
        taskDb.put(task.getId(), task);
    }

    @Override
    public synchronized Optional<Task> findById(TaskId id) {
        return Optional.ofNullable(taskDb.get(id));
    }

    @Override
    public synchronized List<Task> findAll() {
        return new ArrayList<>(taskDb.values());
    }

    @Override
    public synchronized List<Task> findByProjectId(ProjectId projectId) {
        Objects.requireNonNull(projectId, "ProjectId cannot be null");
        return taskDb.values().stream()
            .filter(task -> task.getProjectId().equals(projectId))
            .toList();
    }

    @Override
    public synchronized boolean existsByProjectIdAndTitle(ProjectId projectId, TaskTitle title) {
        Objects.requireNonNull(projectId, "ProjectId cannot be null");
        Objects.requireNonNull(title, "TaskTitle cannot be null");
        return taskDb.values().stream()
            .anyMatch(task -> task.getProjectId().equals(projectId)
                && task.getTitle().sameValueAs(title));
    }

    @Override
    public synchronized void delete(TaskId id) {
        taskDb.remove(id);
    }
}
