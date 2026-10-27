package com.taskmanagement.adapter.presenter;

import com.taskmanagement.application.boundary.TaskShowing;
import com.taskmanagement.application.dto.TaskOutputDTO;

import java.io.PrintStream;

/**
 * Console presenter. Owns all human-readable formatting for the console.
 */
public class TaskConsolePresenter implements TaskShowing {
    private final PrintStream out;

    public TaskConsolePresenter() {
        this(System.out);
    }

    public TaskConsolePresenter(PrintStream out) {
        this.out = out != null ? out : System.out;
    }

    @Override
    public void showResult(TaskOutputDTO outputData) {
        out.println("========================================");
        out.println("THONG TIN TAC VU");
        out.println("ID         : " + outputData.taskId());
        out.println("Tieu de    : " + outputData.title());
        out.println("Mo ta      : " + outputData.description());
        out.println("Trang thai : " + TaskLabels.status(outputData.status()));
        out.println("Uu tien    : " + TaskLabels.priority(outputData.priority()));
        out.println("Han chot   : " + outputData.dueDate());
        out.println("Qua han    : " + (outputData.isOverdue() ? "Co" : "Khong"));
        out.println("Project    : " + outputData.projectId());
        if (!outputData.subTasks().isEmpty()) {
            out.println("SubTask    : " + outputData.subTasks().size());
        }
        out.println("========================================\n");
    }
}
