package com.taskmanagement.adapter.ai;

import com.taskmanagement.adapter.ai.dto.AiPromptOverrideRequest;
import com.taskmanagement.adapter.api.dto.ApiResponse;
import com.taskmanagement.application.ai.AiTaskAssistantInputBoundary;
import com.taskmanagement.application.ai.dto.AiPriorityProposalOutputDTO;
import com.taskmanagement.application.ai.dto.AiPromptOverrideInputDTO;
import com.taskmanagement.application.ai.dto.AiSummaryOutputDTO;
import com.taskmanagement.application.ai.dto.AiTaskBreakdownOutputDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * REST controller exposing AI Task Assistant features.
 * Provides on-demand task breakdown, priority evaluation, and task summarization.
 */
@RestController
@RequestMapping({"/api/v1/tasks", "/api/tasks"})
public class AiTaskApiController {

    private final AiTaskAssistantInputBoundary aiAssistant;

    public AiTaskApiController(AiTaskAssistantInputBoundary aiAssistant) {
        this.aiAssistant = Objects.requireNonNull(aiAssistant, "AiTaskAssistantInputBoundary cannot be null");
    }

    /**
     * AI-assisted task breakdown into smaller subtasks and estimated deadline.
     * When apply=true, persists proposed subtasks and estimated deadline to the task aggregate.
     */
    @PostMapping("/{taskId}/ai-breakdown")
    public ResponseEntity<ApiResponse<AiTaskBreakdownOutputDTO>> breakdownTask(
        @PathVariable UUID taskId,
        @RequestParam(defaultValue = "false") boolean apply,
        @RequestBody(required = false) AiPromptOverrideRequest request
    ) {
        AiPromptOverrideInputDTO override = toOverrideDTO(request);
        AiTaskBreakdownOutputDTO output = aiAssistant.breakdownTask(taskId, apply, override);
        String message = apply
            ? "Task breakdown generated and applied successfully"
            : "Task breakdown preview generated successfully";
        return ResponseEntity.ok(ApiResponse.success(message, output));
    }

    /**
     * AI-assisted task priority evaluation.
     * When apply=true, updates the task priority.
     */
    @PostMapping("/{taskId}/ai-prioritize")
    public ResponseEntity<ApiResponse<AiPriorityProposalOutputDTO>> prioritizeTask(
        @PathVariable UUID taskId,
        @RequestParam(defaultValue = "false") boolean apply,
        @RequestBody(required = false) AiPromptOverrideRequest request
    ) {
        AiPromptOverrideInputDTO override = toOverrideDTO(request);
        AiPriorityProposalOutputDTO output = aiAssistant.prioritizeTask(taskId, apply, override);
        String message = apply
            ? "Task priority evaluated and updated successfully"
            : "Task priority suggestion generated successfully";
        return ResponseEntity.ok(ApiResponse.success(message, output));
    }

    /**
     * AI-assisted task summary generation including key highlights and next steps.
     */
    @PostMapping("/{taskId}/ai-summary")
    public ResponseEntity<ApiResponse<AiSummaryOutputDTO>> summarizeTask(
        @PathVariable UUID taskId,
        @RequestBody(required = false) AiPromptOverrideRequest request
    ) {
        AiPromptOverrideInputDTO override = toOverrideDTO(request);
        AiSummaryOutputDTO output = aiAssistant.summarizeTask(taskId, override);
        return ResponseEntity.ok(ApiResponse.success("Task summary generated successfully", output));
    }

    private static AiPromptOverrideInputDTO toOverrideDTO(AiPromptOverrideRequest request) {
        if (request == null) {
            return null;
        }
        return request.toInputDTO();
    }
}
