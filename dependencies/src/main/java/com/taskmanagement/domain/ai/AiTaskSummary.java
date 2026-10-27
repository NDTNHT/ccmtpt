package com.taskmanagement.domain.ai;

import java.util.List;

/**
 * AI-generated summary of a task, including highlights and recommended next step.
 */
public record AiTaskSummary(
    String summary,
    List<String> keyHighlights,
    String suggestedNextStep
) {
    public AiTaskSummary {
        summary = summary == null ? "" : summary.trim();
        keyHighlights = keyHighlights == null ? List.of() : List.copyOf(keyHighlights);
        suggestedNextStep = suggestedNextStep == null ? "" : suggestedNextStep.trim();
    }
}
