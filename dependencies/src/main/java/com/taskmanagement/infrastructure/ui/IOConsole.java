package com.taskmanagement.infrastructure.ui;

import com.taskmanagement.adapter.presenter.TaskLabels;
import com.taskmanagement.application.boundary.ProjectInputBoundary;
import com.taskmanagement.application.boundary.TaskInputBoundary;
import com.taskmanagement.application.dto.ProjectOutputDTO;
import com.taskmanagement.application.dto.TaskInputDTO;
import com.taskmanagement.application.dto.TaskOutputDTO;
import com.taskmanagement.domain.task.enums.Priority;

import java.time.Instant;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * Interactive console client for task and project operations.
 */
public class IOConsole {
    private final TaskInputBoundary taskInput;
    private final ProjectInputBoundary projectInput;
    private final ConsoleInputReader inputReader;
    private final ConsoleTableRenderer tableRenderer;

    public IOConsole(TaskInputBoundary taskInput, ProjectInputBoundary projectInput) {
        this(taskInput, projectInput, new ConsoleInputReader(new Scanner(System.in)), new ConsoleTableRenderer());
    }

    public IOConsole(TaskInputBoundary taskInput,
                     ProjectInputBoundary projectInput,
                     ConsoleInputReader inputReader,
                     ConsoleTableRenderer tableRenderer) {
        this.taskInput = taskInput;
        this.projectInput = projectInput;
        this.inputReader = inputReader != null ? inputReader : new ConsoleInputReader(new Scanner(System.in));
        this.tableRenderer = tableRenderer != null ? tableRenderer : new ConsoleTableRenderer();
    }

    public void start() {
        UUID currentTaskId = null;

        while (true) {
            tableRenderer.renderMenu(currentTaskId);
            String choice = inputReader.readString("Chon chuc nang (0-6): ");

            try {
                switch (choice) {
                    case "1" -> handleListTasks();
                    case "2" -> currentTaskId = handleCreateTask();
                    case "3" -> currentTaskId = handleStartTask(currentTaskId);
                    case "4" -> currentTaskId = handleCompleteTask(currentTaskId);
                    case "5" -> currentTaskId = handleCancelTask(currentTaskId);
                    case "6" -> handleRefreshPriority(currentTaskId);
                    case "0" -> {
                        tableRenderer.renderMessage("\n-> Cam on ban da su dung chuong trinh. Tam biet!");
                        return;
                    }
                    default -> tableRenderer.renderMessage("Lua chon khong hop le, vui long nhap tu 0 den 6.");
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
}
