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

class AnthropicCompatibleAiAdapterTest {

    private AnthropicCompatibleAiAdapter adapter;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        CustomProviderDefinition def = CustomProviderDefinition.ofAnthropic(
            "anthropic-test",
            "Claude Test",
            "https://api.anthropic.com/v1",
            "", // Empty key forces safe fallback
            "claude-3-5-sonnet-20241022"
        );
        AiConfig config = AiConfig.defaults();
        adapter = new AnthropicCompatibleAiAdapter(def, config);

        project = Project.create("Claude Service", "Anthropic Claude Testing");
        task = Task.create(
            new TaskTitle("Build Claude Prompt Engine"),
            "Implement Anthropic messages protocol integration with Clean Architecture",
            Deadline.of(Instant.now().plus(Duration.ofDays(2))),
            Priority.HIGH,
            project.getId()
        );
    }

    @Test
    @DisplayName("Decompose task should safely fallback to SimulatedAiAdapter when key is missing")
    void testDecomposeFallbackWhenNoApiKey() {
        AiTaskDecompositionDto result = adapter.decomposeTask(task, project, null);

        assertNotNull(result);
        assertEquals(task.getId().value(), result.taskId());
        assertFalse(result.subTasks().isEmpty());
    }

    @Test
    @DisplayName("Suggest subtasks should safely fallback when key is missing")
    void testSuggestSubTasksFallback() {
        AiTaskDecompositionDto result = adapter.suggestSubTasks(task, project, null);

        assertNotNull(result);
        assertFalse(result.subTasks().isEmpty());
    }

    @Test
    @DisplayName("Estimate deadline should safely fallback when key is missing")
    void testEstimateDeadlineFallback() {
        AiDeadlineEstimationDto result = adapter.estimateDeadline(task, project, null);

        assertNotNull(result);
        assertTrue(result.estimatedHours() > 0);
        assertNotNull(result.recommendedDeadline());
    }

    @Test
    @DisplayName("Analyze priority should safely fallback when key is missing")
    void testAnalyzePriorityFallback() {
        AiPriorityRecommendationDto result = adapter.analyzePriority(task, project, Priority.HIGH, null);

        assertNotNull(result);
        assertEquals(Priority.HIGH, result.effectivePriority());
    }

    @Test
    @DisplayName("Generate summary should safely fallback when key is missing")
    void testGenerateSummaryFallback() {
        AiTaskSummaryDto result = adapter.generateSummary(task, project, null);

        assertNotNull(result);
        assertEquals("Build Claude Prompt Engine", result.title());
        assertNotNull(result.summary());
    }
}
