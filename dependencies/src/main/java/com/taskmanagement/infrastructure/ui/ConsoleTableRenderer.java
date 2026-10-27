package com.taskmanagement.infrastructure.ui;

import com.taskmanagement.adapter.presenter.TaskLabels;
import com.taskmanagement.application.dto.ProjectOutputDTO;
import com.taskmanagement.application.dto.TaskOutputDTO;

import java.io.PrintStream;
import java.util.List;
import java.util.UUID;

/**
 * Formats console tables and menus.
 */
public class ConsoleTableRenderer {
    private final PrintStream out;

    public ConsoleTableRenderer() {
        this(System.out);
    }

    public ConsoleTableRenderer(PrintStream out) {
        this.out = out != null ? out : System.out;
    }

    public void renderMenu(UUID currentTaskId) {
        out.println("\n=========================================");
        out.println("        HE THONG QUAN LY TAC VU          ");
        out.println("=========================================");
        out.println("1. Xem danh sach tat ca Task");
        out.println("2. Tao Task moi");
        out.println("3. Bat dau Task");
        out.println("4. Hoan thanh Task");
        out.println("5. Huy Task");
        out.println("6. Danh gia lai do uu tien");
        out.println("0. Thoat");
        out.println("-----------------------------------------");
        if (currentTaskId != null) {
            out.println("Task hien tai: " + currentTaskId);
        }
    }

    public void renderTaskTable(List<TaskOutputDTO> tasks) {
        out.println("\n--- DANH SACH TAT CA TASK ---");
        if (tasks == null || tasks.isEmpty()) {
            out.println("Chua co task nao trong he thong.");
            return;
        }

        out.printf("%-38s | %-25s | %-16s | %-10s | %-8s%n",
            "Task ID", "Tieu De", "Trang Thai", "Uu Tien", "Qua Han");
        out.println("------------------------------------------------------------------------------------------------------");
        for (TaskOutputDTO task : tasks) {
            out.printf("%-38s | %-25s | %-16s | %-10s | %-8s%n",
                task.taskId(),
                truncate(task.title(), 25),
                TaskLabels.status(task.status()),
                TaskLabels.priority(task.priority()),
                task.isOverdue() ? "Co" : "Khong");
        }
    }

    public void renderProjectList(List<ProjectOutputDTO> projects) {
        out.println("Chon Project:");
        if (projects != null) {
            for (int index = 0; index < projects.size(); index++) {
                ProjectOutputDTO project = projects.get(index);
                out.printf("  %d. [%s] %s%n", index + 1, project.type(), project.name());
            }
        }
        int createNewOption = (projects != null ? projects.size() : 0) + 1;
        out.printf("  %d. Tao Project moi...%n", createNewOption);
    }

    public void renderMessage(String message) {
        if (message != null) {
            out.println(message);
        }
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
