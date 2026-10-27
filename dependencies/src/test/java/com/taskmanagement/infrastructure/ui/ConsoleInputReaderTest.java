package com.taskmanagement.infrastructure.ui;

import com.taskmanagement.domain.task.enums.Priority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Scanner;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests for ConsoleInputReader")
class ConsoleInputReaderTest {

    private ConsoleInputReader createReader(String input) {
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new ConsoleInputReader(new Scanner(in));
    }

    @Test
    @DisplayName("Doc chuoi ky tu co ban va trim khoang trang")
    void shouldReadStringAndTrim() {
        ConsoleInputReader reader = createReader("   Task Tieu De   \n");
        assertEquals("Task Tieu De", reader.readString("Prompt: "));
    }

    @Test
    @DisplayName("Doc TaskId - Nhan Enter dung ID mac dinh")
    void shouldReturnDefaultTaskIdOnEmptyInput() {
        UUID defaultId = UUID.randomUUID();
        ConsoleInputReader reader = createReader("\n");
        assertEquals(defaultId, reader.readTaskId("start", defaultId));
    }

    @Test
    @DisplayName("Doc TaskId - Nhap UUID hop le")
    void shouldReadValidUUID() {
        UUID inputId = UUID.randomUUID();
        ConsoleInputReader reader = createReader(inputId.toString() + "\n");
        assertEquals(inputId, reader.readTaskId("start", null));
    }

    @Test
    @DisplayName("Doc TaskId - Nhap chuoi khong hop le tra ve null")
    void shouldReturnNullOnInvalidUUID() {
        ConsoleInputReader reader = createReader("invalid-uuid\n");
        assertNull(reader.readTaskId("start", null));
    }

    @Test
    @DisplayName("Doc Priority theo lua chon")
    void shouldReadPriorityChoices() {
        assertEquals(Priority.LOW, createReader("1\n").readPriority());
        assertEquals(Priority.MEDIUM, createReader("2\n").readPriority());
        assertEquals(Priority.HIGH, createReader("3\n").readPriority());
        assertNull(createReader("4\n").readPriority());
    }

    @Test
    @DisplayName("Doc DueDate - Lua chon 4 khong dat deadline")
    void shouldReadNullDueDateWhenOption4() {
        ConsoleInputReader reader = createReader("4\n");
        assertNull(reader.readDueDate());
    }

    @Test
    @DisplayName("Doc DueDate - Lua chon 1 dat ngay mai")
    void shouldReadDueDateTomorrow() {
        ConsoleInputReader reader = createReader("1\n");
        Instant due = reader.readDueDate();
        assertNotNull(due);
        assertTrue(due.isAfter(Instant.now()));
    }
}
