package com.taskmanagement.application.ai.dto;

import com.taskmanagement.domain.task.enums.Priority;

import java.util.UUID;

/**
 * Output DTO for AI priority proposals.
 */
public record AiPriorityProposalOutputDTO(
    UUID taskId,
    Priority suggestedPriority,
    String reasoning,
    Double confidence,
    boolean applied
) {}
