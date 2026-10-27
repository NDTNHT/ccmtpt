package com.taskmanagement.adapter.presenter;

import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.enums.TaskStatus;

/**
 * Vietnamese display labels for task status and priority.
 */
public final class TaskLabels {
    private TaskLabels() {
    }

    public static String status(TaskStatus status) {
        return switch (status) {
            case TODO -> "Cần làm";
            case IN_PROGRESS -> "Đang thực hiện";
            case DONE -> "Đã hoàn thành";
            case CANCELLED -> "Đã hủy";
        };
    }

    public static String priority(Priority priority) {
        return switch (priority) {
            case LOW -> "Thấp";
            case MEDIUM -> "Trung bình";
            case HIGH -> "Cao";
        };
    }
}
