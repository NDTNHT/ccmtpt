package com.taskmanagement.infrastructure.listener;

import com.taskmanagement.domain.common.event.DomainEvent;
import com.taskmanagement.domain.task.event.TaskCancelledEvent;
import com.taskmanagement.domain.task.event.TaskCompletedEvent;
import com.taskmanagement.domain.task.event.TaskCreatedEvent;
import com.taskmanagement.domain.task.event.TaskPriorityChangedEvent;
import com.taskmanagement.domain.task.event.TaskRescheduledEvent;
import com.taskmanagement.domain.task.event.TaskStartedEvent;
import com.taskmanagement.infrastructure.event.InMemoryDomainEventDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes task domain events to the activity log.
 */
public final class TaskActivityLogger {
    private static final Logger logger = LoggerFactory.getLogger(TaskActivityLogger.class);

    private final InMemoryDomainEventDispatcher dispatcher;

    public TaskActivityLogger(InMemoryDomainEventDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        dispatcher.subscribe(this::handleEvent);
    }

    private void handleEvent(DomainEvent event) {
        if (event instanceof TaskCreatedEvent created) {
            logger.info("Task created: taskId={}, projectId={}, title={}",
                created.taskId(), created.projectId(), created.title().value());
        } else if (event instanceof TaskStartedEvent started) {
            logger.info("Task started: taskId={}", started.taskId());
        } else if (event instanceof TaskCompletedEvent completed) {
            logger.info("Task completed: taskId={}", completed.taskId());
        } else if (event instanceof TaskCancelledEvent cancelled) {
            logger.info("Task cancelled: taskId={}", cancelled.taskId());
        } else if (event instanceof TaskRescheduledEvent rescheduled) {
            logger.info("Task rescheduled: taskId={}, from={}, to={}",
                rescheduled.taskId(), rescheduled.previousDeadline(), rescheduled.newDeadline());
        } else if (event instanceof TaskPriorityChangedEvent changed) {
            logger.info("Task priority changed: taskId={}, from={}, to={}",
                changed.taskId(), changed.previousPriority(), changed.newPriority());
        }
    }
}
