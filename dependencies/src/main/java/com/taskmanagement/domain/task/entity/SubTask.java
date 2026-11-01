package com.taskmanagement.domain.task.entity;

import com.taskmanagement.domain.common.entity.Entity;
import com.taskmanagement.domain.task.exception.TaskValidationException;

import java.util.Objects;
import java.util.UUID;

/**
 * Child entity of Task. Persisted and loaded through its parent task.
 */
public class SubTask extends Entity<UUID> {
    private String title;
    private boolean completed;

    public SubTask(UUID id, String title, boolean completed) {
        super(id != null ? id : UUID.randomUUID());
        this.title = validateTitle(title);
        this.completed = completed;
    }

    public static SubTask create(String title) {
        return new SubTask(UUID.randomUUID(), title, false);
    }

    /**
     * Rehydration factory used by persistence adapters. Does not raise events.
     */
    public static SubTask reconstitute(UUID id, String title, boolean completed) {
        return new SubTask(Objects.requireNonNull(id, "SubTask id cannot be null"), title, completed);
    }

    public void complete() {
        this.completed = true;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void rename(String newTitle) {
        this.title = validateTitle(newTitle);
    }

    private static String validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new TaskValidationException("SubTask title cannot be empty.");
        }
        String trimmed = title.trim();
        if (trimmed.length() > 200) {
            throw new TaskValidationException("SubTask title cannot exceed 200 characters.");
        }
        return trimmed;
    }

    public String getTitle() {
        return title;
    }

    public boolean isCompleted() {
        return completed;
    }
}
