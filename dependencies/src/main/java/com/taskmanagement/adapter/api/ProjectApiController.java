package com.taskmanagement.adapter.api;

import com.taskmanagement.adapter.api.dto.ApiResponse;
import com.taskmanagement.adapter.api.dto.ProjectCreateRequest;
import com.taskmanagement.application.boundary.ProjectInputBoundary;
import com.taskmanagement.application.dto.ProjectOutputDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectApiController {
    private final ProjectInputBoundary projectInput;

    public ProjectApiController(ProjectInputBoundary projectInput) {
        this.projectInput = projectInput;
    }

    @GetMapping
    public ApiResponse<List<ProjectOutputDTO>> getProjects() {
        return ApiResponse.success(
            "Projects retrieved successfully", projectInput.getProjects());
    }

    @GetMapping("/inbox")
    public ApiResponse<ProjectOutputDTO> getInbox() {
        return ApiResponse.success(
            "Inbox retrieved successfully", projectInput.getInboxProject());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectOutputDTO>> createProject(
        @Valid @RequestBody ProjectCreateRequest request
    ) {
        ProjectOutputDTO output = projectInput.createProject(
            request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Project created successfully", output));
    }
}
