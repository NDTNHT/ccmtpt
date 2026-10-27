package com.taskmanagement.domain.ai;

import com.taskmanagement.domain.task.enums.Priority;

import java.util.Objects;

/**
 * AI-suggested task priority with reasoning and confidence score.
 */
public record AiPriorityProposal(
    Priority priority,
    String reasoning,
    Double confidence
) {
    public AiPriorityProposal {
        Objects.requireNonNull(priority, "Priority cannot be null");
        reasoning = reasoning == null ? "" : reasoning.trim();
        if (confidence == null) {
            confidence = 1.0;
        } else {
            confidence = Math.max(0.0, Math.min(1.0, confidence));
        }
    }

    public AiPriorityProposal(Priority priority, String reasoning) {
        this(priority, reasoning, 1.0);
    }
}
