package com.taskmanagement.adapter.ai;

import com.taskmanagement.adapter.presenter.TaskLabels;
import com.taskmanagement.application.ai.dto.AiPriorityProposalOutputDTO;
import com.taskmanagement.application.ai.dto.AiSubTaskProposalOutputDTO;
import com.taskmanagement.application.ai.dto.AiSummaryOutputDTO;
import com.taskmanagement.application.ai.dto.AiTaskBreakdownOutputDTO;

import java.io.PrintStream;
import java.util.List;

/**
 * Presenter for rendering AI outputs in the console interface.
 */
public class AiConsolePresenter {
    private final PrintStream out;

    public AiConsolePresenter() {
        this(System.out);
    }

    public AiConsolePresenter(PrintStream out) {
        this.out = out != null ? out : System.out;
    }

    public void renderBreakdown(AiTaskBreakdownOutputDTO breakdown) {
        out.println("\n========================================================");
        out.println("            KET QUA CHIA NHO TAC VU (AI BREAKDOWN)      ");
        out.println("========================================================");
        out.println("Task ID: " + breakdown.taskId());
        out.println("Trang thai ap dung: " + (breakdown.applied() ? "DA LUU VAO TASK" : "XEM TRUOC (CHUA LUU)"));
        out.println("Han chot du doan: " + (breakdown.estimatedDeadline() != null ? breakdown.estimatedDeadline() : "Khong xac dinh"));
        out.println("Ly do / Phuong phap: " + breakdown.reasoning());
        out.println("--------------------------------------------------------");
        out.println("Danh sach Subtask de xuat:");

        List<AiSubTaskProposalOutputDTO> subtasks = breakdown.subtasks();
        if (subtasks == null || subtasks.isEmpty()) {
            out.println("  (Khong co subtask nao duoc tao)");
        } else {
            for (int i = 0; i < subtasks.size(); i++) {
                AiSubTaskProposalOutputDTO item = subtasks.get(i);
                out.printf("  [%d] [ ] %s%n", (i + 1), item.title());
                if (item.explanation() != null && !item.explanation().isBlank()) {
                    out.printf("          Chi tiet: %s%n", item.explanation());
                }
            }
        }
        out.println("========================================================\n");
    }

    public void renderPriorityProposal(AiPriorityProposalOutputDTO proposal) {
        out.println("\n========================================================");
        out.println("         DE XUAT DO UU TIEN THONG MINH (AI PRIORITY)    ");
        out.println("========================================================");
        out.println("Task ID: " + proposal.taskId());
        out.println("Do uu tien de xuat: " + TaskLabels.priority(proposal.suggestedPriority()));
        out.printf("Do tin cay (Confidence): %.1f%%%n", proposal.confidence() * 100);
        out.println("Trang thai ap dung: " + (proposal.applied() ? "DA CAP NHAT DO UU TIEN" : "XEM TRUOC (CHUA LUU)"));
        out.println("Ly do: " + proposal.reasoning());
        out.println("========================================================\n");
    }

    public void renderSummary(AiSummaryOutputDTO summary) {
        out.println("\n========================================================");
        out.println("              TOM TAT TAC VU THONG MINH (AI SUMMARY)    ");
        out.println("========================================================");
        out.println("Task ID: " + summary.taskId());
        out.println("Tieu de: " + summary.title());
        out.println("--------------------------------------------------------");
        out.println("Tom tat noi dung:");
        out.println("  " + summary.summary());
        out.println("--------------------------------------------------------");
        out.println("Diem noi bat (Key Highlights):");
        if (summary.keyHighlights() == null || summary.keyHighlights().isEmpty()) {
            out.println("  (Khong co diem noi bat)");
        } else {
            for (String highlight : summary.keyHighlights()) {
                out.println("  * " + highlight);
            }
        }
        out.println("--------------------------------------------------------");
        out.println("Buoc tiep theo khuyen nghi (Next Step):");
        out.println("  -> " + summary.suggestedNextStep());
        out.println("========================================================\n");
    }
}
