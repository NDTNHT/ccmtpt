package com.taskmanagement.adapter.api;

import com.taskmanagement.adapter.ai.AiConfig;
import com.taskmanagement.adapter.ai.AiProtocol;
import com.taskmanagement.adapter.ai.CustomProviderDefinition;
import com.taskmanagement.adapter.api.dto.AiAnalysisRequest;
import com.taskmanagement.adapter.api.dto.AiApplyDeadlineRequest;
import com.taskmanagement.adapter.api.dto.AiApplyDecompositionRequest;
import com.taskmanagement.adapter.api.dto.AiApplyPriorityRequest;
import com.taskmanagement.adapter.api.dto.AiConfigDto;
import com.taskmanagement.adapter.api.dto.ApiResponse;
import com.taskmanagement.adapter.api.dto.VerifyProviderRequest;
import com.taskmanagement.application.boundary.AiTaskInputBoundary;
import com.taskmanagement.application.dto.ai.AiAnalysisResultDto;
import com.taskmanagement.application.dto.ai.AiDeadlineEstimationDto;
import com.taskmanagement.application.dto.ai.AiPriorityRecommendationDto;
import com.taskmanagement.application.dto.ai.AiTaskDecompositionDto;
import com.taskmanagement.application.dto.ai.AiTaskSummaryDto;
import com.taskmanagement.application.dto.ai.ProviderVerificationResultDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class AiTaskApiController {
    private final AiTaskInputBoundary aiTaskInput;

    public AiTaskApiController(AiTaskInputBoundary aiTaskInput) {
        this.aiTaskInput = Objects.requireNonNull(aiTaskInput, "aiTaskInput cannot be null");
    }

    @GetMapping("/ai/config")
    public ApiResponse<AiConfigDto> getConfig() {
        AiConfig config = aiTaskInput.getAiConfig();
        if (config == null) {
            return ApiResponse.success("No active AI configuration found", null);
        }
        String maskedKey = (config.getApiKey() != null && !config.getApiKey().isBlank())
            ? (config.getApiKey().length() > 6
                ? config.getApiKey().substring(0, 3) + "..." + config.getApiKey().substring(config.getApiKey().length() - 3)
                : "***")
            : "";
        return ApiResponse.success("AI configuration retrieved successfully",
            new AiConfigDto(config.getProvider(), config.getBaseUrl(), config.getModel(), maskedKey, config.getTimeoutSeconds()));
    }

    @PostMapping("/ai/config")
    public ApiResponse<AiConfigDto> updateConfig(@RequestBody AiConfigDto request) {
        if (request != null) {
            aiTaskInput.updateAiConfig(request.baseUrl(), request.model(), request.apiKey());
        }
        return getConfig();
    }

    @GetMapping("/ai/providers")
    public ApiResponse<List<CustomProviderDefinition>> getProviders() {
        return ApiResponse.success("Registered AI providers retrieved successfully", aiTaskInput.getProviders());
    }

    @PostMapping("/ai/providers")
    public ApiResponse<CustomProviderDefinition> registerProvider(@Valid @RequestBody CustomProviderDefinition request) {
        aiTaskInput.registerCustomProvider(request);
        return ApiResponse.success("Custom AI provider registered successfully", request);
    }

    @PostMapping("/ai/providers/{providerId}/activate")
    public ApiResponse<CustomProviderDefinition> activateProvider(@PathVariable String providerId) {
        aiTaskInput.setActiveProvider(providerId);
        return ApiResponse.success("Provider activated successfully: " + providerId, aiTaskInput.getActiveProvider());
    }

    @PostMapping("/ai/providers/verify")
    public ApiResponse<ProviderVerificationResultDto> verifyProvider(@RequestBody VerifyProviderRequest request) {
        String baseUrl = request != null ? request.baseUrl() : null;
        String apiKey = request != null ? request.apiKey() : null;
        AiProtocol protocol = request != null && request.protocol() != null ? request.protocol() : AiProtocol.OPENAI;
        int timeoutSeconds = (request != null && request.timeoutSeconds() != null) ? request.timeoutSeconds() : 15;
        ProviderVerificationResultDto result = aiTaskInput.verifyAndDiscoverModels(baseUrl, apiKey, protocol, timeoutSeconds);
        return ApiResponse.success(result.message(), result);
    }

    @PostMapping("/ai/providers/{providerId}/verify")
    public ApiResponse<ProviderVerificationResultDto> verifyRegisteredProvider(@PathVariable String providerId) {
        ProviderVerificationResultDto result = aiTaskInput.verifyRegisteredProvider(providerId);
        return ApiResponse.success(result.message(), result);
    }

    @PostMapping("/{taskId}/ai/analyze")
    public ApiResponse<AiAnalysisResultDto> analyze(
        @PathVariable UUID taskId,
        @RequestBody(required = false) AiAnalysisRequest request
    ) {
        String customPrompt = request != null ? request.customSystemPrompt() : null;
        AiAnalysisResultDto result = aiTaskInput.analyzeTask(taskId, customPrompt);
        return ApiResponse.success("Task AI analysis completed successfully", result);
    }

    @PostMapping("/{taskId}/ai/decompose")
    public ApiResponse<AiTaskDecompositionDto> decompose(
        @PathVariable UUID taskId,
        @RequestBody(required = false) AiAnalysisRequest request
    ) {
        String customPrompt = request != null ? request.customSystemPrompt() : null;
        AiTaskDecompositionDto result = aiTaskInput.decomposeTask(taskId, customPrompt);
        return ApiResponse.success("Task checklist decomposed successfully", result);
    }

    @PostMapping("/{taskId}/ai/suggest-subtasks")
    public ApiResponse<AiTaskDecompositionDto> suggestSubTasks(
        @PathVariable UUID taskId,
        @RequestBody(required = false) AiAnalysisRequest request
    ) {
        String customPrompt = request != null ? request.customSystemPrompt() : null;
        AiTaskDecompositionDto result = aiTaskInput.suggestSubTasks(taskId, customPrompt);
        return ApiResponse.success("Subtasks suggested successfully", result);
    }

    @PostMapping("/{taskId}/ai/estimate-deadline")
    public ApiResponse<AiDeadlineEstimationDto> estimateDeadline(
        @PathVariable UUID taskId,
        @RequestBody(required = false) AiAnalysisRequest request
    ) {
        String customPrompt = request != null ? request.customSystemPrompt() : null;
        AiDeadlineEstimationDto result = aiTaskInput.estimateDeadline(taskId, customPrompt);
        return ApiResponse.success("Deadline estimated successfully", result);
    }

    @PostMapping("/{taskId}/ai/priority")
    public ApiResponse<AiPriorityRecommendationDto> analyzePriority(
        @PathVariable UUID taskId,
        @RequestBody(required = false) AiAnalysisRequest request
    ) {
        String customPrompt = request != null ? request.customSystemPrompt() : null;
        AiPriorityRecommendationDto result = aiTaskInput.analyzePriority(taskId, customPrompt);
        return ApiResponse.success("Priority evaluated successfully", result);
    }

    @PostMapping("/{taskId}/ai/summary")
    public ApiResponse<AiTaskSummaryDto> generateSummary(
        @PathVariable UUID taskId,
        @RequestBody(required = false) AiAnalysisRequest request
    ) {
        String customPrompt = request != null ? request.customSystemPrompt() : null;
        AiTaskSummaryDto result = aiTaskInput.generateSummary(taskId, customPrompt);
        return ApiResponse.success("Task summary generated successfully", result);
    }

    @PostMapping("/{taskId}/ai/apply-decomposition")
    public ApiResponse<Void> applyDecomposition(
        @PathVariable UUID taskId,
        @Valid @RequestBody AiApplyDecompositionRequest request
    ) {
        aiTaskInput.applyDecomposition(taskId, request.subTasks());
        return ApiResponse.success("Checklist subtasks applied to task successfully", null);
    }

    @PostMapping("/{taskId}/ai/apply-deadline")
    public ApiResponse<Void> applyDeadline(
        @PathVariable UUID taskId,
        @Valid @RequestBody AiApplyDeadlineRequest request
    ) {
        aiTaskInput.applyEstimatedDeadline(taskId, request.recommendedDeadline());
        return ApiResponse.success("Estimated deadline applied to task successfully", null);
    }

    @PostMapping("/{taskId}/ai/apply-priority")
    public ApiResponse<Void> applyPriority(
        @PathVariable UUID taskId,
        @Valid @RequestBody AiApplyPriorityRequest request
    ) {
        aiTaskInput.applyRecommendedPriority(taskId, request.priority());
        return ApiResponse.success("Recommended priority applied to task successfully", null);
    }
}
