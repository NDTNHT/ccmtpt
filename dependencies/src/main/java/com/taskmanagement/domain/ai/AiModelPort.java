package com.taskmanagement.domain.ai;

import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.task.entity.Task;

/**
 * Outbound port for AI LLM providers.
 * Follows Clean Architecture Dependency Inversion Principle.
 */
public interface AiModelPort {
    /**
     * Decomposes a task into actionable subtasks and estimates a completion deadline.
     */
    AiTaskBreakdown generateTaskBreakdown(Task task, Project project, AiPromptConfig promptConfig);

    /**
     * Evaluates task context to suggest an optimal priority with reasoning.
     */
    AiPriorityProposal suggestPriority(Task task, Project project, AiPromptConfig promptConfig);

    /**
     * Generates a concise summary, key highlights, and recommended next step.
     */
    AiTaskSummary generateTaskSummary(Task task, Project project, AiPromptConfig promptConfig);
}
