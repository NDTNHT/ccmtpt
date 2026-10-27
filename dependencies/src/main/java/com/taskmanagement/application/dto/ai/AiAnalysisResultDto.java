package com.taskmanagement.application.dto.ai;

import java.util.Objects;
import java.util.UUID;

/**
 * Composite DTO containing the full suite of AI analyses: decomposition, estimation,
 * priority recommendation and progress summary.
 */
public record AiAnalysisResultDto(
    UUID taskId,
    AiTaskDecompositionDto decomposition,
    AiDeadlineEstimationDto deadlineEstimation,
    AiPriorityRecommendationDto priorityRecommendation,
    AiTaskSummaryDto summary
) {
    public AiAnalysisResultDto {
        Objects.requireNonNull(taskId, "taskId cannot be null");
    }
}
