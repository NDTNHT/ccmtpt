package com.taskmanagement.domain.task.valueobject;

import com.taskmanagement.domain.task.exception.TaskValidationException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record Deadline(Instant dueDate) {
    public Deadline {
        if (dueDate == null) {
            throw new TaskValidationException("Due date cannot be null.");
        }
    }

    // Factory methods
    public static Deadline of(Instant dueDate) {
        return new Deadline(dueDate);
    }

    public static Deadline inDays(long days) {
        return new Deadline(Instant.now().plus(days, ChronoUnit.DAYS));
    }

    public boolean isOverdue() {
        return isOverdue(Instant.now());
    }

    public boolean isOverdue(Instant currentInstant) {
        return currentInstant.isAfter(dueDate);
    }

    public Duration timeRemaining() {
        return timeRemaining(Instant.now());
    }

    public Duration timeRemaining(Instant currentInstant) {
        return Duration.between(currentInstant, dueDate);
    }

    public boolean isApproaching(Duration threshold) {
        return isApproaching(Instant.now(), threshold);
    }

    public boolean isApproaching(Instant currentInstant, Duration threshold) {
        Duration remaining = timeRemaining(currentInstant);
        return !remaining.isNegative() && remaining.compareTo(threshold) <= 0;
    }
}
