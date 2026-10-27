package com.taskmanagement.application.ai.dto;

/**
 * Output DTO for an AI-suggested subtask.
 */
public record AiSubTaskProposalOutputDTO(
    String title,
    String explanation
) {}
