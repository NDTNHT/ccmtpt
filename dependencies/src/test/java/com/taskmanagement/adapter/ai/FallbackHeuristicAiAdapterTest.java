package com.taskmanagement.adapter.ai;

import com.taskmanagement.domain.ai.AiPriorityProposal;
import com.taskmanagement.domain.ai.AiPromptConfig;
import com.taskmanagement.domain.ai.AiTaskBreakdown;
import com.taskmanagement.domain.ai.AiTaskSummary;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.project.valueobject.ProjectId;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.enums.TaskStatus;
import com.taskmanagement.domain.task.valueobject.Deadline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FallbackHeuristicAiAdapter Unit Tests")
class FallbackHeuristicAiAdapterTest {

    private FallbackHeuristicAiAdapter adapter;
    private Project project;
    private AiPromptConfig config;

    @BeforeEach
    void setUp() {
        adapter = new FallbackHeuristicAiAdapter();
        project = Project.create("Backend", "Backend tasks");
        config = AiPromptConfig.defaultConfig();
    }

    @Test
    @DisplayName("Generates bug-fix subtasks when task relates to a bug")
    void shouldGenerateBugWorkflow() {
        Task bugTask = Task.create("Fix null pointer in login", "Crash occurs when username is blank", null, Priority.MEDIUM, project.getId());

        AiTaskBreakdown breakdown = adapter.generateTaskBreakdown(bugTask, project, config);
        assertNotNull(breakdown);
        assertFalse(breakdown.subtasks().isEmpty());
        assertTrue(breakdown.subtasks().stream().anyMatch(s -> s.title().contains("Reproduce")));
        assertTrue(breakdown.reasoning().contains("bug fix workflow"));
        assertNotNull(breakdown.estimatedDeadline());
    }

    @Test
    @DisplayName("Generates refactoring subtasks when task relates to refactor")
    void shouldGenerateRefactoringWorkflow() {
        Task refactorTask = Task.create("Refactor order service", "Clean architecture modularization", null, Priority.LOW, project.getId());

        AiTaskBreakdown breakdown = adapter.generateTaskBreakdown(refactorTask, project, config);
        assertTrue(breakdown.subtasks().stream().anyMatch(s -> s.title().contains("Audit")));
        assertTrue(breakdown.reasoning().contains("refactoring workflow"));
    }

    @Test
    @DisplayName("Filters out subtasks that are already in the task")
    void shouldFilterExistingSubtasks() {
        Task task = Task.create("Fix payment issue", "Error on checkout", null, Priority.HIGH, project.getId());
        task.addSubTask("Reproduce issue and verify error conditions");

        AiTaskBreakdown breakdown = adapter.generateTaskBreakdown(task, project, config);
        assertFalse(breakdown.subtasks().stream().anyMatch(s -> s.title().equals("Reproduce issue and verify error conditions")));
    }

    @Test
    @DisplayName("Evaluates priority to HIGH for urgent keywords or overdue deadline")
    void shouldEvaluateHighPriority() {
        Task urgentTask = Task.create("Critical production outage", "Database lock detected", null, Priority.LOW, project.getId());
        AiPriorityProposal proposal = adapter.suggestPriority(urgentTask, project, config);
        assertEquals(Priority.HIGH, proposal.priority());
        assertTrue(proposal.confidence() > 0.8);

        // Overdue task
        Instant past = Instant.now().minus(2, ChronoUnit.DAYS);
        Task overdueTask = Task.create("Submit report", "Quarterly figures", Deadline.of(past), Priority.LOW, project.getId());
        AiPriorityProposal overdueProposal = adapter.suggestPriority(overdueTask, project, config);
        assertEquals(Priority.HIGH, overdueProposal.priority());
    }

    @Test
    @DisplayName("Evaluates priority to LOW for cleanup keywords")
    void shouldEvaluateLowPriority() {
        Task lowTask = Task.create("Cleanup comments and typo fixes", "Minor formatting", null, Priority.MEDIUM, project.getId());
        AiPriorityProposal proposal = adapter.suggestPriority(lowTask, project, config);
        assertEquals(Priority.LOW, proposal.priority());
    }

    @Test
    @DisplayName("Generates task summary with status and highlights")
    void shouldGenerateTaskSummary() {
        Task task = Task.create("Build REST API", "Implement endpoints", null, Priority.HIGH, project.getId());
        task.addSubTask("Write controller");

        AiTaskSummary summary = adapter.generateTaskSummary(task, project, config);
        assertNotNull(summary.summary());
        assertTrue(summary.summary().contains("Build REST API"));
        assertFalse(summary.keyHighlights().isEmpty());
        assertNotNull(summary.suggestedNextStep());
    }
}
