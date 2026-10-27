package com.taskmanagement.application.ai.dto;

import java.util.List;
import java.util.UUID;

/**
 * Output DTO for AI task summaries.
 */
public record AiSummaryOutputDTO(
    UUID taskId,
    String title,
    String summary,
    List<String> keyHighlights,
    String suggestedNextStep
) {}
