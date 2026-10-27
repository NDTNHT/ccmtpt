package com.taskmanagement.adapter.ai;

import com.taskmanagement.domain.ai.AiModelPort;
import com.taskmanagement.domain.ai.AiPriorityProposal;
import com.taskmanagement.domain.ai.AiPromptConfig;
import com.taskmanagement.domain.ai.AiSubTaskProposal;
import com.taskmanagement.domain.ai.AiTaskBreakdown;
import com.taskmanagement.domain.ai.AiTaskSummary;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.task.entity.SubTask;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.enums.TaskStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Robust rule-based heuristic AI adapter.
 * Used for offline environments, automated CI testing, or graceful fallback
 * when remote LLM APIs are unreachable or lack API keys.
 */
public class FallbackHeuristicAiAdapter implements AiModelPort {

    private static final Set<String> HIGH_PRIORITY_KEYWORDS = Set.of(
        "urgent", "critical", "blocking", "blocker", "emergency", "asap",
        "p0", "p1", "production", "crash", "security", "vulnerability", "fatal", "hotfix"
    );

    private static final Set<String> LOW_PRIORITY_KEYWORDS = Set.of(
        "cleanup", "minor", "optional", "nice to have", "someday", "typo", "trivial"
    );

    @Override
    public AiTaskBreakdown generateTaskBreakdown(Task task, Project project, AiPromptConfig promptConfig) {
        String text = ((task.getTitle() != null ? task.getTitle().value() : "") + " "
            + (task.getDescription() != null ? task.getDescription() : "")).toLowerCase(Locale.ROOT);

        List<AiSubTaskProposal> proposals = new ArrayList<>();
        Instant estimatedDeadline;
        String reasoning;

        Instant now = Instant.now();

        if (containsAny(text, "bug", "fix", "error", "defect", "fail", "crash", "hotfix")) {
            proposals.add(new AiSubTaskProposal("Reproduce issue and verify error conditions", "Replicate bug in a local test scenario"));
            proposals.add(new AiSubTaskProposal("Identify root cause and write test scenario", "Pinpoint failure source and create regression test"));
            proposals.add(new AiSubTaskProposal("Implement fix and verify against test cases", "Apply minimal and focused corrective changes"));
            proposals.add(new AiSubTaskProposal("Perform regression checks and documentation update", "Ensure no side effects in related components"));
            estimatedDeadline = now.plus(Duration.ofDays(2));
            reasoning = "Heuristic bug fix workflow: reproduction, diagnosis, patch, and regression verification.";
        } else if (containsAny(text, "refactor", "clean", "structure", "architecture", "migrate")) {
            proposals.add(new AiSubTaskProposal("Audit existing code structure and define boundaries", "Map current dependencies and target design"));
            proposals.add(new AiSubTaskProposal("Set up test harnesses and safety net", "Ensure complete test coverage before modifying code"));
            proposals.add(new AiSubTaskProposal("Perform incremental refactoring steps", "Execute modular architectural improvements"));
            proposals.add(new AiSubTaskProposal("Verify behavior consistency and performance", "Validate that external behavior remains intact"));
            estimatedDeadline = now.plus(Duration.ofDays(4));
            reasoning = "Heuristic refactoring workflow: safe, incremental, and verified.";
        } else if (containsAny(text, "doc", "spec", "design", "guide", "readme")) {
            proposals.add(new AiSubTaskProposal("Gather requirements and background context", "Collect user stories and technical constraints"));
            proposals.add(new AiSubTaskProposal("Draft architecture outline and specifications", "Write initial overview and detailed sections"));
            proposals.add(new AiSubTaskProposal("Review draft with stakeholders and incorporate feedback", "Refine content based on peer comments"));
            proposals.add(new AiSubTaskProposal("Finalize and publish documentation", "Ensure formatting and link validity"));
            estimatedDeadline = now.plus(Duration.ofDays(3));
            reasoning = "Heuristic documentation workflow: research, draft, review, and publication.";
        } else if (containsAny(text, "deploy", "release", "docker", "ci", "cd", "pipeline")) {
            proposals.add(new AiSubTaskProposal("Validate environment configurations and secrets", "Confirm configuration parameters and secrets"));
            proposals.add(new AiSubTaskProposal("Execute build and automated test suites", "Ensure clean compilation and test passing"));
            proposals.add(new AiSubTaskProposal("Deploy to staging environment and smoke test", "Verify health and core endpoints on staging"));
            proposals.add(new AiSubTaskProposal("Roll out to production and monitor telemetry", "Perform staged rollout and monitor metrics"));
            estimatedDeadline = now.plus(Duration.ofDays(2));
            reasoning = "Heuristic deployment workflow: validation, build, staging, and production monitoring.";
        } else {
            String title = task.getTitle() != null ? task.getTitle().value() : "Task";
            proposals.add(new AiSubTaskProposal("Clarify scope and acceptance criteria for: " + title, "Understand exact task expectations"));
            proposals.add(new AiSubTaskProposal("Implement core functionality and domain logic", "Build necessary entities, rules, and services"));
            proposals.add(new AiSubTaskProposal("Write unit and integration tests", "Provide high test coverage for happy and edge cases"));
            proposals.add(new AiSubTaskProposal("Review implementation and verify completion", "Perform quality check and peer review"));
            estimatedDeadline = now.plus(Duration.ofDays(5));
            reasoning = "Standard task delivery breakdown (requirements, implementation, testing, review).";
        }

        // Filter out existing subtasks
        List<AiSubTaskProposal> filtered = proposals.stream()
            .filter(proposal -> task.getSubTasks().stream()
                .noneMatch(existing -> existing.getTitle().equalsIgnoreCase(proposal.title())))
            .toList();

        if (filtered.isEmpty()) {
            filtered = List.of(new AiSubTaskProposal("Follow-up review for: " + task.getTitle().value(), "Review ongoing task progress"));
        }

        return new AiTaskBreakdown(filtered, estimatedDeadline, reasoning);
    }

    @Override
    public AiPriorityProposal suggestPriority(Task task, Project project, AiPromptConfig promptConfig) {
        String text = ((task.getTitle() != null ? task.getTitle().value() : "") + " "
            + (task.getDescription() != null ? task.getDescription() : "")).toLowerCase(Locale.ROOT);

        // Check overdue or imminent deadline
        boolean isOverdue = task.isOverdue();
        boolean isApproachingDeadline = task.getDueDate() != null
            && task.getDueDate().isApproaching(Duration.ofDays(2));

        if (isOverdue || isApproachingDeadline || containsAny(text, HIGH_PRIORITY_KEYWORDS)) {
            return new AiPriorityProposal(
                Priority.HIGH,
                "High priority recommended: " + (isOverdue ? "task is overdue" : "urgent keywords or near-term deadline detected"),
                0.95
            );
        }

        if (containsAny(text, LOW_PRIORITY_KEYWORDS)) {
            return new AiPriorityProposal(
                Priority.LOW,
                "Low priority recommended: task contains non-urgent or maintenance keywords",
                0.85
            );
        }

        return new AiPriorityProposal(
            Priority.MEDIUM,
            "Medium priority recommended: standard operational priority for balanced workflow",
            0.80
        );
    }

    @Override
    public AiTaskSummary generateTaskSummary(Task task, Project project, AiPromptConfig promptConfig) {
        String projectName = project != null ? project.getName().value() : "Inbox";
        String summary = String.format(
            "Task '%s' under project '%s'. Status: %s, Priority: %s.",
            task.getTitle().value(),
            projectName,
            task.getStatus(),
            task.getPriority()
        );

        List<String> highlights = new ArrayList<>();
        String desc = task.getDescription();
        highlights.add("Description: " + (desc != null && !desc.isBlank() ? desc : "None provided"));
        highlights.add("Deadline: " + (task.getDueDate() != null
            ? task.getDueDate().dueDate().toString() + (task.isOverdue() ? " [OVERDUE]" : "")
            : "No deadline set"));

        long completedSubtasks = task.getSubTasks().stream().filter(SubTask::isCompleted).count();
        highlights.add(String.format("Subtasks: %d/%d completed", completedSubtasks, task.getSubTasks().size()));

        String nextStep;
        if (task.getStatus() == TaskStatus.TODO) {
            nextStep = "Start the task and begin working on the initial subtask.";
        } else if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            nextStep = "Continue executing remaining subtasks and complete open items.";
        } else if (task.getStatus() == TaskStatus.DONE) {
            nextStep = "Task is already completed; verify all acceptance criteria and archive.";
        } else {
            nextStep = "Task has been cancelled; check if reactivation is required.";
        }

        return new AiTaskSummary(summary, highlights, nextStep);
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String text, Set<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
