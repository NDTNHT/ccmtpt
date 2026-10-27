package com.taskmanagement.application.ai.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Output DTO for task breakdown proposals.
 */
public record AiTaskBreakdownOutputDTO(
    UUID taskId,
    List<AiSubTaskProposalOutputDTO> subtasks,
    Instant estimatedDeadline,
    String reasoning,
    boolean applied
) {}
