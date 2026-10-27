package com.taskmanagement.adapter.ai;

import com.taskmanagement.application.dto.ai.AiAnalysisResultDto;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.project.enums.ProjectType;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.valueobject.Deadline;
import com.taskmanagement.domain.task.valueobject.TaskTitle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CompositeAiAdapterTest {

    @Test
    @DisplayName("Composite adapter should delegate comprehensive analysis without errors")
    void testCompositeDelegation() {
        AiConfig config = AiConfig.forSimulation();
        CompositeAiAdapter adapter = new CompositeAiAdapter(config);

        Project project = Project.create("Web Portal", "Frontend");
        Task task = Task.create(
            new TaskTitle("Design Login Page"),
            "Create responsive login form with validation",
            Deadline.of(Instant.now().plus(Duration.ofDays(2))),
            Priority.LOW,
            project.getId()
        );

        AiAnalysisResultDto result = adapter.analyzeTaskComprehensive(task, project, Priority.LOW, null);

        assertNotNull(result);
        assertNotNull(result.decomposition());
        assertNotNull(result.deadlineEstimation());
        assertNotNull(result.priorityRecommendation());
        assertNotNull(result.summary());
    }
}
