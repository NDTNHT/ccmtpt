package com.taskmanagement.application.control;

import com.taskmanagement.adapter.ai.AiConfig;
import com.taskmanagement.adapter.ai.AiProtocol;
import com.taskmanagement.adapter.ai.AiProviderRegistry;
import com.taskmanagement.adapter.ai.CustomProviderDefinition;
import com.taskmanagement.application.boundary.AiTaskInputBoundary;
import com.taskmanagement.application.dto.ai.AiAnalysisResultDto;
import com.taskmanagement.application.dto.ai.AiDeadlineEstimationDto;
import com.taskmanagement.application.dto.ai.AiPriorityRecommendationDto;
import com.taskmanagement.application.dto.ai.AiTaskDecompositionDto;
import com.taskmanagement.application.dto.ai.AiTaskSummaryDto;
import com.taskmanagement.application.dto.ai.ProviderVerificationResultDto;
import com.taskmanagement.application.exception.TaskNotFoundException;
import com.taskmanagement.application.port.AiAssistantPort;
import com.taskmanagement.application.port.DomainEventDispatcher;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.project.repository.ProjectRepository;
import com.taskmanagement.domain.project.valueobject.ProjectId;
import com.taskmanagement.domain.service.TaskPriorityCalculator;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.exception.TaskValidationException;
import com.taskmanagement.domain.task.repository.TaskRepository;
import com.taskmanagement.domain.task.valueobject.Deadline;
import com.taskmanagement.domain.task.valueobject.TaskId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Coordinates AI assistant operations with domain rules, repositories, and event publishing.
 */
public class AiTaskControl implements AiTaskInputBoundary {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final AiAssistantPort aiAssistantPort;
    private final TaskPriorityCalculator priorityCalculator;
    private final DomainEventDispatcher eventDispatcher;

    public AiTaskControl(TaskRepository taskRepository,
                         ProjectRepository projectRepository,
                         AiAssistantPort aiAssistantPort,
                         DomainEventDispatcher eventDispatcher) {
        this(taskRepository, projectRepository, aiAssistantPort, new TaskPriorityCalculator(), eventDispatcher);
    }

    public AiTaskControl(TaskRepository taskRepository,
                         ProjectRepository projectRepository,
                         AiAssistantPort aiAssistantPort,
                         TaskPriorityCalculator priorityCalculator,
                         DomainEventDispatcher eventDispatcher) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "TaskRepository cannot be null");
        this.projectRepository = Objects.requireNonNull(projectRepository, "ProjectRepository cannot be null");
        this.aiAssistantPort = Objects.requireNonNull(aiAssistantPort, "AiAssistantPort cannot be null");
        this.priorityCalculator = priorityCalculator != null ? priorityCalculator : new TaskPriorityCalculator();
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher, "DomainEventDispatcher cannot be null");
    }

    @Override
    public AiTaskDecompositionDto decomposeTask(UUID taskId, String customPrompt) {
        Task task = getTaskOrThrow(taskId);
        Project project = getProjectOrNull(task.getProjectId());
        return aiAssistantPort.decomposeTask(task, project, customPrompt);
    }

    @Override
    public AiTaskDecompositionDto suggestSubTasks(UUID taskId, String customPrompt) {
        Task task = getTaskOrThrow(taskId);
        Project project = getProjectOrNull(task.getProjectId());
        return aiAssistantPort.suggestSubTasks(task, project, customPrompt);
    }

    @Override
    public AiDeadlineEstimationDto estimateDeadline(UUID taskId, String customPrompt) {
        Task task = getTaskOrThrow(taskId);
        Project project = getProjectOrNull(task.getProjectId());
        return aiAssistantPort.estimateDeadline(task, project, customPrompt);
    }

    @Override
    public AiPriorityRecommendationDto analyzePriority(UUID taskId, String customPrompt) {
        Task task = getTaskOrThrow(taskId);
        Project project = getProjectOrNull(task.getProjectId());
        Priority domainPriority = priorityCalculator.calculatePriority(task, project);
        return aiAssistantPort.analyzePriority(task, project, domainPriority, customPrompt);
    }

    @Override
    public AiTaskSummaryDto generateSummary(UUID taskId, String customPrompt) {
        Task task = getTaskOrThrow(taskId);
        Project project = getProjectOrNull(task.getProjectId());
        return aiAssistantPort.generateSummary(task, project, customPrompt);
    }

    @Override
    public AiAnalysisResultDto analyzeTask(UUID taskId, String customPrompt) {
        Task task = getTaskOrThrow(taskId);
        Project project = getProjectOrNull(task.getProjectId());
        Priority domainPriority = priorityCalculator.calculatePriority(task, project);
        return aiAssistantPort.analyzeTaskComprehensive(task, project, domainPriority, customPrompt);
    }

    @Override
    public void applyDecomposition(UUID taskId, List<String> subTasks) {
        if (subTasks == null || subTasks.isEmpty()) {
            return;
        }
        Task task = getTaskOrThrow(taskId);
        for (String title : subTasks) {
            if (title != null && !title.trim().isBlank()) {
                task.addSubTask(title.trim());
            }
        }
        saveAndDispatch(task);
    }

    @Override
    public void applyEstimatedDeadline(UUID taskId, Instant deadlineInstant) {
        if (deadlineInstant == null) {
            throw new TaskValidationException("Deadline instant cannot be null");
        }
        Task task = getTaskOrThrow(taskId);
        task.reschedule(Deadline.of(deadlineInstant));
        saveAndDispatch(task);
    }

    @Override
    public void applyRecommendedPriority(UUID taskId, Priority priority) {
        if (priority == null) {
            throw new TaskValidationException("Priority cannot be null");
        }
        Task task = getTaskOrThrow(taskId);
        task.changePriority(priority);
        saveAndDispatch(task);
    }

    @Override
    public AiConfig getAiConfig() {
        return aiAssistantPort.getConfig();
    }

    @Override
    public void updateAiConfig(String baseUrl, String model, String apiKey) {
        AiConfig config = aiAssistantPort.getConfig();
        if (config != null) {
            if (baseUrl != null && !baseUrl.isBlank()) {
                config.setBaseUrl(baseUrl);
            }
            if (model != null && !model.isBlank()) {
                config.setModel(model);
            }
            if (apiKey != null) {
                config.setApiKey(apiKey);
            }
        }
    }

    @Override
    public List<CustomProviderDefinition> getProviders() {
        AiProviderRegistry registry = aiAssistantPort.getRegistry();
        if (registry != null) {
            return registry.listProviders();
        }
        return List.of();
    }

    @Override
    public CustomProviderDefinition getActiveProvider() {
        AiProviderRegistry registry = aiAssistantPort.getRegistry();
        if (registry != null) {
            return registry.resolveActiveDefinition();
        }
        return null;
    }

    @Override
    public void registerCustomProvider(CustomProviderDefinition provider) {
        AiProviderRegistry registry = aiAssistantPort.getRegistry();
        if (registry != null) {
            registry.registerProvider(provider);
        }
    }

    @Override
    public void setActiveProvider(String providerId) {
        AiProviderRegistry registry = aiAssistantPort.getRegistry();
        if (registry != null) {
            registry.setActiveProviderId(providerId);
        }
    }

    @Override
    public ProviderVerificationResultDto verifyAndDiscoverModels(String baseUrl, String apiKey, AiProtocol protocol, int timeoutSeconds) {
        return aiAssistantPort.verifyAndDiscoverModels(baseUrl, apiKey, protocol, timeoutSeconds);
    }

    @Override
    public ProviderVerificationResultDto verifyRegisteredProvider(String providerId) {
        AiProviderRegistry registry = aiAssistantPort.getRegistry();
        if (registry != null) {
            return registry.verifyRegisteredProvider(providerId);
        }
        return ProviderVerificationResultDto.failure(500, "Provider registry is not available", "UNKNOWN");
    }

    @Override
    public void updateProviderDefaultModel(String providerId, String defaultModel) {
        AiProviderRegistry registry = aiAssistantPort.getRegistry();
        if (registry != null) {
            registry.updateProviderDefaultModel(providerId, defaultModel);
        }
    }

    // Helpers

    private Task getTaskOrThrow(UUID taskId) {
        Objects.requireNonNull(taskId, "Task id cannot be null");
        return taskRepository.findById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId));
    }

    private Project getProjectOrNull(ProjectId projectId) {
        if (projectId == null) {
            return null;
        }
        return projectRepository.findById(projectId).orElse(null);
    }

    private void saveAndDispatch(Task task) {
        taskRepository.save(task);
        eventDispatcher.dispatch(task.pollDomainEvents());
    }
}
