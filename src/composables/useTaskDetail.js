import { ref, computed } from 'vue';
import { useAiStore } from '../store/useAiStore.js';
import { useTaskStore } from '../store/useTaskStore.js';
import { getStatusColorClass } from '../domain/taskRules.js';

export function useTaskDetail({ props, emit }) {
  const {
    decomposeTask,
    estimateDeadline,
    analyzePriority,
    generateSummary
  } = useAiStore();

  const { addSubTasks, updateTaskInState } = useTaskStore();

  const aiLoading = ref(false);
  const aiFeedback = ref(null);
  const newSubTaskTitle = ref('');

  const totalSubTasks = computed(() => props.task?.subTasks?.length || 0);
  const completedCount = computed(() => {
    return props.task?.subTasks?.filter((s) => s.completed).length || 0;
  });

  const statusColorClass = computed(() => {
    return props.task ? getStatusColorClass(props.task.status) : '';
  });

  function onAddSubTask() {
    if (!newSubTaskTitle.value.trim() || !props.task) return;
    addSubTasks(props.task.id, [newSubTaskTitle.value.trim()]);
    newSubTaskTitle.value = '';
  }

  async function onAiDecompose() {
    if (!props.task) return;
    aiLoading.value = true;
    try {
      const res = await decomposeTask(props.task.id, props.task.title);
      aiFeedback.value = {
        timestamp: new Date().toLocaleTimeString(),
        text: `Generated ${res.subTasks.length} checklist items. ${res.rationale || ''}`,
        applyPayload: {
          type: 'DECOMPOSE',
          items: res.subTasks
        }
      };
      emit('notify', `AI generated ${res.subTasks.length} subtasks!`, 'success');
    } catch (err) {
      emit('notify', `AI decomposition error: ${err.message}`, 'error');
    } finally {
      aiLoading.value = false;
    }
  }

  async function onAiEstimateDeadline() {
    if (!props.task) return;
    aiLoading.value = true;
    try {
      const res = await estimateDeadline(props.task.id);
      aiFeedback.value = {
        timestamp: new Date().toLocaleTimeString(),
        text: `Recommended Deadline: ${res.recommendedDeadline} (Est. ${res.estimatedHours}h, Confidence: ${Math.round(res.confidenceScore * 100)}%). ${res.reasoning || ''}`,
        applyPayload: {
          type: 'DEADLINE',
          deadline: res.recommendedDeadline
        }
      };
      emit('notify', `AI Estimated deadline: ${res.recommendedDeadline}`, 'success');
    } catch (err) {
      emit('notify', `AI estimation error: ${err.message}`, 'error');
    } finally {
      aiLoading.value = false;
    }
  }

  async function onAiAnalyzePriority() {
    if (!props.task) return;
    aiLoading.value = true;
    try {
      const res = await analyzePriority(props.task.id);
      aiFeedback.value = {
        timestamp: new Date().toLocaleTimeString(),
        text: `Recommended Priority: [${res.recommendedPriority}] (Urgency: ${res.urgencyScore}/100, Impact: ${res.impactScore}/100). ${res.reasoning || ''}`,
        applyPayload: {
          type: 'PRIORITY',
          priority: res.recommendedPriority
        }
      };
      emit('notify', `AI Recommended priority: ${res.recommendedPriority}`, 'success');
    } catch (err) {
      emit('notify', `AI priority analysis error: ${err.message}`, 'error');
    } finally {
      aiLoading.value = false;
    }
  }

  async function onAiGenerateSummary() {
    if (!props.task) return;
    aiLoading.value = true;
    try {
      const res = await generateSummary(props.task.id);
      aiFeedback.value = {
        timestamp: new Date().toLocaleTimeString(),
        text: `Summary: ${res.summary}`,
        applyPayload: {
          type: 'SUMMARY',
          summary: res.summary
        }
      };
      emit('notify', 'AI summary generated successfully', 'info');
    } catch (err) {
      emit('notify', `AI summary error: ${err.message}`, 'error');
    } finally {
      aiLoading.value = false;
    }
  }

  function applyAiRecommendation() {
    if (!aiFeedback.value?.applyPayload || !props.task) return;
    const { type, items, deadline, priority, summary } = aiFeedback.value.applyPayload;

    if (type === 'DECOMPOSE' && items) {
      addSubTasks(props.task.id, items);
      emit('notify', 'Subtasks added to task.', 'success');
    } else if (type === 'DEADLINE' && deadline) {
      updateTaskInState(props.task.id, { dueDate: deadline });
      emit('notify', `Due date updated to ${deadline}.`, 'success');
    } else if (type === 'PRIORITY' && priority) {
      updateTaskInState(props.task.id, { priority });
      emit('notify', `Priority updated to [${priority}].`, 'success');
    } else if (type === 'SUMMARY' && summary) {
      updateTaskInState(props.task.id, { aiSummary: summary });
      emit('notify', 'AI summary added to task.', 'success');
    }
    aiFeedback.value = null;
  }

  return {
    aiLoading,
    aiFeedback,
    newSubTaskTitle,
    totalSubTasks,
    completedCount,
    statusColorClass,
    onAddSubTask,
    onAiDecompose,
    onAiEstimateDeadline,
    onAiAnalyzePriority,
    onAiGenerateSummary,
    applyAiRecommendation
  };
}

