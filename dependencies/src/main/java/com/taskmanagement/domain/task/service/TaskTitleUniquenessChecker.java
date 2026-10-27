package com.taskmanagement.domain.task.service;

import com.taskmanagement.domain.project.valueobject.ProjectId;
import com.taskmanagement.domain.task.valueobject.TaskTitle;

/**
 * Checks that no two tasks in the same project share a title (case-insensitive).
 */
@FunctionalInterface
public interface TaskTitleUniquenessChecker {
    boolean isUnique(ProjectId projectId, TaskTitle title);
}
