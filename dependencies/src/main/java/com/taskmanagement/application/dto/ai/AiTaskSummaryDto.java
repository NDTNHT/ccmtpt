package com.taskmanagement.application.dto.ai;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Result DTO containing AI-generated summary of a task, progress percentage and highlights.
 */
public record AiTaskSummaryDto(
    UUID taskId,
    String title,
    String summary,
    double completionRate,
    List<String> keyHighlights
) {
    public AiTaskSummaryDto {
        Objects.requireNonNull(taskId, "taskId cannot be null");
        title = title == null ? "" : title;
        summary = summary == null ? "" : summary;
        keyHighlights = keyHighlights == null ? List.of() : List.copyOf(keyHighlights);
    }
}
