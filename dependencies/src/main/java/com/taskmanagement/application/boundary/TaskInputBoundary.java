package com.taskmanagement.application.boundary;

import com.taskmanagement.application.dto.TaskInputDTO;
import com.taskmanagement.application.dto.TaskOutputDTO;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Task use cases. Identifiers are UUIDs.
 */
public interface TaskInputBoundary {
    TaskOutputDTO createTask(TaskInputDTO input);

    TaskOutputDTO startTask(UUID taskId);

    TaskOutputDTO completeTask(UUID taskId);

    TaskOutputDTO cancelTask(UUID taskId);

    TaskOutputDTO refreshTaskPriority(UUID taskId);

    TaskOutputDTO rescheduleTask(UUID taskId, Instant newDueDate);

    TaskOutputDTO getTask(UUID taskId);

    List<TaskOutputDTO> getTasks();

    List<TaskOutputDTO> getTasksByProject(UUID projectId);
}
