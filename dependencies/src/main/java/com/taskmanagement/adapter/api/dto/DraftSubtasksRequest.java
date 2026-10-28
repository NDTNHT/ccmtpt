package com.taskmanagement.adapter.api.dto;

import java.util.UUID;

public record DraftSubtasksRequest(
    String title,
    String description,
    UUID projectId,
    String customSystemPrompt
) {}
