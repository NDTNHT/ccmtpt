package com.taskmanagement.adapter.ai;

import com.taskmanagement.application.dto.ai.AiAnalysisResultDto;
import com.taskmanagement.application.dto.ai.AiDeadlineEstimationDto;
import com.taskmanagement.application.dto.ai.AiPriorityRecommendationDto;
import com.taskmanagement.application.dto.ai.AiTaskDecompositionDto;
import com.taskmanagement.application.dto.ai.AiTaskSummaryDto;
import com.taskmanagement.application.port.AiAssistantPort;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.valueobject.Deadline;
import com.taskmanagement.domain.task.valueobject.TaskTitle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeAiAdapterTest {

    @Test
    @DisplayName("Composite adapter should throw IllegalStateException when no AI provider is connected")
    void testCompositeThrowsWhenNoProviderConnected() {
        AiConfig config = AiConfig.defaults();
        CompositeAiAdapter adapter = new CompositeAiAdapter(config);

        Project project = Project.create("Web Portal", "Frontend");
        Task task = Task.create(
            new TaskTitle("Design Login Page"),
            "Create responsive login form with validation",
            Deadline.of(Instant.now().plus(Duration.ofDays(2))),
            Priority.LOW,
            project.getId()
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> adapter.analyzeTaskComprehensive(task, project, Priority.LOW, null));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }

    @Test
    @DisplayName("Composite adapter should delegate comprehensive analysis when custom adapter is provided")
    void testCompositeDelegationWithCustomAdapter() {
        Project project = Project.create("Web Portal", "Frontend");
        Task task = Task.create(
            new TaskTitle("Design Login Page"),
            "Create responsive login form with validation",
            Deadline.of(Instant.now().plus(Duration.ofDays(2))),
            Priority.LOW,
            project.getId()
        );

        AiAssistantPort stub = new AiAssistantPort() {
            @Override
            public AiTaskDecompositionDto decomposeTask(Task task, Project project, String customPrompt) {
                return new AiTaskDecompositionDto(task.getId().value(), List.of("Subtask 1"), "Rationale");
            }

            @Override
            public AiTaskDecompositionDto suggestSubTasks(Task task, Project project, String customPrompt) {
                return new AiTaskDecompositionDto(task.getId().value(), List.of("Subtask 2"), "Rationale");
            }

            @Override
            public AiDeadlineEstimationDto estimateDeadline(Task task, Project project, String customPrompt) {
                return new AiDeadlineEstimationDto(task.getId().value(), 8, Instant.now(), "HIGH", "Reason");
            }

            @Override
            public AiPriorityRecommendationDto analyzePriority(Task task, Project project, Priority domainPriority, String customPrompt) {
                return new AiPriorityRecommendationDto(task.getId().value(), Priority.HIGH, domainPriority, Priority.HIGH, "Reason");
            }

            @Override
            public AiTaskSummaryDto generateSummary(Task task, Project project, String customPrompt) {
                return new AiTaskSummaryDto(task.getId().value(), task.getTitle().value(), "Summary", 100.0, List.of("Point 1"));
            }

            @Override
            public AiAnalysisResultDto analyzeTaskComprehensive(Task task, Project project, Priority domainPriority, String customPrompt) {
                return new AiAnalysisResultDto(
                    task.getId().value(),
                    decomposeTask(task, project, customPrompt),
                    estimateDeadline(task, project, customPrompt),
                    analyzePriority(task, project, domainPriority, customPrompt),
                    generateSummary(task, project, customPrompt)
                );
            }
        };

        CompositeAiAdapter adapter = new CompositeAiAdapter(stub);
        AiAnalysisResultDto result = adapter.analyzeTaskComprehensive(task, project, Priority.LOW, null);

        assertNotNull(result);
        assertNotNull(result.decomposition());
        assertNotNull(result.deadlineEstimation());
        assertNotNull(result.priorityRecommendation());
        assertNotNull(result.summary());
    }
}
