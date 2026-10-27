package com.taskmanagement.application.ai;

import com.taskmanagement.domain.project.valueobject.ProjectId;
import com.taskmanagement.domain.task.event.TaskCompletedEvent;
import com.taskmanagement.domain.task.event.TaskCreatedEvent;
import com.taskmanagement.domain.task.valueobject.TaskId;
import com.taskmanagement.domain.task.valueobject.TaskTitle;
import com.taskmanagement.infrastructure.event.InMemoryDomainEventDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("AiTaskEnrichmentListener Unit Tests")
class AiTaskEnrichmentListenerTest {

    private InMemoryDomainEventDispatcher dispatcher;
    private AiTaskAssistantInputBoundary aiAssistant;

    @BeforeEach
    void setUp() {
        dispatcher = new InMemoryDomainEventDispatcher();
        aiAssistant = mock(AiTaskAssistantInputBoundary.class);
    }

    @Test
    @DisplayName("Invokes auto-enrichment when enabled and TaskCreatedEvent is dispatched")
    void shouldEnrichWhenEnabled() {
        new AiTaskEnrichmentListener(dispatcher, aiAssistant, true, null);

        UUID taskId = UUID.randomUUID();
        TaskCreatedEvent event = new TaskCreatedEvent(
            TaskId.of(taskId),
            new TaskTitle("New Task"),
            ProjectId.of(UUID.randomUUID()),
            Instant.now()
        );

        dispatcher.dispatch(List.of(event));

        verify(aiAssistant).enrichTaskOnCreation(taskId);
    }

    @Test
    @DisplayName("Does not invoke auto-enrichment when disabled")
    void shouldNotEnrichWhenDisabled() {
        new AiTaskEnrichmentListener(dispatcher, aiAssistant, false, null);

        UUID taskId = UUID.randomUUID();
        TaskCreatedEvent event = new TaskCreatedEvent(
            TaskId.of(taskId),
            new TaskTitle("New Task"),
            ProjectId.of(UUID.randomUUID()),
            Instant.now()
        );

        dispatcher.dispatch(List.of(event));

        verify(aiAssistant, never()).enrichTaskOnCreation(taskId);
    }

    @Test
    @DisplayName("Ignores other domain event types")
    void shouldIgnoreOtherEvents() {
        new AiTaskEnrichmentListener(dispatcher, aiAssistant, true, null);

        UUID taskId = UUID.randomUUID();
        TaskCompletedEvent completedEvent = new TaskCompletedEvent(TaskId.of(taskId), Instant.now());

        dispatcher.dispatch(List.of(completedEvent));

        verify(aiAssistant, never()).enrichTaskOnCreation(taskId);
    }
}
