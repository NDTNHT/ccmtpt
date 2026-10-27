package com.taskmanagement.adapter.ai;

import com.taskmanagement.application.dto.ai.AiDeadlineEstimationDto;
import com.taskmanagement.application.dto.ai.AiPriorityRecommendationDto;
import com.taskmanagement.application.dto.ai.AiTaskDecompositionDto;
import com.taskmanagement.application.dto.ai.AiTaskSummaryDto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleAiAdapterTest {

    private OpenAiCompatibleAiAdapter adapter;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        AiConfig config = new AiConfig();
        config.setApiKey(""); // Empty key forces fallback
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
    @DisplayName("Decompose task should smoothly fall back when API key is missing")
    void testDecomposeFallbackWhenNoApiKey() {
        AiTaskDecompositionDto result = adapter.decomposeTask(task, project, null);

        assertNotNull(result);
        assertEquals(task.getId().value(), result.taskId());
        assertFalse(result.subTasks().isEmpty());
    }

    @Test
    @DisplayName("Suggest subtasks should smoothly fall back when API key is missing")
    void testSuggestSubTasksFallbackWhenNoApiKey() {
        AiTaskDecompositionDto result = adapter.suggestSubTasks(task, project, null);

        assertNotNull(result);
        assertFalse(result.subTasks().isEmpty());
    }

    @Test
    @DisplayName("Estimate deadline should smoothly fall back when API key is missing")
    void testEstimateDeadlineFallbackWhenNoApiKey() {
        AiDeadlineEstimationDto result = adapter.estimateDeadline(task, project, null);

        assertNotNull(result);
        assertTrue(result.estimatedHours() > 0);
        assertNotNull(result.recommendedDeadline());
    }

    @Test
    @DisplayName("Analyze priority should smoothly fall back when API key is missing")
    void testAnalyzePriorityFallbackWhenNoApiKey() {
        AiPriorityRecommendationDto result = adapter.analyzePriority(task, project, Priority.MEDIUM, null);

        assertNotNull(result);
        assertNotNull(result.effectivePriority());
        assertNotNull(result.reasoning());
    }

    @Test
    @DisplayName("Generate summary should smoothly fall back when API key is missing")
    void testGenerateSummaryFallbackWhenNoApiKey() {
        AiTaskSummaryDto result = adapter.generateSummary(task, project, null);

        assertNotNull(result);
        assertEquals("Refactor Auth Subsystem", result.title());
        assertNotNull(result.summary());
    }
}
