package com.taskmanagement.application.ai;

import com.taskmanagement.application.ai.dto.AiPriorityProposalOutputDTO;
import com.taskmanagement.application.ai.dto.AiPromptOverrideInputDTO;
import com.taskmanagement.application.ai.dto.AiSubTaskProposalOutputDTO;
import com.taskmanagement.application.ai.dto.AiSummaryOutputDTO;
import com.taskmanagement.application.ai.dto.AiTaskBreakdownOutputDTO;
import com.taskmanagement.application.exception.ProjectNotFoundException;
import com.taskmanagement.application.exception.TaskNotFoundException;
import com.taskmanagement.application.port.DomainEventDispatcher;
import com.taskmanagement.domain.ai.AiModelPort;
import com.taskmanagement.domain.ai.AiPriorityProposal;
import com.taskmanagement.domain.ai.AiPromptConfig;
import com.taskmanagement.domain.ai.AiSubTaskProposal;
import com.taskmanagement.domain.ai.AiTaskBreakdown;
import com.taskmanagement.domain.ai.AiTaskSummary;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.project.repository.ProjectRepository;
import com.taskmanagement.domain.project.valueobject.ProjectId;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.exception.TaskValidationException;
import com.taskmanagement.domain.task.repository.TaskRepository;
import com.taskmanagement.domain.task.valueobject.Deadline;
import com.taskmanagement.domain.task.valueobject.TaskId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Use case interactor for AI-assisted task workflows.
 */
public class AiTaskAssistantControl implements AiTaskAssistantInputBoundary {
    private static final Logger logger = LoggerFactory.getLogger(AiTaskAssistantControl.class);

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final AiModelPort aiModelPort;
    private final DomainEventDispatcher eventDispatcher;
    private final AiPromptConfig defaultPromptConfig;

    public AiTaskAssistantControl(TaskRepository taskRepository,
                                  ProjectRepository projectRepository,
                                  AiModelPort aiModelPort,
                                  DomainEventDispatcher eventDispatcher) {
        this(taskRepository, projectRepository, aiModelPort, eventDispatcher, AiPromptConfig.defaultConfig());
    }

    public AiTaskAssistantControl(TaskRepository taskRepository,
                                  ProjectRepository projectRepository,
                                  AiModelPort aiModelPort,
                                  DomainEventDispatcher eventDispatcher,
                                  AiPromptConfig defaultPromptConfig) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "TaskRepository cannot be null");
        this.projectRepository = Objects.requireNonNull(projectRepository, "ProjectRepository cannot be null");
        this.aiModelPort = Objects.requireNonNull(aiModelPort, "AiModelPort cannot be null");
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher, "DomainEventDispatcher cannot be null");
        this.defaultPromptConfig = defaultPromptConfig != null ? defaultPromptConfig : AiPromptConfig.defaultConfig();
    }

    @Override
    public AiTaskBreakdownOutputDTO breakdownTask(UUID taskId, boolean apply, AiPromptOverrideInputDTO promptOverride) {
        Objects.requireNonNull(taskId, "Task id cannot be null");
        Task task = getTaskOrThrow(taskId);
        Project project = resolveProject(task.getProjectId().value());
        AiPromptConfig promptConfig = resolvePromptConfig(promptOverride);

        AiTaskBreakdown breakdown = aiModelPort.generateTaskBreakdown(task, project, promptConfig);

        boolean mutated = false;
        if (apply) {
            if (breakdown.subtasks() != null) {
                for (AiSubTaskProposal proposal : breakdown.subtasks()) {
                    boolean alreadyExists = task.getSubTasks().stream()
                        .anyMatch(existing -> existing.getTitle().equalsIgnoreCase(proposal.title()));
                    if (!alreadyExists) {
                        try {
                            task.addSubTask(proposal.title());
                            mutated = true;
                        } catch (TaskValidationException e) {
                            logger.warn("Skipping subtask '{}' for task {}: {}", proposal.title(), taskId, e.getMessage());
                        }
                    }
                }
            }
            if (breakdown.estimatedDeadline() != null) {
                try {
                    task.reschedule(Deadline.of(breakdown.estimatedDeadline()));
                    mutated = true;
                } catch (TaskValidationException e) {
                    logger.warn("Skipping deadline '{}' for task {}: {}", breakdown.estimatedDeadline(), taskId, e.getMessage());
                }
            }

            if (mutated) {
                taskRepository.save(task);
                eventDispatcher.dispatch(task.pollDomainEvents());
            }
        }

        List<AiSubTaskProposalOutputDTO> subtaskOutputs = breakdown.subtasks().stream()
            .map(p -> new AiSubTaskProposalOutputDTO(p.title(), p.explanation()))
            .toList();

        return new AiTaskBreakdownOutputDTO(
            task.getId().value(),
            subtaskOutputs,
            breakdown.estimatedDeadline(),
            breakdown.reasoning(),
            apply
        );
    }

    @Override
    public AiPriorityProposalOutputDTO prioritizeTask(UUID taskId, boolean apply, AiPromptOverrideInputDTO promptOverride) {
        Objects.requireNonNull(taskId, "Task id cannot be null");
        Task task = getTaskOrThrow(taskId);
        Project project = resolveProject(task.getProjectId().value());
        AiPromptConfig promptConfig = resolvePromptConfig(promptOverride);

        AiPriorityProposal proposal = aiModelPort.suggestPriority(task, project, promptConfig);

        if (apply && proposal.priority() != null) {
            if (task.getPriority() != proposal.priority()) {
                task.changePriority(proposal.priority());
                taskRepository.save(task);
                eventDispatcher.dispatch(task.pollDomainEvents());
            }
        }

        return new AiPriorityProposalOutputDTO(
            task.getId().value(),
            proposal.priority(),
            proposal.reasoning(),
            proposal.confidence(),
            apply
        );
    }

    @Override
    public AiSummaryOutputDTO summarizeTask(UUID taskId, AiPromptOverrideInputDTO promptOverride) {
        Objects.requireNonNull(taskId, "Task id cannot be null");
        Task task = getTaskOrThrow(taskId);
        Project project = resolveProject(task.getProjectId().value());
        AiPromptConfig promptConfig = resolvePromptConfig(promptOverride);

        AiTaskSummary summary = aiModelPort.generateTaskSummary(task, project, promptConfig);

        return new AiSummaryOutputDTO(
            task.getId().value(),
            task.getTitle().value(),
            summary.summary(),
            summary.keyHighlights(),
            summary.suggestedNextStep()
        );
    }

    @Override
    public void enrichTaskOnCreation(UUID taskId) {
        if (taskId == null) {
            return;
        }
        try {
            Task task = taskRepository.findById(TaskId.of(taskId)).orElse(null);
            if (task == null) {
                return;
            }
            logger.info("Executing auto-enrichment on task creation for task {}", taskId);
            breakdownTask(taskId, true, null);
            prioritizeTask(taskId, true, null);
        } catch (Exception e) {
            logger.warn("Auto-enrichment skipped or failed for taskId {}: {}", taskId, e.getMessage());
        }
    }

    private Task getTaskOrThrow(UUID taskId) {
        return taskRepository.findById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId));
    }

    private Project resolveProject(UUID projectId) {
        if (projectId == null) {
            return projectRepository.getInbox();
        }
        return projectRepository.findById(ProjectId.of(projectId))
            .orElseGet(projectRepository::getInbox);
    }

    private AiPromptConfig resolvePromptConfig(AiPromptOverrideInputDTO override) {
        if (override == null) {
            return defaultPromptConfig;
        }
        String sysPrompt = override.systemPrompt();
        String instructions = override.additionalInstructions();

        AiPromptConfig current = defaultPromptConfig;
        if (sysPrompt != null && !sysPrompt.isBlank()) {
            current = current.withSystemPrompt(sysPrompt);
        }
        if (instructions != null && !instructions.isBlank()) {
            current = current.withAdditionalInstructions(instructions);
        }
        if (override.hasCustomProvider()) {
            current = current.withCustomProvider(override.toCustomProviderConfig());
        } else if (override.provider() != null && !override.provider().isBlank()) {
            current = current.withProviderOverride(override.provider());
        }
        return current;
    }
}
