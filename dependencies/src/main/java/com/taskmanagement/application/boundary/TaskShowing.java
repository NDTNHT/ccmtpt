package com.taskmanagement.application.boundary;

import com.taskmanagement.application.dto.TaskOutputDTO;

/**
 * Receives the task read model after a use case completes.
 */
@FunctionalInterface
public interface TaskShowing {
    void showResult(TaskOutputDTO outputData);
}
