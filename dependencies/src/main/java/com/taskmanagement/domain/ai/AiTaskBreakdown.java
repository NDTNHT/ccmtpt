package com.taskmanagement.domain.ai;

import java.time.Instant;
import java.util.List;

/**
 * Task decomposition proposal containing subtasks, an estimated deadline, and reasoning.
 */
public record AiTaskBreakdown(
    List<AiSubTaskProposal> subtasks,
    Instant estimatedDeadline,
    String reasoning
) {
    public AiTaskBreakdown {
        subtasks = subtasks == null ? List.of() : List.copyOf(subtasks);
        reasoning = reasoning == null ? "" : reasoning.trim();
    }
}
