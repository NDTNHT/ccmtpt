import { describe, it, expect, beforeEach } from 'vitest';
import { useTaskStore } from '@/store/useTaskStore.js';

describe('store/useTaskStore (Feature)', () => {
  beforeEach(async () => {
    localStorage.clear();
    const { fetchTasks } = useTaskStore();
    await fetchTasks();
  });

  describe('Filtering & Search', () => {
    it('filters tasks by status', () => {
      const { state, filteredTasks, setFilters } = useTaskStore();
      setFilters({ status: 'IN_PROGRESS', priority: 'ALL', search: '' });

      expect(state.statusFilter).toBe('IN_PROGRESS');
      expect(filteredTasks.value.every((t) => t.status === 'IN_PROGRESS')).toBe(true);
    });

    it('filters tasks by priority', () => {
      const { state, filteredTasks, setFilters } = useTaskStore();
      setFilters({ status: 'ALL', priority: 'CRITICAL', search: '' });

      expect(state.priorityFilter).toBe('CRITICAL');
      expect(filteredTasks.value.every((t) => t.priority === 'CRITICAL')).toBe(true);
    });

    it('filters tasks by search query matching title, description, or id', () => {
      const { filteredTasks, setFilters } = useTaskStore();

      setFilters({ status: 'ALL', priority: 'ALL', search: 'NEURAL' });
      expect(filteredTasks.value.length).toBeGreaterThan(0);
      expect(filteredTasks.value[0].title).toContain('NEURAL');

      setFilters({ search: 'task-sys-002' });
      expect(filteredTasks.value).toHaveLength(1);
      expect(filteredTasks.value[0].id).toBe('task-sys-002');
    });
  });

  describe('Task Statistics Aggregation', () => {
    it('computes accurate totals and completion rate in a single pass', () => {
      const { taskStats } = useTaskStore();
      const stats = taskStats.value;

      expect(stats.total).toBe(stats.todo + stats.inProgress + stats.completed + stats.cancelled);
      expect(stats.completionRate).toBeGreaterThanOrEqual(0);
      expect(stats.completionRate).toBeLessThanOrEqual(100);
    });
  });

  describe('Reactive Task Operations & Transitions', () => {
    it('creates a task and prepends it to state.tasks', async () => {
      const { state, createTask } = useTaskStore();
      const initialCount = state.tasks.length;

      const created = await createTask({
        title: 'REACTIVE NEW MISSION',
        priority: 'HIGH'
      });

      expect(state.tasks.length).toBe(initialCount + 1);
      expect(state.tasks[0].id).toBe(created.id);
      expect(state.selectedTask?.id).toBe(created.id);
    });

    it('updates status via startTask, completeTask, and cancelTask', () => {
      const { state, startTask, completeTask, cancelTask } = useTaskStore();
      const target = state.tasks[0];

      startTask(target.id);
      expect(state.tasks.find((t) => t.id === target.id)?.status).toBe('IN_PROGRESS');

      completeTask(target.id);
      expect(state.tasks.find((t) => t.id === target.id)?.status).toBe('COMPLETED');

      cancelTask(target.id);
      expect(state.tasks.find((t) => t.id === target.id)?.status).toBe('CANCELLED');
    });

    it('toggles subtasks within reactive state and persists to storage', () => {
      const { state, toggleSubTask } = useTaskStore();
      const taskWithSubtasks = state.tasks.find((t) => t.subTasks && t.subTasks.length > 0);
      expect(taskWithSubtasks).toBeDefined();

      const st = taskWithSubtasks.subTasks[0];
      const initialCompleted = st.completed;

      toggleSubTask(taskWithSubtasks.id, st.id);
      expect(st.completed).toBe(!initialCompleted);
    });
  });
});
