package com.taskmanagement.application.dto.ai;

import com.taskmanagement.domain.task.enums.Priority;

import java.util.Objects;
import java.util.UUID;

/**
 * Result DTO containing AI semantic priority proposal, Domain Service calculated priority,
 * and the reconciled effective priority.
 */
public record AiPriorityRecommendationDto(
    UUID taskId,
    Priority aiProposedPriority,
    Priority domainCalculatedPriority,
    Priority effectivePriority,
    String reasoning
) {
    public AiPriorityRecommendationDto {
        Objects.requireNonNull(taskId, "taskId cannot be null");
        aiProposedPriority = aiProposedPriority == null ? Priority.LOW : aiProposedPriority;
        domainCalculatedPriority = domainCalculatedPriority == null ? Priority.LOW : domainCalculatedPriority;
        effectivePriority = effectivePriority == null ? domainCalculatedPriority : effectivePriority;
        reasoning = reasoning == null ? "" : reasoning;
    }
}
