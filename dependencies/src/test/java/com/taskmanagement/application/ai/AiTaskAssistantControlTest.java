package com.taskmanagement.application.ai;

import com.taskmanagement.adapter.persistence.IOSaveToMemory;
import com.taskmanagement.adapter.persistence.IOSaveToMemoryProject;
import com.taskmanagement.application.ai.dto.AiPriorityProposalOutputDTO;
import com.taskmanagement.application.ai.dto.AiPromptOverrideInputDTO;
import com.taskmanagement.application.ai.dto.AiSummaryOutputDTO;
import com.taskmanagement.application.ai.dto.AiTaskBreakdownOutputDTO;
import com.taskmanagement.application.exception.TaskNotFoundException;
import com.taskmanagement.domain.ai.AiModelPort;
import com.taskmanagement.domain.ai.AiPriorityProposal;
import com.taskmanagement.domain.ai.AiPromptConfig;
import com.taskmanagement.domain.ai.AiSubTaskProposal;
import com.taskmanagement.domain.ai.AiTaskBreakdown;
import com.taskmanagement.domain.ai.AiTaskSummary;
import com.taskmanagement.domain.common.event.DomainEvent;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.project.repository.ProjectRepository;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.event.TaskPriorityChangedEvent;
import com.taskmanagement.domain.task.event.TaskRescheduledEvent;
import com.taskmanagement.domain.task.repository.TaskRepository;
import com.taskmanagement.domain.task.valueobject.TaskId;
import com.taskmanagement.infrastructure.event.InMemoryDomainEventDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AiTaskAssistantControl Unit Tests")
class AiTaskAssistantControlTest {

    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private AiModelPort aiModelPort;
    private InMemoryDomainEventDispatcher dispatcher;
    private List<DomainEvent> publishedEvents;
    private AiTaskAssistantControl assistantControl;

    private Project testProject;
    private Task testTask;

    @BeforeEach
    void setUp() {
        taskRepository = new IOSaveToMemory();
        projectRepository = new IOSaveToMemoryProject();
        aiModelPort = mock(AiModelPort.class);
        dispatcher = new InMemoryDomainEventDispatcher();
        publishedEvents = new ArrayList<>();
        dispatcher.subscribe(publishedEvents::add);

        assistantControl = new AiTaskAssistantControl(
            taskRepository,
            projectRepository,
            aiModelPort,
            dispatcher
        );

        testProject = projectRepository.getInbox();
        testTask = Task.create("API Integration", "Integrate third-party API", null, Priority.LOW, testProject.getId());
        taskRepository.save(testTask);
        publishedEvents.clear(); // Clear creation event
    }

    @Test
    @DisplayName("breakdownTask with apply=false previews proposals without modifying task aggregate")
    void shouldPreviewBreakdownWithoutMutating() {
        Instant expectedDeadline = Instant.now().plusSeconds(86400 * 3);
        AiTaskBreakdown mockBreakdown = new AiTaskBreakdown(
            List.of(
                new AiSubTaskProposal("Read API specification", "Study endpoints"),
                new AiSubTaskProposal("Create client wrapper", "HTTP calls")
            ),
            expectedDeadline,
            "Decomposed into 2 phases"
        );
        when(aiModelPort.generateTaskBreakdown(any(), any(), any())).thenReturn(mockBreakdown);

        AiTaskBreakdownOutputDTO result = assistantControl.breakdownTask(testTask.getId().value(), false, null);

        assertNotNull(result);
        assertEquals(2, result.subtasks().size());
        assertEquals("Read API specification", result.subtasks().get(0).title());
        assertFalse(result.applied());

        // Verify task in repository was NOT changed
        Task reloaded = taskRepository.findById(testTask.getId()).orElseThrow();
        assertTrue(reloaded.getSubTasks().isEmpty());
        assertNull(reloaded.getDueDate());
        assertTrue(publishedEvents.isEmpty());
    }

    @Test
    @DisplayName("breakdownTask with apply=true persists subtasks and deadline, emitting domain events")
    void shouldApplyBreakdownToTask() {
        Instant expectedDeadline = Instant.now().plusSeconds(86400 * 3);
        AiTaskBreakdown mockBreakdown = new AiTaskBreakdown(
            List.of(
                new AiSubTaskProposal("Read API specification", "Study endpoints"),
                new AiSubTaskProposal("Create client wrapper", "HTTP calls")
            ),
            expectedDeadline,
            "Decomposed into 2 phases"
        );
        when(aiModelPort.generateTaskBreakdown(any(), any(), any())).thenReturn(mockBreakdown);

        AiTaskBreakdownOutputDTO result = assistantControl.breakdownTask(testTask.getId().value(), true, null);

        assertTrue(result.applied());

        // Verify task in repository was updated
        Task reloaded = taskRepository.findById(testTask.getId()).orElseThrow();
        assertEquals(2, reloaded.getSubTasks().size());
        assertEquals("Read API specification", reloaded.getSubTasks().get(0).getTitle());
        assertEquals(expectedDeadline, reloaded.getDueDate().dueDate());

        // Verify rescheduling event was dispatched
        assertTrue(publishedEvents.stream().anyMatch(e -> e instanceof TaskRescheduledEvent));
    }

    @Test
    @DisplayName("prioritizeTask with apply=false previews priority suggestion without saving")
    void shouldPreviewPriorityWithoutMutating() {
        AiPriorityProposal mockProposal = new AiPriorityProposal(Priority.HIGH, "Urgent dependency", 0.9);
        when(aiModelPort.suggestPriority(any(), any(), any())).thenReturn(mockProposal);

        AiPriorityProposalOutputDTO result = assistantControl.prioritizeTask(testTask.getId().value(), false, null);

        assertNotNull(result);
        assertEquals(Priority.HIGH, result.suggestedPriority());
        assertFalse(result.applied());

        Task reloaded = taskRepository.findById(testTask.getId()).orElseThrow();
        assertEquals(Priority.LOW, reloaded.getPriority());
        assertTrue(publishedEvents.isEmpty());
    }

    @Test
    @DisplayName("prioritizeTask with apply=true updates priority in task and dispatches event")
    void shouldApplyPriorityToTask() {
        AiPriorityProposal mockProposal = new AiPriorityProposal(Priority.HIGH, "Urgent dependency", 0.9);
        when(aiModelPort.suggestPriority(any(), any(), any())).thenReturn(mockProposal);

        AiPriorityProposalOutputDTO result = assistantControl.prioritizeTask(testTask.getId().value(), true, null);

        assertTrue(result.applied());

        Task reloaded = taskRepository.findById(testTask.getId()).orElseThrow();
        assertEquals(Priority.HIGH, reloaded.getPriority());

        assertTrue(publishedEvents.stream().anyMatch(e -> e instanceof TaskPriorityChangedEvent));
    }

    @Test
    @DisplayName("summarizeTask returns structured summary DTO")
    void shouldGenerateTaskSummary() {
        AiTaskSummary mockSummary = new AiTaskSummary(
            "API Integration task summary",
            List.of("Point 1", "Point 2"),
            "Start reviewing docs"
        );
        when(aiModelPort.generateTaskSummary(any(), any(), any())).thenReturn(mockSummary);

        AiSummaryOutputDTO result = assistantControl.summarizeTask(testTask.getId().value(), null);

        assertNotNull(result);
        assertEquals("API Integration task summary", result.summary());
        assertEquals(2, result.keyHighlights().size());
        assertEquals("Start reviewing docs", result.suggestedNextStep());
    }

    @Test
    @DisplayName("Throws TaskNotFoundException when task id is unknown")
    void shouldThrowWhenTaskNotFound() {
        UUID unknownId = UUID.randomUUID();
        assertThrows(TaskNotFoundException.class, () -> assistantControl.breakdownTask(unknownId, false, null));
        assertThrows(TaskNotFoundException.class, () -> assistantControl.prioritizeTask(unknownId, false, null));
        assertThrows(TaskNotFoundException.class, () -> assistantControl.summarizeTask(unknownId, null));
    }

    @Test
    @DisplayName("enrichTaskOnCreation performs auto-enrichment on task")
    void shouldEnrichTaskOnCreation() {
        Instant deadline = Instant.now().plusSeconds(86400 * 2);
        AiTaskBreakdown mockBreakdown = new AiTaskBreakdown(
            List.of(new AiSubTaskProposal("Auto subtask 1")),
            deadline,
            "Auto breakdown"
        );
        AiPriorityProposal mockPriority = new AiPriorityProposal(Priority.HIGH, "Auto priority", 0.88);

        when(aiModelPort.generateTaskBreakdown(any(), any(), any())).thenReturn(mockBreakdown);
        when(aiModelPort.suggestPriority(any(), any(), any())).thenReturn(mockPriority);

        assistantControl.enrichTaskOnCreation(testTask.getId().value());

        Task enriched = taskRepository.findById(testTask.getId()).orElseThrow();
        assertEquals(1, enriched.getSubTasks().size());
        assertEquals("Auto subtask 1", enriched.getSubTasks().get(0).getTitle());
        assertEquals(Priority.HIGH, enriched.getPriority());
    }
}
