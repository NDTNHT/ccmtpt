package com.taskmanagement.domain.service;

import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.project.enums.ProjectType;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.valueobject.Deadline;

import java.time.Duration;
import java.time.Instant;

/**
 * Calculates task priority from the task deadline and project context. Overloads accept
 * an explicit clock for deterministic behaviour.
 */
public class TaskPriorityCalculator {
    public static final Duration URGENT_WINDOW = Duration.ofDays(2);
    public static final Duration SOON_WINDOW = Duration.ofDays(7);

    public Priority calculatePriority(Task task, Project project) {
        return calculatePriority(task, project, Instant.now());
    }

    public Priority calculatePriority(Task task, Project project, Instant now) {
        if (task == null) {
            return Priority.LOW;
        }
        return calculatePriority(task.getDueDate(), task.getPriority(), project, now);
    }

    public Priority calculatePriority(Deadline dueDate, Priority userPriority, Project project) {
        return calculatePriority(dueDate, userPriority, project, Instant.now());
    }

    public Priority calculatePriority(Deadline dueDate,
                                      Priority userPriority,
                                      Project project,
                                      Instant now) {
        Instant reference = now != null ? now : Instant.now();
        boolean urgent = dueDate != null
            && (dueDate.isOverdue(reference) || dueDate.isApproaching(reference, URGENT_WINDOW));

        // An unclassified INBOX task is never escalated to HIGH; it is capped at MEDIUM
        // so the user is reminded to classify it.
        if (project != null && project.getProjectType() == ProjectType.INBOX) {
            return urgent ? Priority.MEDIUM : effective(userPriority);
        }

        // A regular project escalates overdue / two-day tasks to HIGH.
        if (urgent) {
            return Priority.HIGH;
        }

        // Tasks due within a week are at least MEDIUM.
        if (dueDate != null && dueDate.isApproaching(reference, SOON_WINDOW)) {
            return Priority.MEDIUM;
        }

        return effective(userPriority);
    }

    private static Priority effective(Priority priority) {
        return priority != null ? priority : Priority.LOW;
    }
}
