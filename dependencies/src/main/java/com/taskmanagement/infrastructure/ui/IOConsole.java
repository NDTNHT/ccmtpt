package com.taskmanagement.infrastructure.ui;

import com.taskmanagement.adapter.ai.AiProtocol;
import com.taskmanagement.adapter.ai.CustomProviderDefinition;
import com.taskmanagement.adapter.presenter.TaskLabels;
import com.taskmanagement.application.boundary.AiTaskInputBoundary;
import com.taskmanagement.application.boundary.ProjectInputBoundary;
import com.taskmanagement.application.boundary.TaskInputBoundary;
import com.taskmanagement.application.dto.ProjectOutputDTO;
import com.taskmanagement.application.dto.TaskInputDTO;
import com.taskmanagement.application.dto.TaskOutputDTO;
import com.taskmanagement.application.dto.ai.AiAnalysisResultDto;
import com.taskmanagement.application.dto.ai.AiDeadlineEstimationDto;
import com.taskmanagement.application.dto.ai.AiPriorityRecommendationDto;
import com.taskmanagement.application.dto.ai.AiTaskDecompositionDto;
import com.taskmanagement.application.dto.ai.AiTaskSummaryDto;
import com.taskmanagement.application.dto.ai.ProviderVerificationResultDto;
import com.taskmanagement.domain.task.enums.Priority;

import java.time.Instant;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * Interactive console client for task, project, and AI assistant operations.
 */
public class IOConsole {
    private final TaskInputBoundary taskInput;
    private final ProjectInputBoundary projectInput;
    private final AiTaskInputBoundary aiTaskInput;
    private final ConsoleInputReader inputReader;
    private final ConsoleTableRenderer tableRenderer;

    public IOConsole(TaskInputBoundary taskInput, ProjectInputBoundary projectInput) {
        this(taskInput, projectInput, null, new ConsoleInputReader(new Scanner(System.in)), new ConsoleTableRenderer());
    }

    public IOConsole(TaskInputBoundary taskInput,
                     ProjectInputBoundary projectInput,
                     AiTaskInputBoundary aiTaskInput) {
        this(taskInput, projectInput, aiTaskInput, new ConsoleInputReader(new Scanner(System.in)), new ConsoleTableRenderer());
    }

    public IOConsole(TaskInputBoundary taskInput,
                     ProjectInputBoundary projectInput,
                     ConsoleInputReader inputReader,
                     ConsoleTableRenderer tableRenderer) {
        this(taskInput, projectInput, null, inputReader, tableRenderer);
    }

    public IOConsole(TaskInputBoundary taskInput,
                     ProjectInputBoundary projectInput,
                     AiTaskInputBoundary aiTaskInput,
                     ConsoleInputReader inputReader,
                     ConsoleTableRenderer tableRenderer) {
        this.taskInput = taskInput;
        this.projectInput = projectInput;
        this.aiTaskInput = aiTaskInput;
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
                    case "7" -> currentTaskId = handleAiAssistant(currentTaskId);
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

    private UUID handleAiAssistant(UUID currentId) {
        if (aiTaskInput == null) {
            tableRenderer.renderMessage("\n[THONG BAO]: Tro ly AI chua duoc cau hinh.");
            return currentId;
        }

        tableRenderer.renderAiMenu(currentId);
        String subChoice = inputReader.readString("Chon chuc nang AI (0-8): ");
        if ("0".equals(subChoice)) {
            return currentId;
        }

        if ("7".equals(subChoice)) {
            handleManageProviders();
            return currentId;
        }

        if ("8".equals(subChoice)) {
            handleUpdateCurrentConfig();
            return currentId;
        }

        UUID id = inputReader.readTaskId("su dung tro ly AI", currentId);
        if (id == null) {
            return currentId;
        }

        String customPrompt = inputReader.readString("Nhap System Prompt tuy bien (nhan Enter de dung mac dinh): ");
        if (customPrompt.isBlank()) {
            customPrompt = null;
        }

        switch (subChoice) {
            case "1" -> {
                AiAnalysisResultDto result = aiTaskInput.analyzeTask(id, customPrompt);
                tableRenderer.renderAiAnalysisResult(result);
                String apply = inputReader.readString("Ban co muon ap dung tat ca (Subtasks, Deadline, Priority) vao Task? (y/n): ");
                if ("y".equalsIgnoreCase(apply)) {
                    aiTaskInput.applyDecomposition(id, result.decomposition().subTasks());
                    aiTaskInput.applyEstimatedDeadline(id, result.deadlineEstimation().recommendedDeadline());
                    aiTaskInput.applyRecommendedPriority(id, result.priorityRecommendation().effectivePriority());
                    tableRenderer.renderMessage("-> DA AP DUNG TOAN BO KET QUA AI VAO TASK THANH CONG!");
                }
            }
            case "2" -> {
                AiTaskDecompositionDto decomp = aiTaskInput.decomposeTask(id, customPrompt);
                tableRenderer.renderAiDecomposition(decomp);
                String apply = inputReader.readString("Ban co muon them checklist subtasks nay vao Task? (y/n): ");
                if ("y".equalsIgnoreCase(apply)) {
                    aiTaskInput.applyDecomposition(id, decomp.subTasks());
                    tableRenderer.renderMessage("-> DA THEM SUBTASKS VAO TASK THANH CONG!");
                }
            }
            case "3" -> {
                AiTaskDecompositionDto suggested = aiTaskInput.suggestSubTasks(id, customPrompt);
                tableRenderer.renderAiDecomposition(suggested);
                String apply = inputReader.readString("Ban co muon them subtasks goi y nay vao Task? (y/n): ");
                if ("y".equalsIgnoreCase(apply)) {
                    aiTaskInput.applyDecomposition(id, suggested.subTasks());
                    tableRenderer.renderMessage("-> DA BO SUNG SUBTASKS VAO TASK THANH CONG!");
                }
            }
            case "4" -> {
                AiDeadlineEstimationDto est = aiTaskInput.estimateDeadline(id, customPrompt);
                tableRenderer.renderAiDeadlineEstimation(est);
                String apply = inputReader.readString("Ban co muon cap nhat Deadline nay vao Task? (y/n): ");
                if ("y".equalsIgnoreCase(apply)) {
                    aiTaskInput.applyEstimatedDeadline(id, est.recommendedDeadline());
                    tableRenderer.renderMessage("-> DA CAP NHAT DEADLINE VAO TASK THANH CONG!");
                }
            }
            case "5" -> {
                AiPriorityRecommendationDto prio = aiTaskInput.analyzePriority(id, customPrompt);
                tableRenderer.renderAiPriorityRecommendation(prio);
                String apply = inputReader.readString("Ban co muon cap nhat Priority nay (" + prio.effectivePriority() + ") vao Task? (y/n): ");
                if ("y".equalsIgnoreCase(apply)) {
                    aiTaskInput.applyRecommendedPriority(id, prio.effectivePriority());
                    tableRenderer.renderMessage("-> DA CAP NHAT PRIORITY VAO TASK THANH CONG!");
                }
            }
            case "6" -> {
                AiTaskSummaryDto summary = aiTaskInput.generateSummary(id, customPrompt);
                tableRenderer.renderAiSummary(summary);
            }
            default -> tableRenderer.renderMessage("Lua chon chuc nang AI khong hop le.");
        }

        return id;
    }

    private void handleManageProviders() {
        List<CustomProviderDefinition> providers = aiTaskInput.getProviders();
        CustomProviderDefinition active = aiTaskInput.getActiveProvider();
        tableRenderer.renderProvidersList(providers, active);

        tableRenderer.renderMessage("1. Chuyen doi Provider dang hoat dong (Activate)");
        tableRenderer.renderMessage("2. Them Provider tuy chinh moi (OpenAI hoac Anthropic standard)");
        tableRenderer.renderMessage("3. Kiem tra Key & Lay danh sach Models tu Provider (/models)");
        tableRenderer.renderMessage("0. Quay lai");

        String choice = inputReader.readString("Lua chon (0-3): ");
        if ("1".equals(choice)) {
            String idToActivate = inputReader.readString("Nhap ID Provider can kich hoat (hoac 'auto'): ");
            try {
                aiTaskInput.setActiveProvider(idToActivate);
                tableRenderer.renderMessage("-> DA KICH HOAT PROVIDER: " + idToActivate);
            } catch (Exception ex) {
                tableRenderer.renderMessage("[LOI]: " + ex.getMessage());
            }
        } else if ("2".equals(choice)) {
            tableRenderer.renderMessage("\n--- THEM PROVIDER TUY CHINH MOI ---");
            String id = inputReader.readString("Nhap ID Provider (vd: claude-work, ollama-local, deepseek): ");
            String name = inputReader.readString("Nhap Ten hien thi: ");
            tableRenderer.renderMessage("Chon giao thuc API:");
            tableRenderer.renderMessage("  1. OpenAI Standard (/chat/completions)");
            tableRenderer.renderMessage("  2. Anthropic Standard (/messages)");
            tableRenderer.renderMessage("  3. Google Gemini Standard (:generateContent)");
            String protoChoice = inputReader.readString("Chon (1-3): ");
            AiProtocol protocol;
            String defaultUrl;
            String defaultModel;
            if ("3".equals(protoChoice)) {
                protocol = AiProtocol.GEMINI;
                defaultUrl = "https://generativelanguage.googleapis.com/v1beta";
                defaultModel = "gemini-2.5-flash";
            } else if ("2".equals(protoChoice)) {
                protocol = AiProtocol.ANTHROPIC;
                defaultUrl = "https://api.anthropic.com/v1";
                defaultModel = "claude-3-5-sonnet-20241022";
            } else {
                protocol = AiProtocol.OPENAI;
                defaultUrl = "https://api.openai.com/v1";
                defaultModel = "gpt-4o-mini";
            }
            String baseUrl = inputReader.readString("Nhap Base URL (nhan Enter de dung " + defaultUrl + "): ");
            if (baseUrl.isBlank()) {
                baseUrl = defaultUrl;
            }
            String model = inputReader.readString("Nhap Model ID (nhan Enter de dung " + defaultModel + "): ");
            if (model.isBlank()) {
                model = defaultModel;
            }
            String apiKey = inputReader.readString("Nhap API Key: ");
            CustomProviderDefinition newProvider = new CustomProviderDefinition(id, name, protocol, baseUrl, apiKey, model, 30);
            aiTaskInput.registerCustomProvider(newProvider);
            tableRenderer.renderMessage("-> DA DANG KY PROVIDER MOI THANH CONG: " + id + " [" + protocol + "]");

            String activateNow = inputReader.readString("Ban co muon kich hoat provider nay ngay bay gio? (y/n): ");
            if ("y".equalsIgnoreCase(activateNow)) {
                aiTaskInput.setActiveProvider(id);
                tableRenderer.renderMessage("-> DA CHUYEN ACTIVE PROVIDER SANG: " + id);
            }
        } else if ("3".equals(choice)) {
            tableRenderer.renderMessage("\n--- KIEM TRA KEY & LAY DANH SACH MODELS (/models) ---");
            tableRenderer.renderMessage("1. Kiem tra Provider da dang ky trong he thong");
            tableRenderer.renderMessage("2. Nhap truc tiep Base URL va API Key de kiem tra");
            tableRenderer.renderMessage("0. Quay lai");
            String testChoice = inputReader.readString("Lua chon (0-2): ");
            if ("1".equals(testChoice)) {
                String providerId = inputReader.readString("Nhap ID Provider can kiem tra: ");
                tableRenderer.renderMessage("Dang ket noi den endpoint /models de xac minh...");
                ProviderVerificationResultDto result = aiTaskInput.verifyRegisteredProvider(providerId);
                tableRenderer.renderVerificationResult(result);
                if (result.valid() && !result.availableModels().isEmpty()) {
                    String updateDef = inputReader.readString("Ban co muon cap nhat Model mac dinh cho provider nay tu danh sach tren? (y/n): ");
                    if ("y".equalsIgnoreCase(updateDef)) {
                        String modelIdx = inputReader.readString("Nhap so thu tu Model muon chon (1-" + result.availableModels().size() + "): ");
                        try {
                            int idx = Integer.parseInt(modelIdx.trim());
                            if (idx >= 1 && idx <= result.availableModels().size()) {
                                String selectedModel = result.availableModels().get(idx - 1);
                                aiTaskInput.updateProviderDefaultModel(providerId, selectedModel);
                                tableRenderer.renderMessage("-> DA CAP NHAT MODEL MAC DINH CUA " + providerId + " THANH: " + selectedModel);
                            }
                        } catch (Exception ex) {
                            tableRenderer.renderMessage("[LOI]: So thu tu khong hop le.");
                        }
                    }
                }
            } else if ("2".equals(testChoice)) {
                tableRenderer.renderMessage("Chon giao thuc API:");
                tableRenderer.renderMessage("  1. OpenAI Standard (/models)");
                tableRenderer.renderMessage("  2. Anthropic Standard (/models)");
                tableRenderer.renderMessage("  3. Google Gemini Standard (/models)");
                String protoChoice = inputReader.readString("Chon (1-3): ");
                AiProtocol protocol;
                String defaultUrl;
                if ("3".equals(protoChoice)) {
                    protocol = AiProtocol.GEMINI;
                    defaultUrl = "https://generativelanguage.googleapis.com/v1beta";
                } else if ("2".equals(protoChoice)) {
                    protocol = AiProtocol.ANTHROPIC;
                    defaultUrl = "https://api.anthropic.com/v1";
                } else {
                    protocol = AiProtocol.OPENAI;
                    defaultUrl = "https://api.openai.com/v1";
                }
                String baseUrl = inputReader.readString("Nhap Base URL (nhan Enter de dung " + defaultUrl + "): ");
                if (baseUrl.isBlank()) {
                    baseUrl = defaultUrl;
                }
                String apiKey = inputReader.readString("Nhap API Key can kiem tra: ");
                tableRenderer.renderMessage("Dang ket noi den " + baseUrl + "/models de xac minh...");
                ProviderVerificationResultDto result = aiTaskInput.verifyAndDiscoverModels(baseUrl, apiKey, protocol, 15);
                tableRenderer.renderVerificationResult(result);
                if (result.valid() && !result.availableModels().isEmpty()) {
                    String saveAs = inputReader.readString("Xac minh thanh cong! Ban co muon luu lam Provider moi khong? (y/n): ");
                    if ("y".equalsIgnoreCase(saveAs)) {
                        String id = inputReader.readString("Nhap ID Provider moi: ");
                        String name = inputReader.readString("Nhap Ten hien thi: ");
                        String defaultModel = result.availableModels().get(0);
                        String modelPick = inputReader.readString("Chon so thu tu Model (1-" + result.availableModels().size() + ") hoac nhan Enter de dung [" + defaultModel + "]: ");
                        String chosenModel = defaultModel;
                        if (!modelPick.isBlank()) {
                            try {
                                int idx = Integer.parseInt(modelPick.trim());
                                if (idx >= 1 && idx <= result.availableModels().size()) {
                                    chosenModel = result.availableModels().get(idx - 1);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                        CustomProviderDefinition newProvider = new CustomProviderDefinition(id, name, protocol, baseUrl, apiKey, chosenModel, 30, result.availableModels());
                        aiTaskInput.registerCustomProvider(newProvider);
                        tableRenderer.renderMessage("-> DA LUU VA DANG KY PROVIDER MOI THANH CONG: " + id + " voi Model: " + chosenModel);
                        String act = inputReader.readString("Kich hoat provider nay ngay bay gio? (y/n): ");
                        if ("y".equalsIgnoreCase(act)) {
                            aiTaskInput.setActiveProvider(id);
                            tableRenderer.renderMessage("-> DA CHUYEN ACTIVE PROVIDER SANG: " + id);
                        }
                    }
                }
            }
        }
    }

    private void handleUpdateCurrentConfig() {
        tableRenderer.renderAiConfig(aiTaskInput.getAiConfig());
        String change = inputReader.readString("Ban co muon thay doi cau hinh Provider / Key? (y/n): ");
        if ("y".equalsIgnoreCase(change)) {
            String newUrl = inputReader.readString("Nhap Base URL moi (nhan Enter de giu nguyen): ");
            String newModel = inputReader.readString("Nhap Model ID moi (nhan Enter de giu nguyen): ");
            String newKey = inputReader.readString("Nhap API Key moi (nhan Enter de giu nguyen): ");
            aiTaskInput.updateAiConfig(
                newUrl.isBlank() ? null : newUrl.trim(),
                newModel.isBlank() ? null : newModel.trim(),
                newKey.isBlank() ? null : newKey.trim()
            );
            tableRenderer.renderMessage("-> DA CAP NHAT CAU HINH AI THANH CONG!");
            tableRenderer.renderAiConfig(aiTaskInput.getAiConfig());
        }
    }
}
