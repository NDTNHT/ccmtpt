package com.taskmanagement.domain.task.valueobject;

import com.taskmanagement.domain.task.exception.TaskValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests for Deadline Value Object")
class DeadlineTest {

    @Test
    @DisplayName("Ném lỗi nếu dueDate null")
    void shouldThrowExceptionWhenDueDateIsNull() {
        assertThrows(TaskValidationException.class, () -> new Deadline(null));
    }

    @Test
    @DisplayName("Kiểm tra quá hạn chính xác với thời gian truyền vào (Pure function)")
    void shouldCheckOverdueDeterministically() {
        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant now = Instant.now();

        Deadline deadline = Deadline.of(past);
        assertTrue(deadline.isOverdue(now));

        Instant future = Instant.now().plus(2, ChronoUnit.HOURS);
        Deadline futureDeadline = Deadline.of(future);
        assertFalse(futureDeadline.isOverdue(now));
    }

    @Test
    @DisplayName("Kiểm tra hạn chót đang đến gần (isApproaching)")
    void shouldCheckApproachingCorrectly() {
        Instant now = Instant.now();
        Instant deadlineTime = now.plus(1, ChronoUnit.DAYS);

        Deadline deadline = Deadline.of(deadlineTime);
        assertTrue(deadline.isApproaching(now, Duration.ofDays(2)));
        assertFalse(deadline.isApproaching(now, Duration.ofHours(12)));
    }
}
