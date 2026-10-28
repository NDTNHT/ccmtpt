package com.taskmanagement.adapter.ai;

import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.valueobject.Deadline;
import com.taskmanagement.domain.task.valueobject.TaskTitle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleAiAdapterTest {

    private OpenAiCompatibleAiAdapter adapter;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        AiConfig config = new AiConfig();
        config.setApiKey(""); // Empty key must fail
        adapter = new OpenAiCompatibleAiAdapter(config);

        project = Project.create("Core Engine", "Infrastructure");
        task = Task.create(
            new TaskTitle("Refactor Auth Subsystem"),
            "Decouple JWT token validator and session storage",
            Deadline.of(Instant.now().plus(Duration.ofDays(2))),
            Priority.MEDIUM,
            project.getId()
        );
    }

    @Test
    @DisplayName("Decompose task should throw IllegalStateException when API key is missing")
    void testDecomposeThrowsWhenNoApiKey() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> adapter.decomposeTask(task, project, null));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }

    @Test
    @DisplayName("Suggest subtasks should throw IllegalStateException when API key is missing")
    void testSuggestSubTasksThrowsWhenNoApiKey() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> adapter.suggestSubTasks(task, project, null));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }

    @Test
    @DisplayName("Estimate deadline should throw IllegalStateException when API key is missing")
    void testEstimateDeadlineThrowsWhenNoApiKey() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> adapter.estimateDeadline(task, project, null));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }

    @Test
    @DisplayName("Analyze priority should throw IllegalStateException when API key is missing")
    void testAnalyzePriorityThrowsWhenNoApiKey() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> adapter.analyzePriority(task, project, Priority.MEDIUM, null));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }

    @Test
    @DisplayName("Generate summary should throw IllegalStateException when API key is missing")
    void testGenerateSummaryThrowsWhenNoApiKey() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> adapter.generateSummary(task, project, null));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }
}
