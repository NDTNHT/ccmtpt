package com.taskmanagement.adapter.ai;

import com.taskmanagement.application.dto.ai.AiAnalysisResultDto;
import com.taskmanagement.application.dto.ai.AiDeadlineEstimationDto;
import com.taskmanagement.application.dto.ai.AiPriorityRecommendationDto;
import com.taskmanagement.application.dto.ai.AiTaskDecompositionDto;
import com.taskmanagement.application.dto.ai.AiTaskSummaryDto;
import com.taskmanagement.application.dto.ai.ProviderVerificationResultDto;
import com.taskmanagement.application.port.AiAssistantPort;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;

import java.util.Objects;

/**
 * Composite adapter that selects dynamically between user-registered providers (OpenAI, Anthropic,
 * Pollinations, Ollama, or custom).
 */
public class CompositeAiAdapter implements AiAssistantPort {
    private final AiConfig config;
    private final AiProviderRegistry registry;
    private final AiAssistantPort customActiveAdapter;

    public CompositeAiAdapter(AiConfig config) {
        this(config, new AiProviderRegistry(config));
    }

    public CompositeAiAdapter(AiConfig config, AiProviderRegistry registry) {
        this.config = config != null ? config : AiConfig.defaults();
        this.registry = registry != null ? registry : new AiProviderRegistry(this.config);
        this.customActiveAdapter = null;
    }

    public CompositeAiAdapter(AiAssistantPort customActiveAdapter) {
        this.config = AiConfig.defaults();
        this.registry = new AiProviderRegistry(this.config);
        this.customActiveAdapter = Objects.requireNonNull(customActiveAdapter, "customActiveAdapter cannot be null");
    }

    @Override
    public AiProviderRegistry getRegistry() {
        return registry;
    }

    @Override
    public AiConfig getConfig() {
        return config;
    }

    @Override
    public ProviderVerificationResultDto verifyAndDiscoverModels(String baseUrl, String apiKey, AiProtocol protocol, int timeoutSeconds) {
        if (customActiveAdapter != null) {
            return customActiveAdapter.verifyAndDiscoverModels(baseUrl, apiKey, protocol, timeoutSeconds);
        }
        return registry.verifyAndDiscoverModels(baseUrl, apiKey, protocol, timeoutSeconds);
    }

    private AiAssistantPort resolveActiveAdapter() {
        if (customActiveAdapter != null) {
            return customActiveAdapter;
        }
        CustomProviderDefinition activeDefinition = registry.resolveActiveDefinition();
        return registry.createAdapterFor(activeDefinition);
    }

    @Override
    public AiTaskDecompositionDto decomposeTask(Task task, Project project, String customPrompt) {
        return resolveActiveAdapter().decomposeTask(task, project, customPrompt);
    }

    @Override
    public AiTaskDecompositionDto suggestSubTasks(Task task, Project project, String customPrompt) {
        return resolveActiveAdapter().suggestSubTasks(task, project, customPrompt);
    }

    @Override
    public AiDeadlineEstimationDto estimateDeadline(Task task, Project project, String customPrompt) {
        return resolveActiveAdapter().estimateDeadline(task, project, customPrompt);
    }

    @Override
    public AiPriorityRecommendationDto analyzePriority(Task task, Project project, Priority domainPriority, String customPrompt) {
        return resolveActiveAdapter().analyzePriority(task, project, domainPriority, customPrompt);
    }

    @Override
    public AiTaskSummaryDto generateSummary(Task task, Project project, String customPrompt) {
        return resolveActiveAdapter().generateSummary(task, project, customPrompt);
    }

    @Override
    public AiAnalysisResultDto analyzeTaskComprehensive(Task task, Project project, Priority domainPriority, String customPrompt) {
        return resolveActiveAdapter().analyzeTaskComprehensive(task, project, domainPriority, customPrompt);
    }
}
