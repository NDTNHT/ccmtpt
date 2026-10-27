package com.taskmanagement.application.ai;

import com.taskmanagement.application.ai.dto.AiPriorityProposalOutputDTO;
import com.taskmanagement.application.ai.dto.AiPromptOverrideInputDTO;
import com.taskmanagement.application.ai.dto.AiSummaryOutputDTO;
import com.taskmanagement.application.ai.dto.AiTaskBreakdownOutputDTO;

import java.util.UUID;

/**
 * Inbound port for AI Task Assistant use cases.
 */
public interface AiTaskAssistantInputBoundary {
    /**
     * Decomposes a task into subtasks and estimates completion deadline.
     * When apply is true, the generated subtasks and deadline are saved to the task aggregate.
     */
    AiTaskBreakdownOutputDTO breakdownTask(UUID taskId, boolean apply, AiPromptOverrideInputDTO promptOverride);

    /**
     * Analyzes task attributes and project context to suggest optimal priority.
     * When apply is true, updates the task priority.
     */
    AiPriorityProposalOutputDTO prioritizeTask(UUID taskId, boolean apply, AiPromptOverrideInputDTO promptOverride);

    /**
     * Generates a concise summary, key highlights, and recommended next step for the task.
     */
    AiSummaryOutputDTO summarizeTask(UUID taskId, AiPromptOverrideInputDTO promptOverride);

    /**
     * Event-driven auto-enrichment hook executed on task creation.
     */
    void enrichTaskOnCreation(UUID taskId);
}
