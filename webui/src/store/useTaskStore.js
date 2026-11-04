import { reactive, computed, ref, watch } from 'vue';
import { apiService } from '../services/apiService.js';
import { storageService } from '../services/storageService.js';

const backendOnline = ref(true);

const state = reactive({
  tasks: storageService.getTasks(),
  selectedTask: null,
  hoveredTask: null,
  statusFilter: 'ALL',
  priorityFilter: 'ALL',
  searchQuery: '',
  loading: false,
  error: null
});

// Auto-persist state.tasks changes to localStorage
watch(
  () => state.tasks,
  (newTasks) => {
    storageService.saveTasks(newTasks);
  },
  { deep: true }
);

export function useTaskStore() {
  const filteredTasks = computed(() => {
    return state.tasks.filter((t) => {
      const matchesStatus = state.statusFilter === 'ALL' || t.status === state.statusFilter;
      const matchesPriority = state.priorityFilter === 'ALL' || t.priority === state.priorityFilter;
      const q = state.searchQuery.trim().toLowerCase();
      const matchesQuery = !q ||
        t.title.toLowerCase().includes(q) ||
        (t.description && t.description.toLowerCase().includes(q)) ||
        t.id.toLowerCase().includes(q);
      return matchesStatus && matchesPriority && matchesQuery;
    });
  });

  const taskStats = computed(() => {
    const total = state.tasks.length;
    const todo = state.tasks.filter((t) => t.status === 'TODO').length;
    const inProgress = state.tasks.filter((t) => t.status === 'IN_PROGRESS').length;
    const completed = state.tasks.filter((t) => t.status === 'COMPLETED').length;
    const cancelled = state.tasks.filter((t) => t.status === 'CANCELLED').length;
    const completionRate = total > 0 ? Math.round((completed / total) * 100) : 0;
    return { total, todo, inProgress, completed, cancelled, completionRate };
  });

  async function fetchTasks() {
    state.loading = true;
    state.error = null;
    try {
      const data = await apiService.getTasks();
      backendOnline.value = true;
      if (Array.isArray(data) && data.length > 0) {
        state.tasks = data.map((t) => ({
          ...t,
          id: (t.taskId || t.id || `task-${Date.now()}`).toString(),
          subTasks: (t.subTasks || []).map((st, idx) => ({
            ...st,
            id: (st.subTaskId || st.id || `st-${idx}`).toString()
          }))
        }));
      }
    } catch (err) {
      console.warn('Local storage fetch error:', err.message);
      state.error = `LOCAL_STORAGE_ERR: ${err.message}`;
    } finally {
      state.loading = false;
    }
  }

  async function createTask(payload) {
    state.loading = true;
    try {
      const createdTask = await apiService.createTask(payload);
      state.tasks.unshift(createdTask);
      state.selectedTask = createdTask;
      return createdTask;
    } finally {
      state.loading = false;
    }
  }

  async function startTask(taskId) {
    try {
      const updated = await apiService.startTask(taskId);
      updateTaskInState(taskId, updated);
    } catch (err) {
      updateTaskInState(taskId, { status: 'IN_PROGRESS' });
    }
  }

  async function completeTask(taskId) {
    try {
      const updated = await apiService.completeTask(taskId);
      updateTaskInState(taskId, updated);
    } catch (err) {
      updateTaskInState(taskId, { status: 'COMPLETED' });
    }
  }

  async function cancelTask(taskId) {
    try {
      const updated = await apiService.cancelTask(taskId);
      updateTaskInState(taskId, updated);
    } catch (err) {
      updateTaskInState(taskId, { status: 'CANCELLED' });
    }
  }

  async function refreshPriority(taskId) {
    try {
      const updated = await apiService.refreshPriority(taskId);
      updateTaskInState(taskId, updated);
    } catch (err) {
      console.warn('Priority refresh locally handled');
    }
  }

  function updateTaskInState(taskId, partial) {
    const idx = state.tasks.findIndex((t) => t.id === taskId);
    if (idx !== -1) {
      state.tasks[idx] = { ...state.tasks[idx], ...partial };
      if (state.selectedTask && state.selectedTask.id === taskId) {
        state.selectedTask = state.tasks[idx];
      }
      if (state.hoveredTask && state.hoveredTask.id === taskId) {
        state.hoveredTask = state.tasks[idx];
      }
      storageService.saveTasks(state.tasks);
    }
  }

  function selectTask(task) {
    state.selectedTask = task ? { ...task } : null;
  }

  function setHoveredTask(task) {
    state.hoveredTask = task;
  }

  function toggleSubTask(taskId, subTaskId) {
    const task = state.tasks.find((t) => t.id === taskId);
    if (!task || !task.subTasks) return;
    const st = task.subTasks.find((s) => s.id === subTaskId);
    if (st) {
      st.completed = !st.completed;
      if (state.selectedTask && state.selectedTask.id === taskId) {
        state.selectedTask = { ...task };
      }
      storageService.saveTasks(state.tasks);
    }
  }

  function addSubTasks(taskId, newSubTasks) {
    const task = state.tasks.find((t) => t.id === taskId);
    if (!task) return;
    if (!task.subTasks) task.subTasks = [];
    const formatted = newSubTasks.map((title, i) => ({
      id: `st-ai-${Date.now()}-${i}`,
      title,
      completed: false
    }));
    task.subTasks.push(...formatted);
    if (state.selectedTask && state.selectedTask.id === taskId) {
      state.selectedTask = { ...task };
    }
    storageService.saveTasks(state.tasks);
  }

  function setFilters({ status, priority, search }) {
    if (status !== undefined) state.statusFilter = status;
    if (priority !== undefined) state.priorityFilter = priority;
    if (search !== undefined) state.searchQuery = search;
  }

  function exportDatabase() {
    storageService.exportDatabase();
  }

  function importDatabase(jsonString) {
    storageService.importDatabase(jsonString);
    state.tasks = storageService.getTasks();
    if (state.selectedTask) {
      state.selectedTask = state.tasks[0] || null;
    }
  }

  return {
    state,
    filteredTasks,
    taskStats,
    backendOnline,
    fetchTasks,
    createTask,
    startTask,
    completeTask,
    cancelTask,
    refreshPriority,
    selectTask,
    setHoveredTask,
    toggleSubTask,
    addSubTasks,
    updateTaskInState,
    setFilters,
    exportDatabase,
    importDatabase
  };
}
