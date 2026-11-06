import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useTaskDetail } from '@/composables/useTaskDetail.js';
import { useTaskStore } from '@/store/useTaskStore.js';

describe('composables/useTaskDetail (Feature)', () => {
  beforeEach(async () => {
    localStorage.clear();
    const { fetchTasks } = useTaskStore();
    await fetchTasks();
  });

  it('computes subtask counts and status colors properly', () => {
    const task = {
      id: 'task-test-detail',
      title: 'DETAIL MISSION',
      status: 'IN_PROGRESS',
      subTasks: [
        { id: 'st-1', title: 'Sub 1', completed: true },
        { id: 'st-2', title: 'Sub 2', completed: false }
      ]
    };
    const props = { task, isOpen: true };
    const emit = vi.fn();

    const composable = useTaskDetail({ props, emit });

    expect(composable.totalSubTasks.value).toBe(2);
    expect(composable.completedCount.value).toBe(1);
    expect(composable.statusColorClass.value).toBe('text-amber');
  });

  it('applies AI deadline recommendations directly to task state', () => {
    const { state } = useTaskStore();
    const targetTask = state.tasks[0];
    const props = { task: targetTask, isOpen: true };
    const emit = vi.fn();

    const composable = useTaskDetail({ props, emit });

    composable.aiFeedback.value = {
      timestamp: '12:00:00',
      text: 'Recommended Deadline: 2026-12-31',
      applyPayload: {
        type: 'DEADLINE',
        deadline: '2026-12-31'
      }
    };

    composable.applyAiRecommendation();

    expect(state.tasks[0].dueDate).toBe('2026-12-31');
    expect(emit).toHaveBeenCalledWith('notify', 'Due date updated to 2026-12-31.', 'success');
    expect(composable.aiFeedback.value).toBeNull();
  });

  it('applies AI priority recommendations directly to task state', () => {
    const { state } = useTaskStore();
    const targetTask = state.tasks[0];
    const props = { task: targetTask, isOpen: true };
    const emit = vi.fn();

    const composable = useTaskDetail({ props, emit });

    composable.aiFeedback.value = {
      timestamp: '12:00:00',
      text: 'Recommended Priority: CRITICAL',
      applyPayload: {
        type: 'PRIORITY',
        priority: 'CRITICAL'
      }
    };

    composable.applyAiRecommendation();

    expect(state.tasks[0].priority).toBe('CRITICAL');
    expect(emit).toHaveBeenCalledWith('notify', 'Priority updated to [CRITICAL].', 'success');
  });
});
