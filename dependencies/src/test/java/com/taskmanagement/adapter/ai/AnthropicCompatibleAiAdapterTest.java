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
            "", // Empty key must fail
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
    @DisplayName("Decompose task should throw IllegalStateException when key is missing")
    void testDecomposeThrowsWhenNoApiKey() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> adapter.decomposeTask(task, project, null));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }

    @Test
    @DisplayName("Suggest subtasks should throw IllegalStateException when key is missing")
    void testSuggestSubTasksThrows() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> adapter.suggestSubTasks(task, project, null));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }

    @Test
    @DisplayName("Estimate deadline should throw IllegalStateException when key is missing")
    void testEstimateDeadlineThrows() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> adapter.estimateDeadline(task, project, null));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }

    @Test
    @DisplayName("Analyze priority should throw IllegalStateException when key is missing")
    void testAnalyzePriorityThrows() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> adapter.analyzePriority(task, project, Priority.HIGH, null));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }

    @Test
    @DisplayName("Generate summary should throw IllegalStateException when key is missing")
    void testGenerateSummaryThrows() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> adapter.generateSummary(task, project, null));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }
}
