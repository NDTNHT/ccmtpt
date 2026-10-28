package com.taskmanagement.application.boundary;

import com.taskmanagement.adapter.ai.AiConfig;
import com.taskmanagement.adapter.ai.AiProtocol;
import com.taskmanagement.adapter.ai.CustomProviderDefinition;
import com.taskmanagement.application.dto.ai.AiAnalysisResultDto;
import com.taskmanagement.application.dto.ai.AiDeadlineEstimationDto;
import com.taskmanagement.application.dto.ai.AiPriorityRecommendationDto;
import com.taskmanagement.application.dto.ai.AiTaskDecompositionDto;
import com.taskmanagement.application.dto.ai.AiTaskSummaryDto;
import com.taskmanagement.application.dto.ai.ProviderVerificationResultDto;
import com.taskmanagement.domain.task.enums.Priority;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Inbound port exposing AI-driven task enhancement, planning, key verification,
 * model discovery, and custom provider management capabilities.
 */
public interface AiTaskInputBoundary {

    /**
     * Breaks down an unpersisted task draft into concrete checklist subtasks.
     */
    AiTaskDecompositionDto decomposeDraft(String title, String description, UUID projectId, String customPrompt);

    /**
     * Breaks down the specified task into concrete checklist subtasks.
     */
    AiTaskDecompositionDto decomposeTask(UUID taskId, String customPrompt);

    /**
     * Suggests complementary subtasks for the specified task.
     */
    AiTaskDecompositionDto suggestSubTasks(UUID taskId, String customPrompt);

    /**
     * Estimates completion effort and proposes a deadline for the specified task.
     */
    AiDeadlineEstimationDto estimateDeadline(UUID taskId, String customPrompt);

    /**
     * Calculates hybrid semantic and business-rule priority for the specified task.
     */
    AiPriorityRecommendationDto analyzePriority(UUID taskId, String customPrompt);

    /**
     * Generates an executive progress summary for the specified task.
     */
    AiTaskSummaryDto generateSummary(UUID taskId, String customPrompt);

    /**
     * Runs full AI analysis for the specified task.
     */
    AiAnalysisResultDto analyzeTask(UUID taskId, String customPrompt);

    /**
     * Confirms and persists the provided subtask checklist into the task aggregate root.
     */
    void applyDecomposition(UUID taskId, List<String> subTasks);

    /**
     * Confirms and updates the task deadline with the estimated instant.
     */
    void applyEstimatedDeadline(UUID taskId, Instant deadlineInstant);

    /**
     * Confirms and updates the task priority with the recommended priority.
     */
    void applyRecommendedPriority(UUID taskId, Priority priority);

    /**
     * Retrieves the current AI configuration.
     */
    AiConfig getAiConfig();

    /**
     * Dynamically updates runtime AI configuration (Base URL, Model, API Key).
     */
    void updateAiConfig(String baseUrl, String model, String apiKey);

    /**
     * Lists all registered standard and custom providers.
     */
    List<CustomProviderDefinition> getProviders();

    /**
     * Retrieves the definition of the currently active provider.
     */
    CustomProviderDefinition getActiveProvider();

    /**
     * Registers a new custom provider (OpenAI or Anthropic protocol).
     */
    void registerCustomProvider(CustomProviderDefinition provider);

    /**
     * Activates a provider by its unique ID.
     */
    void setActiveProvider(String providerId);

    /**
     * Verifies API credentials and connectivity by contacting the provider's /models endpoint
     * and discovers all available models.
     */
    ProviderVerificationResultDto verifyAndDiscoverModels(String baseUrl, String apiKey, AiProtocol protocol, int timeoutSeconds);

    /**
     * Verifies an already-registered provider and updates its discovered available models.
     */
    ProviderVerificationResultDto verifyRegisteredProvider(String providerId);

    /**
     * Updates the default model of an existing registered provider.
     */
    void updateProviderDefaultModel(String providerId, String defaultModel);
}
