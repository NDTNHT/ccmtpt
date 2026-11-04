package com.taskmanagement.adapter.api;

import com.taskmanagement.adapter.api.dto.ApiResponse;
import com.taskmanagement.adapter.api.dto.TaskCreateRequest;
import com.taskmanagement.adapter.api.dto.TaskRescheduleRequest;
import com.taskmanagement.application.boundary.TaskInputBoundary;
import com.taskmanagement.application.dto.TaskInputDTO;
import com.taskmanagement.application.dto.TaskOutputDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskApiController {
    private final TaskInputBoundary taskInput;

    public TaskApiController(TaskInputBoundary taskInput) {
        this.taskInput = taskInput;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskOutputDTO>> createTask(
        @Valid @RequestBody TaskCreateRequest request
    ) {
        TaskInputDTO input = new TaskInputDTO(
            request.title(),
            request.description(),
            request.dueDate(),
            request.priority(),
            request.projectId()
        );
        TaskOutputDTO output = taskInput.createTask(input);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Task created successfully", output));
    }

    @GetMapping
    public ApiResponse<List<TaskOutputDTO>> getTasks() {
        return ApiResponse.success("Tasks retrieved successfully", taskInput.getTasks());
    }

    @GetMapping("/{taskId}")
    public ApiResponse<TaskOutputDTO> getTask(@PathVariable UUID taskId) {
        return ApiResponse.success("Task retrieved successfully", taskInput.getTask(taskId));
    }

    @GetMapping("/by-project/{projectId}")
    public ApiResponse<List<TaskOutputDTO>> getTasksByProject(@PathVariable UUID projectId) {
        return ApiResponse.success(
            "Tasks retrieved successfully", taskInput.getTasksByProject(projectId));
    }

    @PostMapping("/{taskId}/start")
    public ApiResponse<TaskOutputDTO> startTask(@PathVariable UUID taskId) {
        return ApiResponse.success("Task started successfully", taskInput.startTask(taskId));
    }

    @PostMapping("/{taskId}/complete")
    public ApiResponse<TaskOutputDTO> completeTask(@PathVariable UUID taskId) {
        return ApiResponse.success("Task completed successfully", taskInput.completeTask(taskId));
    }

    @PostMapping("/{taskId}/cancel")
    public ApiResponse<TaskOutputDTO> cancelTask(@PathVariable UUID taskId) {
        return ApiResponse.success("Task cancelled successfully", taskInput.cancelTask(taskId));
    }

    @PostMapping("/{taskId}/refresh-priority")
    public ApiResponse<TaskOutputDTO> refreshTaskPriority(@PathVariable UUID taskId) {
        return ApiResponse.success(
            "Task priority refreshed successfully", taskInput.refreshTaskPriority(taskId));
    }

    @PatchMapping("/{taskId}/deadline")
    public ApiResponse<TaskOutputDTO> rescheduleTask(
        @PathVariable UUID taskId,
        @Valid @RequestBody TaskRescheduleRequest request
    ) {
        return ApiResponse.success(
            "Task rescheduled successfully", taskInput.rescheduleTask(taskId, request.dueDate()));
    }
}
