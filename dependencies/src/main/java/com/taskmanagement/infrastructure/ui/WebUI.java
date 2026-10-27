package com.taskmanagement.infrastructure.ui;

import com.taskmanagement.application.boundary.TaskInputBoundary;
import com.taskmanagement.application.dto.TaskInputDTO;
import com.taskmanagement.application.dto.TaskOutputDTO;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Web client adapter for task operations.
 */
public class WebUI {
    private final TaskInputBoundary taskInput;

    public WebUI(TaskInputBoundary taskInput) {
        this.taskInput = Objects.requireNonNull(taskInput, "TaskInputBoundary cannot be null");
    }

    public TaskOutputDTO createTask(TaskInputDTO input) {
        return taskInput.createTask(input);
    }

    public List<TaskOutputDTO> getTasks() {
        return taskInput.getTasks();
    }

    public TaskOutputDTO getTask(UUID taskId) {
        return taskInput.getTask(taskId);
    }

    public TaskOutputDTO startTask(UUID taskId) {
        return taskInput.startTask(taskId);
    }

    public TaskOutputDTO completeTask(UUID taskId) {
        return taskInput.completeTask(taskId);
    }

    public TaskOutputDTO cancelTask(UUID taskId) {
        return taskInput.cancelTask(taskId);
    }
}
