package com.taskmanagement.domain.task.repository;

import com.taskmanagement.domain.project.valueobject.ProjectId;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.valueobject.TaskId;
import com.taskmanagement.domain.task.valueobject.TaskTitle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence contract for tasks.
 */
public interface TaskRepository {
    void save(Task task);

    Optional<Task> findById(TaskId id);

    default Optional<Task> findById(UUID id) {
        return findById(TaskId.of(id));
    }

    List<Task> findAll();

    List<Task> findByProjectId(ProjectId projectId);

    boolean existsByProjectIdAndTitle(ProjectId projectId, TaskTitle title);

    void delete(TaskId id);
}
