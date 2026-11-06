import { ref, computed } from 'vue';
import { useAiStore } from '../store/useAiStore.js';
import { useTaskStore } from '../store/useTaskStore.js';

export function useAiCopilot({ emit }) {
  const {
    state: aiState,
    decomposeTask,
    estimateDeadline,
    analyzePriority,
    generateSummary,
    runFullAnalysis
  } = useAiStore();

  const {
    state: taskState,
    addSubTasks,
    updateTaskInState
  } = useTaskStore();

  const selectedTaskId = ref(taskState.tasks[0]?.id || '');

  const selectedTask = computed(() => {
    return taskState.tasks.find((t) => t.id === selectedTaskId.value) || taskState.tasks[0];
  });

  async function handleDecompose() {
    if (!selectedTask.value) return;
    try {
      await decomposeTask(selectedTask.value.id);
      emit('notify', 'Checklist items generated.', 'success');
    } catch (err) {
      emit('notify', err.message, 'error');
    }
  }

  async function handleEstimate() {
    if (!selectedTask.value) return;
    try {
      await estimateDeadline(selectedTask.value.id);
      emit('notify', 'Deadline calculated.', 'success');
    } catch (err) {
      emit('notify', err.message, 'error');
    }
  }

  async function handlePriority() {
    if (!selectedTask.value) return;
    try {
      await analyzePriority(selectedTask.value.id);
      emit('notify', 'Priority evaluation completed.', 'success');
    } catch (err) {
      emit('notify', err.message, 'error');
    }
  }

  async function handleSummary() {
    if (!selectedTask.value) return;
    try {
      await generateSummary(selectedTask.value.id);
      emit('notify', 'Task summary generated.', 'success');
    } catch (err) {
      emit('notify', err.message, 'error');
    }
  }

  async function handleRunFullAudit() {
    if (!selectedTask.value) return;
    try {
      await runFullAnalysis(selectedTask.value.id);
      emit('notify', 'Full AI analysis completed.', 'success');
    } catch (err) {
      emit('notify', err.message, 'error');
    }
  }

  function applyDecompositionResult() {
    const res = aiState.activeAiResult;
    if (res?.data?.subTasks && selectedTask.value) {
      addSubTasks(selectedTask.value.id, res.data.subTasks);
      emit('notify', 'Subtasks applied to task.', 'success');
    }
  }

  function applyDeadlineResult() {
    const res = aiState.activeAiResult;
    if (res?.data?.recommendedDeadline && selectedTask.value) {
      updateTaskInState(selectedTask.value.id, { dueDate: res.data.recommendedDeadline });
      emit('notify', `Due date updated to ${res.data.recommendedDeadline}.`, 'success');
    }
  }

  function applyPriorityResult() {
    const res = aiState.activeAiResult;
    if (res?.data?.recommendedPriority && selectedTask.value) {
      updateTaskInState(selectedTask.value.id, { priority: res.data.recommendedPriority });
      emit('notify', `Priority updated to [${res.data.recommendedPriority}].`, 'success');
    }
  }

  function applySummaryResult() {
    const res = aiState.activeAiResult;
    if (res?.data?.summary && selectedTask.value) {
      updateTaskInState(selectedTask.value.id, { aiSummary: res.data.summary });
      emit('notify', 'Summary saved to task.', 'success');
    }
  }

  function applyFullAuditResult() {
    const res = aiState.activeAiResult;
    if (!res?.data || !selectedTask.value) return;

    const updates = {};
    if (res.data.deadlineEstimation?.recommendedDeadline) {
      updates.dueDate = res.data.deadlineEstimation.recommendedDeadline;
    }
    if (res.data.priorityRecommendation?.recommendedPriority) {
      updates.priority = res.data.priorityRecommendation.recommendedPriority;
    }
    if (res.data.summary?.summary) {
      updates.aiSummary = res.data.summary.summary;
    }
    if (Object.keys(updates).length > 0) {
      updateTaskInState(selectedTask.value.id, updates);
    }

    if (res.data.decomposition?.subTasks?.length) {
      addSubTasks(selectedTask.value.id, res.data.decomposition.subTasks);
    }
    emit('notify', 'All AI recommendations applied to task.', 'success');
  }

  return {
    aiState,
    taskState,
    selectedTaskId,
    selectedTask,
    handleDecompose,
    handleEstimate,
    handlePriority,
    handleSummary,
    handleRunFullAudit,
    applyDecompositionResult,
    applyDeadlineResult,
    applyPriorityResult,
    applySummaryResult,
    applyFullAuditResult
  };
}
