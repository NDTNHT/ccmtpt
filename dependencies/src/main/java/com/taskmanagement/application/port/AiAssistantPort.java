package com.taskmanagement.application.port;

import com.taskmanagement.adapter.ai.AiConfig;
import com.taskmanagement.adapter.ai.AiProtocol;
import com.taskmanagement.adapter.ai.AiProviderRegistry;
import com.taskmanagement.application.dto.ai.AiAnalysisResultDto;
import com.taskmanagement.application.dto.ai.AiDeadlineEstimationDto;
import com.taskmanagement.application.dto.ai.AiPriorityRecommendationDto;
import com.taskmanagement.application.dto.ai.AiTaskDecompositionDto;
import com.taskmanagement.application.dto.ai.AiTaskSummaryDto;
import com.taskmanagement.application.dto.ai.ProviderVerificationResultDto;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;

/**
 * Outbound port for interacting with AI models (OpenAI-compatible, Anthropic-compatible,
 * OpenCode-ported, or fallback providers).
 */
public interface AiAssistantPort {

    /**
     * Decomposes the task's title and description into a checklist of actionable subtasks.
     */
    AiTaskDecompositionDto decomposeTask(Task task, Project project, String customPrompt);

    /**
     * Suggests additional subtasks based on existing subtasks and task description.
     */
    AiTaskDecompositionDto suggestSubTasks(Task task, Project project, String customPrompt);

    /**
     * Estimates required effort and recommended deadline for completing the task.
     */
    AiDeadlineEstimationDto estimateDeadline(Task task, Project project, String customPrompt);

    /**
     * Evaluates task complexity, scope, and urgency to recommend a priority.
     */
    AiPriorityRecommendationDto analyzePriority(Task task, Project project, Priority domainPriority, String customPrompt);

    /**
     * Generates a concise executive and progress summary of the task.
     */
    AiTaskSummaryDto generateSummary(Task task, Project project, String customPrompt);

    /**
     * Performs an end-to-end comprehensive AI analysis covering decomposition, estimation,
     * priority evaluation and summary generation in a unified execution.
     */
    AiAnalysisResultDto analyzeTaskComprehensive(Task task, Project project, Priority domainPriority, String customPrompt);

    /**
     * Verifies API credentials and connectivity by contacting the provider's /models endpoint
     * and discovers all available models.
     */
    default ProviderVerificationResultDto verifyAndDiscoverModels(String baseUrl, String apiKey, AiProtocol protocol, int timeoutSeconds) {
        return ProviderVerificationResultDto.failure(501, "Provider verification not supported by this adapter", protocol != null ? protocol.name() : "UNKNOWN");
    }

    /**
     * Returns the underlying AI configuration if available.
     */
    default AiConfig getConfig() {
        return null;
    }

    /**
     * Returns the provider registry if available.
     */
    default AiProviderRegistry getRegistry() {
        return null;
    }
}
