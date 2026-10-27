package com.taskmanagement.adapter.ai;

import com.taskmanagement.application.dto.ai.AiAnalysisResultDto;
import com.taskmanagement.application.dto.ai.AiDeadlineEstimationDto;
import com.taskmanagement.application.dto.ai.AiPriorityRecommendationDto;
import com.taskmanagement.application.dto.ai.AiTaskDecompositionDto;
import com.taskmanagement.application.dto.ai.AiTaskSummaryDto;
import com.taskmanagement.application.dto.ai.ProviderVerificationResultDto;
import com.taskmanagement.application.port.AiAssistantPort;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.project.enums.ProjectType;
import com.taskmanagement.domain.task.entity.SubTask;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Deterministic, offline fallback AI implementation.
 * Performs intelligent semantic heuristics, text parsing, and pattern extraction
 * without requiring external network connectivity or paid API credentials.
 */
public class SimulatedAiAdapter implements AiAssistantPort {
    private final AiConfig config;

    public SimulatedAiAdapter() {
        this(AiConfig.forSimulation());
    }

    public SimulatedAiAdapter(AiConfig config) {
        this.config = config != null ? config : AiConfig.forSimulation();
    }

    @Override
    public AiConfig getConfig() {
        return config;
    }

    @Override
    public ProviderVerificationResultDto verifyAndDiscoverModels(String baseUrl, String apiKey, AiProtocol protocol, int timeoutSeconds) {
        return ProviderVerificationResultDto.success(200, List.of("heuristic-default", "heuristic-fast", "heuristic-deep"), "SIMULATED");
    }

    @Override
    public AiTaskDecompositionDto decomposeTask(Task task, Project project, String customPrompt) {
        Objects.requireNonNull(task, "Task cannot be null");
        String title = task.getTitle().value();
        String description = task.getDescription() != null ? task.getDescription() : "";
        String combined = (title + " " + description).toLowerCase(Locale.ROOT);

        Set<String> subTasks = new LinkedHashSet<>();

        // If description has line breaks or numbered points, extract them
        if (description.contains("\n") || description.matches("(?s).*\\d+\\..*")) {
            for (String line : description.split("\\r?\\n")) {
                String cleaned = line.replaceFirst("^[\\d+\\-*•]\\s*", "").trim();
                if (cleaned.length() >= 5) {
                    subTasks.add(cleaned);
                }
            }
        }

        // Domain semantic heuristics
        if (combined.contains("refactor") || combined.contains("restructure")) {
            subTasks.add("Analyze current design and identify coupling hotspots");
            subTasks.add("Define new Ports and Boundaries following Clean Architecture");
            subTasks.add("Refactor implementation into isolated cohesive classes");
            subTasks.add("Write unit tests to verify behavior preservation");
        } else if (combined.contains("api") || combined.contains("controller") || combined.contains("endpoint")) {
            subTasks.add("Design REST API request/response contracts and DTOs");
            subTasks.add("Implement controller endpoints and input validation");
            subTasks.add("Wire use case interactor into infrastructure configuration");
            subTasks.add("Perform integration testing and verify error responses");
        } else if (combined.contains("bug") || combined.contains("fix") || combined.contains("error")) {
            subTasks.add("Reproduce defect with targeted failing test case");
            subTasks.add("Identify root cause in domain or persistence layer");
            subTasks.add("Implement bug fix and verify edge conditions");
            subTasks.add("Ensure full regression test suite passes");
        } else if (combined.contains("ui") || combined.contains("console") || combined.contains("view")) {
            subTasks.add("Draft user interaction flow and menu structure");
            subTasks.add("Implement UI presenter and console renderers");
            subTasks.add("Handle user input edge cases and graceful cancellations");
        } else {
            subTasks.add("Clarify functional specifications and acceptance criteria");
            subTasks.add("Implement domain entity changes and validation invariants");
            subTasks.add("Build application use case logic and boundary ports");
            subTasks.add("Conduct verification testing and documentation");
        }

        String rationale = "Decomposed into " + subTasks.size() + " actionable checklist steps based on task scope.";
        return new AiTaskDecompositionDto(task.getId().value(), new ArrayList<>(subTasks), rationale);
    }

    @Override
    public AiTaskDecompositionDto suggestSubTasks(Task task, Project project, String customPrompt) {
        Objects.requireNonNull(task, "Task cannot be null");
        List<SubTask> existing = task.getSubTasks();
        Set<String> suggestions = new LinkedHashSet<>();

        boolean hasTesting = existing.stream().anyMatch(s -> s.getTitle().toLowerCase(Locale.ROOT).contains("test"));
        boolean hasDocs = existing.stream().anyMatch(s -> s.getTitle().toLowerCase(Locale.ROOT).contains("doc"));
        boolean hasReview = existing.stream().anyMatch(s -> s.getTitle().toLowerCase(Locale.ROOT).contains("review"));
        boolean hasDeploy = existing.stream().anyMatch(s -> s.getTitle().toLowerCase(Locale.ROOT).contains("deploy"));

        if (!hasTesting) {
            suggestions.add("Add unit and boundary tests covering edge cases");
        }
        if (!hasDocs) {
            suggestions.add("Document API endpoints and architecture design");
        }
        if (!hasReview) {
            suggestions.add("Perform code review and Clean Architecture compliance audit");
        }
        if (!hasDeploy) {
            suggestions.add("Verify build artifacts and deployment readiness");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("Perform regression and performance verification");
            suggestions.add("Archive completed project artifacts");
        }

        String rationale = "Suggested " + suggestions.size() + " complementary subtasks to improve delivery quality.";
        return new AiTaskDecompositionDto(task.getId().value(), new ArrayList<>(suggestions), rationale);
    }

    @Override
    public AiDeadlineEstimationDto estimateDeadline(Task task, Project project, String customPrompt) {
        Objects.requireNonNull(task, "Task cannot be null");
        int subTaskCount = Math.max(task.getSubTasks().size(), 1);
        String combined = (task.getTitle().value() + " " + (task.getDescription() != null ? task.getDescription() : "")).toLowerCase(Locale.ROOT);

        int baseHours = 8;
        if (combined.contains("urgent") || combined.contains("critical") || combined.contains("hotfix")) {
            baseHours = 4;
        } else if (combined.contains("complex") || combined.contains("architecture") || combined.contains("redesign")) {
            baseHours = 24;
        }

        int totalHours = baseHours + (subTaskCount * 4);
        Instant now = Instant.now();
        Instant recommendedDeadline = now.plus(Duration.ofHours(totalHours));

        String confidence = subTaskCount >= 3 ? "HIGH" : "MEDIUM";
        String explanation = String.format("Estimated %d hours based on %d subtasks and task scope complexity.",
            totalHours, subTaskCount);

        return new AiDeadlineEstimationDto(task.getId().value(), totalHours, recommendedDeadline, confidence, explanation);
    }

    @Override
    public AiPriorityRecommendationDto analyzePriority(Task task, Project project, Priority domainPriority, String customPrompt) {
        Objects.requireNonNull(task, "Task cannot be null");
        String text = (task.getTitle().value() + " " + (task.getDescription() != null ? task.getDescription() : "")).toLowerCase(Locale.ROOT);

        Priority aiProposed;
        String reasoning;

        if (text.contains("critical") || text.contains("blocker") || text.contains("p0") || text.contains("emergency")) {
            aiProposed = Priority.HIGH;
            reasoning = "AI identified high-severity keywords indicating critical project blockers.";
        } else if (text.contains("bug") || text.contains("defect") || text.contains("urgent")) {
            aiProposed = Priority.HIGH;
            reasoning = "AI identified defect/urgent keywords requiring immediate attention.";
        } else if (text.contains("minor") || text.contains("chore") || text.contains("cosmetic") || text.contains("doc")) {
            aiProposed = Priority.LOW;
            reasoning = "AI identified low-impact routine task keywords.";
        } else {
            aiProposed = Priority.MEDIUM;
            reasoning = "Standard task complexity without urgent escalations.";
        }

        // Domain invariant: INBOX project caps priority at MEDIUM
        Priority effective = aiProposed;
        if (project != null && project.getProjectType() == ProjectType.INBOX && effective == Priority.HIGH) {
            effective = Priority.MEDIUM;
            reasoning += " (Capped at MEDIUM due to Inbox project domain policy).";
        }

        // Domain invariant: if domain priority escalated due to approaching deadline, respect domain escalation
        if (domainPriority == Priority.HIGH) {
            effective = Priority.HIGH;
            reasoning += " (Escalated to HIGH by domain deadline rules).";
        }

        return new AiPriorityRecommendationDto(task.getId().value(), aiProposed, domainPriority, effective, reasoning);
    }

    @Override
    public AiTaskSummaryDto generateSummary(Task task, Project project, String customPrompt) {
        Objects.requireNonNull(task, "Task cannot be null");
        List<SubTask> subTasks = task.getSubTasks();
        long completed = subTasks.stream().filter(SubTask::isCompleted).count();
        double completionRate = subTasks.isEmpty() ? 0.0 : ((double) completed / subTasks.size()) * 100.0;

        List<String> highlights = new ArrayList<>();
        highlights.add("Status: " + task.getStatus());
        highlights.add("Priority: " + task.getPriority());
        highlights.add(String.format(Locale.US, "Checklist Progress: %d/%d completed (%.1f%%)", completed, subTasks.size(), completionRate));

        if (task.getDueDate() != null) {
            highlights.add("Deadline: " + task.getDueDate().dueDate());
            if (task.isOverdue()) {
                highlights.add("WARNING: Task is currently overdue!");
            }
        }

        String summaryText = String.format(Locale.US,
            "Task '%s' is %s with %s priority. Progress is at %.1f%% across %d defined subtask(s).",
            task.getTitle().value(), task.getStatus(), task.getPriority(), completionRate, subTasks.size());

        return new AiTaskSummaryDto(task.getId().value(), task.getTitle().value(), summaryText, completionRate, highlights);
    }

    @Override
    public AiAnalysisResultDto analyzeTaskComprehensive(Task task, Project project, Priority domainPriority, String customPrompt) {
        AiTaskDecompositionDto decomposition = decomposeTask(task, project, customPrompt);
        AiDeadlineEstimationDto deadlineEstimation = estimateDeadline(task, project, customPrompt);
        AiPriorityRecommendationDto priorityRecommendation = analyzePriority(task, project, domainPriority, customPrompt);
        AiTaskSummaryDto summary = generateSummary(task, project, customPrompt);

        return new AiAnalysisResultDto(
            task.getId().value(),
            decomposition,
            deadlineEstimation,
            priorityRecommendation,
            summary
        );
    }
}
