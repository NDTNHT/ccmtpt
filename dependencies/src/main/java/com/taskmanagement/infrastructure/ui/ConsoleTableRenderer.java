package com.taskmanagement.infrastructure.ui;

import com.taskmanagement.adapter.ai.AiConfig;
import com.taskmanagement.adapter.ai.CustomProviderDefinition;
import com.taskmanagement.adapter.presenter.TaskLabels;
import com.taskmanagement.application.dto.ProjectOutputDTO;
import com.taskmanagement.application.dto.TaskOutputDTO;
import com.taskmanagement.application.dto.ai.AiAnalysisResultDto;
import com.taskmanagement.application.dto.ai.AiDeadlineEstimationDto;
import com.taskmanagement.application.dto.ai.AiPriorityRecommendationDto;
import com.taskmanagement.application.dto.ai.AiTaskDecompositionDto;
import com.taskmanagement.application.dto.ai.AiTaskSummaryDto;
import com.taskmanagement.application.dto.ai.ProviderVerificationResultDto;

import java.io.PrintStream;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Formats console tables, menus, and AI assistant analysis output.
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
        out.println("7. Tro ly AI (Decompose, Deadline, Subtasks, Summary, Auto-Priority)");
        out.println("0. Thoat");
        out.println("-----------------------------------------");
        if (currentTaskId != null) {
            out.println("Task hien tai: " + currentTaskId);
        }
    }

    public void renderAiMenu(UUID currentTaskId) {
        out.println("\n=========================================");
        out.println("          TRO LY AI TASK ASSISTANT       ");
        out.println("=========================================");
        if (currentTaskId != null) {
            out.println("Dang chon Task: " + currentTaskId);
        } else {
            out.println("Chua chon Task (he thong se yeu cau nhap ID hoac chon)");
        }
        out.println("1. Phan tich toan dien Task (Decompose + Estimate + Priority + Summary)");
        out.println("2. Boc tach Checklist (Subtasks) & Xem truoc");
        out.println("3. Goi y bo sung Subtasks con thieu");
        out.println("4. Du toan Thoi han hoan thanh (Deadline)");
        out.println("5. Danh gia do uu tien tu dong (Auto-Prioritize - Hybrid)");
        out.println("6. Tao ban Tom tat tien do Task (Summary)");
        out.println("7. Quan ly & Chuyen doi Provider (OpenAI / Anthropic / Custom)");
        out.println("8. Xem / Thay doi Cau hinh Provider hien tai");
        out.println("0. Quay lai menu chinh");
        out.println("-----------------------------------------");
    }

    public void renderAiConfig(AiConfig config) {
        out.println("\n--- [AI] CAU HINH HIEN TAI ---");
        if (config == null) {
            out.println("Chua co thong tin cau hinh.");
            return;
        }
        out.println("Provider   : " + config.getProvider());
        out.println("Base URL   : " + config.getBaseUrl());
        out.println("Model ID   : " + config.getModel());
        String maskedKey = (config.getApiKey() != null && !config.getApiKey().isBlank())
            ? (config.getApiKey().length() > 6
                ? config.getApiKey().substring(0, 3) + "..." + config.getApiKey().substring(config.getApiKey().length() - 3)
                : "***")
            : "(Not configured - API key required)";
        out.println("API Key    : " + maskedKey);
        out.println("Timeout (s): " + config.getTimeoutSeconds());
        out.println("----------------------------------");
    }

    public void renderProvidersList(List<CustomProviderDefinition> providers, CustomProviderDefinition active) {
        out.println("\n--- [AI] DANH SACH PROVIDERS DA DANG KY ---");
        if (providers == null || providers.isEmpty()) {
            out.println("Chua co provider nao duoc dang ky.");
            return;
        }

        out.printf("%-14s | %-20s | %-10s | %-22s | %-25s | %-8s | %-7s%n",
            "ID", "Ten Provider", "Chuan", "Model", "Base URL", "Discov", "Active");
        out.println("--------------------------------------------------------------------------------------------------------------------");
        for (CustomProviderDefinition p : providers) {
            boolean isActive = active != null && p.id().equalsIgnoreCase(active.id());
            String discov = p.availableModels() != null && !p.availableModels().isEmpty()
                ? p.availableModels().size() + " mods"
                : "-";
            out.printf("%-14s | %-20s | %-10s | %-22s | %-25s | %-8s | %-7s%n",
                p.id(),
                truncate(p.name(), 20),
                p.protocol(),
                truncate(p.defaultModel(), 22),
                truncate(p.baseUrl(), 25),
                discov,
                isActive ? "[*]" : "");
        }
        out.println("--------------------------------------------------------------------------------------------------------------------");
    }

    public void renderVerificationResult(ProviderVerificationResultDto result) {
        out.println("\n--- [AI] KET QUA KIEM TRA PROVIDER & MODELS ---");
        if (result == null) {
            out.println("Khong co ket qua kiem tra.");
            return;
        }
        out.printf("Trang thai : %s (HTTP %d)%n", result.valid() ? "HOP LE / THANH CONG" : "THAT BAI / KHONG HOP LE", result.statusCode());
        out.printf("Chuan API  : %s%n", result.protocol());
        out.printf("Thong bao  : %s%n", result.message());
        if (!result.availableModels().isEmpty()) {
            out.printf("Danh sach Models tim thay (%d models):%n", result.availableModels().size());
            int limit = Math.min(result.availableModels().size(), 30);
            for (int i = 0; i < limit; i++) {
                out.printf("  [%2d] %s%n", i + 1, result.availableModels().get(i));
            }
            if (result.availableModels().size() > limit) {
                out.printf("  ... va con %d models khac.%n", result.availableModels().size() - limit);
            }
        } else {
            out.println("Khong tim thay model nao trong danh sach phan hoi.");
        }
        out.println("-------------------------------------------------");
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

    public void renderAiDecomposition(AiTaskDecompositionDto decomposition) {
        out.println("\n--- [AI] KET QUA BOC TACH CHECKLIST (SUBTASKS) ---");
        out.println("Ly do / Danh gia: " + decomposition.rationale());
        out.println("Cac dau viec duoc de xuat:");
        List<String> items = decomposition.subTasks();
        if (items.isEmpty()) {
            out.println("  (Khong co dau viec nao)");
        } else {
            for (int i = 0; i < items.size(); i++) {
                out.printf("  [%d] %s%n", i + 1, items.get(i));
            }
        }
    }

    public void renderAiDeadlineEstimation(AiDeadlineEstimationDto estimation) {
        out.println("\n--- [AI] DU TOAN THOI HAN HOAN THANH ---");
        out.printf("So gio uoc tinh: %d gio%n", estimation.estimatedHours());
        out.printf("Han chot de xuat: %s%n", estimation.recommendedDeadline());
        out.printf("Muc do tin cay: %s%n", estimation.confidence());
        out.printf("Giai thich: %s%n", estimation.explanation());
    }

    public void renderAiPriorityRecommendation(AiPriorityRecommendationDto priority) {
        out.println("\n--- [AI] DANH GIA DO UU TIEN TU DONG (HYBRID) ---");
        out.printf("AI de xuat (Ngữ nghĩa): %s%n", priority.aiProposedPriority());
        out.printf("Domain Service tinh toan: %s%n", priority.domainCalculatedPriority());
        out.printf("-> Do uu tien ap dung (Effective): %s%n", priority.effectivePriority());
        out.printf("Ly do: %s%n", priority.reasoning());
    }

    public void renderAiSummary(AiTaskSummaryDto summary) {
        out.println("\n--- [AI] TOM TAT TIEN DO TAC VU ---");
        out.printf("Tieu de: %s%n", summary.title());
        out.printf(Locale.US, "Ty le hoan thanh: %.1f%%%n", summary.completionRate());
        out.println("Noi dung tom tat: " + summary.summary());
        if (!summary.keyHighlights().isEmpty()) {
            out.println("Diem noi bat:");
            for (String h : summary.keyHighlights()) {
                out.println("  • " + h);
            }
        }
    }

    public void renderAiAnalysisResult(AiAnalysisResultDto result) {
        out.println("\n=======================================================");
        out.println("       KET QUA PHAN TICH TOAN DIEN BANG AI             ");
        out.println("=======================================================");
        renderAiDecomposition(result.decomposition());
        renderAiDeadlineEstimation(result.deadlineEstimation());
        renderAiPriorityRecommendation(result.priorityRecommendation());
        renderAiSummary(result.summary());
        out.println("=======================================================");
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
