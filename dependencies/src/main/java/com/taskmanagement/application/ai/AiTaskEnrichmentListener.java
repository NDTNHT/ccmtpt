package com.taskmanagement.application.ai;

import com.taskmanagement.domain.common.event.DomainEvent;
import com.taskmanagement.domain.task.event.TaskCreatedEvent;
import com.taskmanagement.infrastructure.event.InMemoryDomainEventDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Event-driven listener that triggers AI auto-enrichment upon TaskCreatedEvent.
 */
public final class AiTaskEnrichmentListener {
    private static final Logger logger = LoggerFactory.getLogger(AiTaskEnrichmentListener.class);

    private final InMemoryDomainEventDispatcher dispatcher;
    private final AiTaskAssistantInputBoundary aiAssistant;
    private final boolean autoEnrichEnabled;
    private final ExecutorService executor;

    public AiTaskEnrichmentListener(InMemoryDomainEventDispatcher dispatcher,
                                    AiTaskAssistantInputBoundary aiAssistant,
                                    boolean autoEnrichEnabled) {
        this(dispatcher, aiAssistant, autoEnrichEnabled, null);
    }

    public AiTaskEnrichmentListener(InMemoryDomainEventDispatcher dispatcher,
                                    AiTaskAssistantInputBoundary aiAssistant,
                                    boolean autoEnrichEnabled,
                                    ExecutorService executor) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "Dispatcher cannot be null");
        this.aiAssistant = Objects.requireNonNull(aiAssistant, "AiAssistant cannot be null");
        this.autoEnrichEnabled = autoEnrichEnabled;
        this.executor = executor;
        dispatcher.subscribe(this::handleEvent);
    }

    public void handleEvent(DomainEvent event) {
        if (!autoEnrichEnabled) {
            return;
        }
        if (event instanceof TaskCreatedEvent created) {
            UUID taskId = created.taskId().value();
            if (executor != null && !executor.isShutdown()) {
                executor.submit(() -> runEnrichment(taskId));
            } else {
                runEnrichment(taskId);
            }
        }
    }

    private void runEnrichment(UUID taskId) {
        try {
            logger.info("Starting background AI auto-enrichment for task {}", taskId);
            aiAssistant.enrichTaskOnCreation(taskId);
        } catch (Exception e) {
            logger.warn("AI auto-enrichment failed for task {}: {}", taskId, e.getMessage());
        }
    }

    public boolean isAutoEnrichEnabled() {
        return autoEnrichEnabled;
    }
}
