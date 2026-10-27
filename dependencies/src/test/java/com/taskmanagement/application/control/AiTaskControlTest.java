package com.taskmanagement.application.control;

import com.taskmanagement.adapter.ai.SimulatedAiAdapter;
import com.taskmanagement.adapter.persistence.IOSaveToMemory;
import com.taskmanagement.adapter.persistence.IOSaveToMemoryProject;
import com.taskmanagement.application.dto.ai.AiAnalysisResultDto;
import com.taskmanagement.application.dto.ai.AiDeadlineEstimationDto;
import com.taskmanagement.application.dto.ai.AiPriorityRecommendationDto;
import com.taskmanagement.application.dto.ai.AiTaskDecompositionDto;
import com.taskmanagement.application.dto.ai.AiTaskSummaryDto;
import com.taskmanagement.application.exception.TaskNotFoundException;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.project.repository.ProjectRepository;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.repository.TaskRepository;
import com.taskmanagement.domain.task.valueobject.Deadline;
import com.taskmanagement.domain.task.valueobject.TaskTitle;
import com.taskmanagement.infrastructure.event.InMemoryDomainEventDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("AiTaskControl Application Service")
class AiTaskControlTest {
    private AiTaskControl aiControl;
    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private InMemoryDomainEventDispatcher dispatcher;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        taskRepository = new IOSaveToMemory();
        projectRepository = new IOSaveToMemoryProject();
        dispatcher = new InMemoryDomainEventDispatcher();

        project = Project.create("Sprint Backlog", "Sprint 1 tasks");
        projectRepository.save(project);

        task = Task.create(
            new TaskTitle("Refactor Payment Service"),
            "Decouple payment gateway and add idempotency check",
            Deadline.of(Instant.now().plus(Duration.ofDays(3))),
            Priority.MEDIUM,
            project.getId()
        );
        taskRepository.save(task);

        aiControl = new AiTaskControl(taskRepository, projectRepository, new SimulatedAiAdapter(), dispatcher);
    }

    @Test
    @DisplayName("Decompose task should return subtasks without modifying entity until applied")
    void testDecomposeTask() {
        AiTaskDecompositionDto result = aiControl.decomposeTask(task.getId().value(), null);

        assertNotNull(result);
        assertEquals(task.getId().value(), result.taskId());
        assertFalse(result.subTasks().isEmpty());

        // Verify task in repository still has 0 subtasks before apply
        Task persisted = taskRepository.findById(task.getId()).orElseThrow();
        assertEquals(0, persisted.getSubTasks().size());
    }

    @Test
    @DisplayName("Apply decomposition should persist subtasks into task aggregate root")
    void testApplyDecomposition() {
        List<String> subTasks = List.of(
            "Draft interface contract",
            "Write unit tests",
            "Implement gateway adapter"
        );

        aiControl.applyDecomposition(task.getId().value(), subTasks);

        Task persisted = taskRepository.findById(task.getId()).orElseThrow();
        assertEquals(3, persisted.getSubTasks().size());
        assertEquals("Draft interface contract", persisted.getSubTasks().get(0).getTitle());
    }

    @Test
    @DisplayName("Apply estimated deadline should reschedule task and persist changes")
    void testApplyEstimatedDeadline() {
        Instant newDeadline = Instant.now().plus(Duration.ofDays(5));

        aiControl.applyEstimatedDeadline(task.getId().value(), newDeadline);

        Task persisted = taskRepository.findById(task.getId()).orElseThrow();
        assertEquals(newDeadline, persisted.getDueDate().dueDate());
    }

    @Test
    @DisplayName("Apply recommended priority should change priority and persist changes")
    void testApplyRecommendedPriority() {
        aiControl.applyRecommendedPriority(task.getId().value(), Priority.HIGH);

        Task persisted = taskRepository.findById(task.getId()).orElseThrow();
        assertEquals(Priority.HIGH, persisted.getPriority());
    }

    @Test
    @DisplayName("Analyze priority should evaluate semantic and domain rules")
    void testAnalyzePriority() {
        AiPriorityRecommendationDto result = aiControl.analyzePriority(task.getId().value(), null);

        assertNotNull(result);
        assertEquals(task.getId().value(), result.taskId());
        assertNotNull(result.effectivePriority());
    }

    @Test
    @DisplayName("Generate summary should analyze task progress")
    void testGenerateSummary() {
        aiControl.applyDecomposition(task.getId().value(), List.of("Subtask 1", "Subtask 2"));
        Task reloaded = taskRepository.findById(task.getId()).orElseThrow();
        reloaded.completeSubTask(reloaded.getSubTasks().get(0).getId());
        taskRepository.save(reloaded);

        AiTaskSummaryDto summary = aiControl.generateSummary(task.getId().value(), null);

        assertNotNull(summary);
        assertEquals(50.0, summary.completionRate());
    }

    @Test
    @DisplayName("Analyze task comprehensive returns all components")
    void testAnalyzeTaskComprehensive() {
        AiAnalysisResultDto result = aiControl.analyzeTask(task.getId().value(), null);

        assertNotNull(result);
        assertNotNull(result.decomposition());
        assertNotNull(result.deadlineEstimation());
        assertNotNull(result.priorityRecommendation());
        assertNotNull(result.summary());
    }

    @Test
    @DisplayName("Throw TaskNotFoundException when task id does not exist")
    void testTaskNotFound() {
        UUID nonExistent = UUID.randomUUID();
        assertThrows(TaskNotFoundException.class, () -> aiControl.decomposeTask(nonExistent, null));
    }
}
