package com.taskmanagement.infrastructure.ui;

import com.taskmanagement.domain.task.enums.Priority;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;
import java.util.UUID;

/**
 * Reads and parses console input.
 */
public class ConsoleInputReader {
    private final Scanner scanner;

    public ConsoleInputReader(Scanner scanner) {
        this.scanner = scanner != null ? scanner : new Scanner(System.in);
    }

    public String readString(String prompt) {
        if (prompt != null && !prompt.isEmpty()) {
            System.out.print(prompt);
        }
        return scanner.nextLine().trim();
    }

    public UUID readTaskId(String prompt, UUID defaultId) {
        if (defaultId != null) {
            System.out.print("Nhan Enter de dung Task hien tai hoac nhap UUID moi: ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) return defaultId;
            try {
                return UUID.fromString(line);
            } catch (IllegalArgumentException e) {
                System.out.println("Dinh dang UUID khong hop le.");
                return null;
            }
        } else {
            System.out.print("Nhap UUID cua Task can " + prompt + ": ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                System.out.println("UUID khong duoc de trong.");
                return null;
            }
            try {
                return UUID.fromString(line);
            } catch (IllegalArgumentException e) {
                System.out.println("Dinh dang UUID khong hop le.");
                return null;
            }
        }
    }

    public Instant readDueDate() {
        System.out.println("Chon thoi han:");
        System.out.println("  1. Ngay mai");
        System.out.println("  2. Sau 3 ngay");
        System.out.println("  3. Da qua han");
        System.out.println("  4. Khong dat deadline");
        System.out.println("  5. Nhap ngay thu cong");
        System.out.print("Lua chon (1-5): ");
        String dChoice = scanner.nextLine().trim();

        return switch (dChoice) {
            case "2" -> Instant.now().plus(3, ChronoUnit.DAYS);
            case "3" -> Instant.now().minus(1, ChronoUnit.DAYS);
            case "4" -> null;
            case "5" -> {
                System.out.print("Nhap ngay (yyyy-MM-dd, dd/MM/yyyy, hoac so ngay +/-N): ");
                yield parseCustomDate(scanner.nextLine());
            }
            default -> Instant.now().plus(1, ChronoUnit.DAYS);
        };
    }

    public Priority readPriority() {
        System.out.println("Chon muc uu tien:");
        System.out.println("  1. LOW");
        System.out.println("  2. MEDIUM");
        System.out.println("  3. HIGH");
        System.out.println("  4. Tu dong");
        System.out.print("Lua chon (1-4): ");
        String pChoice = scanner.nextLine().trim();

        return switch (pChoice) {
            case "1" -> Priority.LOW;
            case "2" -> Priority.MEDIUM;
            case "3" -> Priority.HIGH;
            default -> null;
        };
    }

    private Instant parseCustomDate(String input) {
        if (input == null || input.trim().isEmpty()) {
            return Instant.now().plus(1, ChronoUnit.DAYS);
        }
        String trimmed = input.trim();
        // Case 1: Relative integer days (+5, -2, 3)
        try {
            int relativeDays = Integer.parseInt(trimmed.replace("+", ""));
            return Instant.now().plus(relativeDays, ChronoUnit.DAYS);
        } catch (NumberFormatException ignored) {}

        // Case 2: yyyy-MM-dd
        try {
            LocalDate date = LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
            return date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception ignored) {}

        // Case 3: dd/MM/yyyy
        try {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate date = LocalDate.parse(trimmed, dtf);
            return date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception ignored) {}

        System.out.println("Dinh dang ngay khong hop le, tu dong gan mac dinh ngay mai.");
        return Instant.now().plus(1, ChronoUnit.DAYS);
    }
}
