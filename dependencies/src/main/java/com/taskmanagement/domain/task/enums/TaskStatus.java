package com.taskmanagement.domain.task.enums;

/**
 * Task lifecycle state.
 *
 * <pre>
 * TODO ---start---> IN_PROGRESS ---complete---> DONE
 *   |                    |
 *   +------cancel--------+------> CANCELLED
 * </pre>
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    CANCELLED
}
