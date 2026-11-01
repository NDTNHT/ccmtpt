// Explicit local demonstration: deterministic rules, no external AI request.
export function draftChecklist(title, description = '') {
  const lines = description.split(/\n|;/).map(s => s.replace(/^\s*[-*\d.)]+\s*/, '').trim()).filter(s => s.length >= 3);
  return [...new Set(lines.length >= 2 ? lines : [
    `Làm rõ yêu cầu: ${title}`, `Thực hiện: ${title}`, `Kiểm tra kết quả: ${title}`, 'Tổng kết và ghi chép'
  ])].map(s => s.slice(0, 200)).slice(0, 12);
}
export function mockAnalysis(operation, task) {
  if (!task) throw new Error('Hãy chọn một công việc.');
  const subTasks = draftChecklist(task.title, task.description);
  const existing = task.subTasks || [];
  const completed = existing.filter(s => s.completed).length;
  const completionPercentage = existing.length ? Math.round(completed / existing.length * 100) : task.status === 'DONE' ? 100 : 0;
  const remaining = Math.max(existing.length - completed, 1);
  const estimatedHours = remaining * 4;
  const decomposition = { taskId: task.id, subTasks, rationale: 'MOCK LOCAL: tách các dòng mô tả hoặc dùng các bước mẫu.' };
  const deadlineEstimation = { taskId: task.id, estimatedHours,
    recommendedDeadline: new Date(Date.now() + estimatedHours * 3600000).toISOString(),
    confidence: 'LOW', explanation: 'MOCK LOCAL: 4 giờ cho mỗi đầu việc còn lại; cần người dùng kiểm tra.' };
  const priorityRecommendation = { taskId: task.id, effectivePriority: task.priority,
    reasoning: 'MOCK LOCAL: giữ nguyên mức ưu tiên do người dùng chọn.' };
  const summary = { taskId: task.id, summary: `MOCK LOCAL: ${completed}/${existing.length} checklist hoàn thành (${completionPercentage}%). Trạng thái: ${task.status}.`,
    completionPercentage, highlights: [`Còn ${existing.length - completed} đầu việc`, task.dueDate ? `Hạn: ${task.dueDate}` : 'Chưa đặt thời hạn'] };
  return { DECOMPOSE: decomposition, SUGGEST_SUBTASKS: decomposition, ESTIMATE_DEADLINE: deadlineEstimation,
    AUTO_PRIORITY: priorityRecommendation, SUMMARY: summary,
    FULL_ANALYSIS: { taskId: task.id, decomposition, deadlineEstimation, priorityRecommendation, summary } }[operation];
}
