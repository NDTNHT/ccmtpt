package com.taskmanagement.adapter.ai;

import com.taskmanagement.application.dto.ai.AiAnalysisResultDto;
import com.taskmanagement.application.dto.ai.AiDeadlineEstimationDto;
import com.taskmanagement.application.dto.ai.AiPriorityRecommendationDto;
import com.taskmanagement.application.dto.ai.AiTaskDecompositionDto;
import com.taskmanagement.application.dto.ai.AiTaskSummaryDto;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.project.enums.ProjectType;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.valueobject.Deadline;
import com.taskmanagement.domain.task.valueobject.TaskTitle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulatedAiAdapterTest {

    private SimulatedAiAdapter adapter;
    private Project regularProject;
    private Project inboxProject;

    @BeforeEach
    void setUp() {
        adapter = new SimulatedAiAdapter();
        regularProject = Project.create("Backend Core", "Core services");
        inboxProject = Project.createInbox();
    }

    @Test
    @DisplayName("Decompose task should extract steps for refactoring task")
    void testDecomposeRefactorTask() {
        Task task = Task.create(
            new TaskTitle("Refactor User Module"),
            "Need to decouple database logic",
            Deadline.of(Instant.now().plus(Duration.ofDays(3))),
            Priority.MEDIUM,
            regularProject.getId()
        );

        AiTaskDecompositionDto result = adapter.decomposeTask(task, regularProject, null);

        assertNotNull(result);
        assertEquals(task.getId().value(), result.taskId());
        assertFalse(result.subTasks().isEmpty());
        assertTrue(result.subTasks().stream().anyMatch(s -> s.toLowerCase().contains("refactor") || s.toLowerCase().contains("clean architecture")));
    }

    @Test
    @DisplayName("Suggest subtasks should identify missing tests and docs")
    void testSuggestSubTasks() {
        Task task = Task.create(
            new TaskTitle("Build Payment Gateway"),
            "Implement PayPal integration",
            Deadline.of(Instant.now().plus(Duration.ofDays(2))),
            Priority.HIGH,
            regularProject.getId()
        );
        task.addSubTask("Setup PayPal credentials");

        AiTaskDecompositionDto result = adapter.suggestSubTasks(task, regularProject, null);

        assertNotNull(result);
        assertTrue(result.subTasks().stream().anyMatch(s -> s.toLowerCase().contains("test")));
        assertTrue(result.subTasks().stream().anyMatch(s -> s.toLowerCase().contains("doc")));
    }

    @Test
    @DisplayName("Estimate deadline should calculate hours based on subtasks and complexity")
    void testEstimateDeadline() {
        Task task = Task.create(
            new TaskTitle("Complex Architecture Redesign"),
            "Full redesign of messaging infrastructure",
            Deadline.of(Instant.now().plus(Duration.ofDays(5))),
            Priority.MEDIUM,
            regularProject.getId()
        );
        task.addSubTask("Step 1: Kafka broker setup");
        task.addSubTask("Step 2: Consumer group rebalance");
        task.addSubTask("Step 3: Dead letter queue handling");

        AiDeadlineEstimationDto result = adapter.estimateDeadline(task, regularProject, null);

        assertNotNull(result);
        assertTrue(result.estimatedHours() >= 24);
        assertNotNull(result.recommendedDeadline());
        assertEquals("HIGH", result.confidence());
    }

    @Test
    @DisplayName("Analyze priority should recognize critical blocker and cap on inbox project")
    void testAnalyzePriorityWithInboxCap() {
        Task task = Task.create(
            new TaskTitle("Critical crash on payment page"),
            "Emergency fix required for checkout crash",
            Deadline.of(Instant.now().plus(Duration.ofDays(1))),
            Priority.LOW,
            inboxProject.getId()
        );

        AiPriorityRecommendationDto result = adapter.analyzePriority(task, inboxProject, Priority.LOW, null);

        assertNotNull(result);
        assertEquals(Priority.HIGH, result.aiProposedPriority());
        assertEquals(Priority.MEDIUM, result.effectivePriority());
        assertTrue(result.reasoning().contains("Inbox project"));
    }

    @Test
    @DisplayName("Generate summary should calculate completion rate accurately")
    void testGenerateSummary() {
        Task task = Task.create(
            new TaskTitle("Ship Release 1.0"),
            "Final deployment checklist",
            Deadline.of(Instant.now().plus(Duration.ofDays(2))),
            Priority.HIGH,
            regularProject.getId()
        );
        var sub1 = task.addSubTask("Smoke testing");
        task.addSubTask("Update release notes");
        task.completeSubTask(sub1.getId());

        AiTaskSummaryDto summary = adapter.generateSummary(task, regularProject, null);

        assertNotNull(summary);
        assertEquals(50.0, summary.completionRate());
        assertTrue(summary.summary().contains("50.0%"));
        assertFalse(summary.keyHighlights().isEmpty());
    }

    @Test
    @DisplayName("Comprehensive analysis should bundle all AI components")
    void testAnalyzeTaskComprehensive() {
        Task task = Task.create(
            new TaskTitle("Build Search API"),
            "Develop elasticsearch indexing and search endpoint",
            Deadline.of(Instant.now().plus(Duration.ofDays(4))),
            Priority.MEDIUM,
            regularProject.getId()
        );

        AiAnalysisResultDto result = adapter.analyzeTaskComprehensive(task, regularProject, Priority.MEDIUM, null);

        assertNotNull(result);
        assertNotNull(result.decomposition());
        assertNotNull(result.deadlineEstimation());
        assertNotNull(result.priorityRecommendation());
        assertNotNull(result.summary());
        assertEquals(task.getId().value(), result.taskId());
    }
}
