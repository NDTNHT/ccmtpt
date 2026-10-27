package com.taskmanagement.infrastructure.ui;

import com.taskmanagement.adapter.ai.AiConsolePresenter;
import com.taskmanagement.adapter.presenter.TaskLabels;
import com.taskmanagement.application.ai.AiTaskAssistantInputBoundary;
import com.taskmanagement.application.ai.dto.AiPriorityProposalOutputDTO;
import com.taskmanagement.application.ai.dto.AiPromptOverrideInputDTO;
import com.taskmanagement.application.ai.dto.AiSummaryOutputDTO;
import com.taskmanagement.application.ai.dto.AiTaskBreakdownOutputDTO;
import com.taskmanagement.application.boundary.ProjectInputBoundary;
import com.taskmanagement.application.boundary.TaskInputBoundary;
import com.taskmanagement.application.dto.ProjectOutputDTO;
import com.taskmanagement.application.dto.TaskInputDTO;
import com.taskmanagement.application.dto.TaskOutputDTO;
import com.taskmanagement.domain.ai.AiCustomProviderConfig;
import com.taskmanagement.domain.task.enums.Priority;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

/**
 * Interactive console client for task and project operations.
 */
public class IOConsole {
    private final TaskInputBoundary taskInput;
    private final ProjectInputBoundary projectInput;
    private final AiTaskAssistantInputBoundary aiAssistant;
    private final AiConsolePresenter aiPresenter;
    private final ConsoleInputReader inputReader;
    private final ConsoleTableRenderer tableRenderer;
    private AiCustomProviderConfig consoleCustomProvider = null;

    public IOConsole(TaskInputBoundary taskInput, ProjectInputBoundary projectInput) {
        this(taskInput, projectInput, null, new ConsoleInputReader(new Scanner(System.in)), new ConsoleTableRenderer());
    }

    public IOConsole(TaskInputBoundary taskInput,
                     ProjectInputBoundary projectInput,
                     ConsoleInputReader inputReader,
                     ConsoleTableRenderer tableRenderer) {
        this(taskInput, projectInput, null, inputReader, tableRenderer);
    }

    public IOConsole(TaskInputBoundary taskInput,
                     ProjectInputBoundary projectInput,
                     AiTaskAssistantInputBoundary aiAssistant) {
        this(taskInput, projectInput, aiAssistant, new ConsoleInputReader(new Scanner(System.in)), new ConsoleTableRenderer());
    }

    public IOConsole(TaskInputBoundary taskInput,
                     ProjectInputBoundary projectInput,
                     AiTaskAssistantInputBoundary aiAssistant,
                     ConsoleInputReader inputReader,
                     ConsoleTableRenderer tableRenderer) {
        this(taskInput, projectInput, aiAssistant, new AiConsolePresenter(), inputReader, tableRenderer);
    }

    public IOConsole(TaskInputBoundary taskInput,
                     ProjectInputBoundary projectInput,
                     AiTaskAssistantInputBoundary aiAssistant,
                     AiConsolePresenter aiPresenter,
                     ConsoleInputReader inputReader,
                     ConsoleTableRenderer tableRenderer) {
        this.taskInput = taskInput;
        this.projectInput = projectInput;
        this.aiAssistant = aiAssistant;
        this.aiPresenter = aiPresenter != null ? aiPresenter : new AiConsolePresenter();
        this.inputReader = inputReader != null ? inputReader : new ConsoleInputReader(new Scanner(System.in));
        this.tableRenderer = tableRenderer != null ? tableRenderer : new ConsoleTableRenderer();
    }

    public void start() {
        UUID currentTaskId = null;

        while (true) {
            tableRenderer.renderMenu(currentTaskId);
            String choice = inputReader.readString("Chon chuc nang (0-7): ");

            try {
                switch (choice) {
                    case "1" -> handleListTasks();
                    case "2" -> currentTaskId = handleCreateTask();
                    case "3" -> currentTaskId = handleStartTask(currentTaskId);
                    case "4" -> currentTaskId = handleCompleteTask(currentTaskId);
                    case "5" -> currentTaskId = handleCancelTask(currentTaskId);
                    case "6" -> handleRefreshPriority(currentTaskId);
                    case "7" -> handleAiAssistant(currentTaskId);
                    case "0" -> {
                        tableRenderer.renderMessage("\n-> Cam on ban da su dung chuong trinh. Tam biet!");
                        return;
                    }
                    default -> tableRenderer.renderMessage("Lua chon khong hop le, vui long nhap tu 0 den 7.");
                }
            } catch (Exception exception) {
                tableRenderer.renderMessage("\n[LOI]: " + exception.getMessage());
            }
        }
    }

    private void handleListTasks() {
        List<TaskOutputDTO> tasks = taskInput.getTasks();
        tableRenderer.renderTaskTable(tasks);
    }

    private UUID handleCreateTask() {
        tableRenderer.renderMessage("\n--- TAO TASK MOI ---");
        String title = inputReader.readString("Nhap tieu de Task: ");
        String description = inputReader.readString("Nhap mo ta Task: ");
        UUID projectId = chooseProject();

        Instant dueDate = inputReader.readDueDate();
        Priority priority = inputReader.readPriority();

        TaskInputDTO input = new TaskInputDTO(title, description, dueDate, priority, projectId);
        TaskOutputDTO output = taskInput.createTask(input);
        tableRenderer.renderMessage("-> TAO TASK THANH CONG! ID: " + output.taskId());
        return output.taskId();
    }

    private UUID chooseProject() {
        List<ProjectOutputDTO> projects = projectInput.getProjects();
        tableRenderer.renderProjectList(projects);
        int createNewOption = projects.size() + 1;
        String rawChoice = inputReader.readString("Lua chon (1-" + createNewOption + "): ");

        if (rawChoice.isBlank()) {
            return projectInput.getInboxProject().id();
        }

        try {
            int selected = Integer.parseInt(rawChoice);
            if (selected >= 1 && selected <= projects.size()) {
                return projects.get(selected - 1).id();
            }
            if (selected == createNewOption) {
                String newName = inputReader.readString("Nhap ten Project moi: ");
                String newDescription = inputReader.readString("Nhap mo ta Project: ");
                ProjectOutputDTO created = projectInput.createProject(newName, newDescription);
                tableRenderer.renderMessage(
                    "-> Da tao Project: " + created.name() + " [" + created.type() + "]");
                return created.id();
            }
        } catch (NumberFormatException exception) {
            tableRenderer.renderMessage("Lua chon khong phai so.");
        }

        tableRenderer.renderMessage("Lua chon khong hop le, su dung Project Inbox.");
        return projectInput.getInboxProject().id();
    }

    private UUID handleStartTask(UUID currentId) {
        UUID id = inputReader.readTaskId("bat dau", currentId);
        if (id == null) {
            return currentId;
        }
        taskInput.startTask(id);
        tableRenderer.renderMessage("-> DA CHUYEN SANG TRANG THAI: DANG THUC HIEN!");
        return id;
    }

    private UUID handleCompleteTask(UUID currentId) {
        UUID id = inputReader.readTaskId("hoan thanh", currentId);
        if (id == null) {
            return currentId;
        }
        taskInput.completeTask(id);
        tableRenderer.renderMessage("-> DA CHUYEN SANG TRANG THAI: DA HOAN THANH!");
        return id;
    }

    private UUID handleCancelTask(UUID currentId) {
        UUID id = inputReader.readTaskId("huy", currentId);
        if (id == null) {
            return currentId;
        }
        taskInput.cancelTask(id);
        tableRenderer.renderMessage("-> DA CHUYEN SANG TRANG THAI: DA HUY!");
        return id;
    }

    private void handleRefreshPriority(UUID currentId) {
        tableRenderer.renderMessage("\n--- DANH GIA LAI DO UU TIEN ---");
        UUID id = inputReader.readTaskId("danh gia lai do uu tien", currentId);
        if (id == null) {
            return;
        }

        TaskOutputDTO before = taskInput.getTask(id);
        tableRenderer.renderMessage(
            "Uu tien hien tai: " + TaskLabels.priority(before.priority())
                + ", Han chot: " + before.dueDate());

        TaskOutputDTO after = taskInput.refreshTaskPriority(id);
        tableRenderer.renderMessage(
            "Uu tien sau khi danh gia: " + TaskLabels.priority(after.priority()));
        if (before.priority() != after.priority()) {
            tableRenderer.renderMessage("=> Do uu tien da duoc cap nhat!");
        } else {
            tableRenderer.renderMessage("=> Do uu tien giu nguyen.");
        }
    }

    private void handleAiAssistant(UUID currentTaskId) {
        if (aiAssistant == null) {
            tableRenderer.renderMessage("\n[THONG BAO]: Tro ly AI chua duoc cau hinh trong phien console nay.");
            return;
        }

        tableRenderer.renderMessage("\n--- TRO LY AI (AI ASSISTANT) ---");
        if (consoleCustomProvider != null) {
            tableRenderer.renderMessage("[Custom Provider Active]: " + consoleCustomProvider.format().toUpperCase()
                + " -> " + consoleCustomProvider.baseUrl() + " (Model: " + consoleCustomProvider.model() + ")");
        }
        tableRenderer.renderMessage("1. Chia nho Task thanh Subtask & Du doan Deadline (AI Breakdown)");
        tableRenderer.renderMessage("2. De xuat Do uu tien thong minh (AI Priority)");
        tableRenderer.renderMessage("3. Tom tat noi dung Task (AI Summary)");
        tableRenderer.renderMessage("4. Tu cau hinh Custom AI Provider (OpenAI / Anthropic format)");
        tableRenderer.renderMessage("0. Quay lai menu chinh");

        String subChoice = inputReader.readString("Lua chon (0-4): ");
        switch (subChoice.trim()) {
            case "1" -> handleAiBreakdown(currentTaskId);
            case "2" -> handleAiPrioritize(currentTaskId);
            case "3" -> handleAiSummary(currentTaskId);
            case "4" -> handleCustomProviderConfig();
            case "0" -> {}
            default -> tableRenderer.renderMessage("Lua chon khong hop le.");
        }
    }

    private void handleCustomProviderConfig() {
        tableRenderer.renderMessage("\n--- CAU HINH CUSTOM AI PROVIDER (SELF-INTEGRATION) ---");
        tableRenderer.renderMessage("Chon dinh dang giao thuc (Wire format):");
        tableRenderer.renderMessage("1. OpenAI-compatible format (/chat/completions, Bearer token, Ollama, OpenRouter, DeepSeek)");
        tableRenderer.renderMessage("2. Anthropic format (/messages, x-api-key, Claude models, Bedrock Anthropic proxies)");
        tableRenderer.renderMessage("3. Xoa cau hinh Custom (Quay lai mac dinh)");
        String choice = inputReader.readString("Lua chon (1-3): ");
        if ("3".equals(choice.trim())) {
            consoleCustomProvider = null;
            tableRenderer.renderMessage("=> Da xoa custom provider. Su dung provider he thong mac dinh.");
            return;
        }

        String format = "2".equals(choice.trim()) ? "anthropic" : "openai";
        String defaultUrl = "anthropic".equals(format) ? "https://api.anthropic.com/v1" : "https://api.openai.com/v1";
        String baseUrl = inputReader.readString("Base URL (Mac dinh: " + defaultUrl + "): ");
        if (baseUrl.isBlank()) {
            baseUrl = defaultUrl;
        }

        String apiKey = inputReader.readString("API Key (Enter neu chay local/Ollama khong can key): ");
        String defaultModel = "anthropic".equals(format) ? "claude-3-5-sonnet-20241022" : "gpt-4o-mini";
        String model = inputReader.readString("Model name (Mac dinh: " + defaultModel + "): ");
        if (model.isBlank()) {
            model = defaultModel;
        }

        consoleCustomProvider = new AiCustomProviderConfig(format, baseUrl, apiKey, model, Map.of());
        tableRenderer.renderMessage("=> [THANH CONG]: Da thiet lap Custom Provider (" + format.toUpperCase()
            + " tai " + baseUrl + ") cho phien console!");
    }

    private AiPromptOverrideInputDTO buildOverride(String instructions) {
        if (consoleCustomProvider != null) {
            return new AiPromptOverrideInputDTO(
                null,
                instructions.isBlank() ? null : instructions,
                "custom",
                consoleCustomProvider.format(),
                consoleCustomProvider.baseUrl(),
                consoleCustomProvider.apiKey(),
                consoleCustomProvider.model(),
                consoleCustomProvider.headers()
            );
        }
        return instructions.isBlank() ? null : new AiPromptOverrideInputDTO(null, instructions);
    }

    private void handleAiBreakdown(UUID currentTaskId) {
        tableRenderer.renderMessage("\n--- AI TASK BREAKDOWN ---");
        UUID id = inputReader.readTaskId("chia nho bang AI", currentTaskId);
        if (id == null) return;

        String instructions = inputReader.readString("Chi dan bo sung cho AI (Enter de bo qua): ");
        String applyChoice = inputReader.readString("Ban co muon ap dung luu Subtasks va Deadline vao Task khong? (y/N): ");
        boolean apply = "y".equalsIgnoreCase(applyChoice.trim());

        AiPromptOverrideInputDTO override = buildOverride(instructions);
        AiTaskBreakdownOutputDTO breakdown = aiAssistant.breakdownTask(id, apply, override);
        aiPresenter.renderBreakdown(breakdown);
    }

    private void handleAiPrioritize(UUID currentTaskId) {
        tableRenderer.renderMessage("\n--- AI PRIORITY PROPOSAL ---");
        UUID id = inputReader.readTaskId("danh gia uu tien bang AI", currentTaskId);
        if (id == null) return;

        String instructions = inputReader.readString("Chi dan bo sung cho AI (Enter de bo qua): ");
        String applyChoice = inputReader.readString("Ban co muon cap nhat luu do uu tien vao Task khong? (y/N): ");
        boolean apply = "y".equalsIgnoreCase(applyChoice.trim());

        AiPromptOverrideInputDTO override = buildOverride(instructions);
        AiPriorityProposalOutputDTO proposal = aiAssistant.prioritizeTask(id, apply, override);
        aiPresenter.renderPriorityProposal(proposal);
    }

    private void handleAiSummary(UUID currentTaskId) {
        tableRenderer.renderMessage("\n--- AI TASK SUMMARY ---");
        UUID id = inputReader.readTaskId("tom tat bang AI", currentTaskId);
        if (id == null) return;

        String instructions = inputReader.readString("Chi dan bo sung cho AI (Enter de bo qua): ");
        AiPromptOverrideInputDTO override = buildOverride(instructions);
        AiSummaryOutputDTO summary = aiAssistant.summarizeTask(id, override);
        aiPresenter.renderSummary(summary);
    }
}
