import { reactive, computed, ref, watch } from 'vue';
import { taskService } from '../services/taskService.js';

const backendOnline = ref(true);

const state = reactive({
  tasks: taskService.getTasks(),
  projects: taskService.getProjects(),
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
    taskService.saveTasks(newTasks);
  },
  { deep: true }
);

export function useTaskStore() {
  const filteredTasks = computed(() => {
    const q = state.searchQuery.trim().toLowerCase();
    return state.tasks.filter((t) => {
      const matchesStatus = state.statusFilter === 'ALL' || t.status === state.statusFilter;
      const matchesPriority = state.priorityFilter === 'ALL' || t.priority === state.priorityFilter;
      const matchesQuery = !q ||
        t.title.toLowerCase().includes(q) ||
        (t.description && t.description.toLowerCase().includes(q)) ||
        t.id.toLowerCase().includes(q);
      return matchesStatus && matchesPriority && matchesQuery;
    });
  });

  const taskStats = computed(() => {
    const total = state.tasks.length;
    let todo = 0;
    let inProgress = 0;
    let completed = 0;
    let cancelled = 0;

    for (const t of state.tasks) {
      if (t.status === 'TODO') todo++;
      else if (t.status === 'IN_PROGRESS') inProgress++;
      else if (t.status === 'COMPLETED') completed++;
      else if (t.status === 'CANCELLED') cancelled++;
    }

    const completionRate = total > 0 ? Math.round((completed / total) * 100) : 0;
    return { total, todo, inProgress, completed, cancelled, completionRate };
  });

  async function fetchTasks() {
    state.loading = true;
    state.error = null;
    try {
      state.tasks = taskService.getTasks();
      state.projects = taskService.getProjects();
      backendOnline.value = true;
    } catch (err) {
      console.warn('Error loading local tasks:', err.message);
      state.error = `STORAGE_ERR: ${err.message}`;
    } finally {
      state.loading = false;
    }
  }

  async function createTask(payload) {
    state.loading = true;
    try {
      const createdTask = taskService.createTask(payload);
      state.tasks.unshift(createdTask);
      state.selectedTask = createdTask;
      return createdTask;
    } finally {
      state.loading = false;
    }
  }

  function setTaskStatus(taskId, status) {
    try {
      const updated = taskService.updateTask(taskId, { status });
      updateTaskInState(taskId, updated);
    } catch {
      updateTaskInState(taskId, { status });
    }
  }

  const startTask = (taskId) => setTaskStatus(taskId, 'IN_PROGRESS');
  const completeTask = (taskId) => setTaskStatus(taskId, 'COMPLETED');
  const cancelTask = (taskId) => setTaskStatus(taskId, 'CANCELLED');

  async function refreshPriority(taskId) {
    try {
      const updated = taskService.refreshPriority(taskId);
      updateTaskInState(taskId, updated);
    } catch {
      console.warn('Priority refresh locally handled');
    }
  }

  function updateTaskInState(taskId, partial) {
    const idx = state.tasks.findIndex((t) => t.id === taskId);
    if (idx !== -1) {
      state.tasks[idx] = { ...state.tasks[idx], ...partial };
      if (state.selectedTask?.id === taskId) {
        state.selectedTask = state.tasks[idx];
      }
      if (state.hoveredTask?.id === taskId) {
        state.hoveredTask = state.tasks[idx];
      }
      taskService.saveTasks(state.tasks);
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
    if (!task?.subTasks) return;
    const st = task.subTasks.find((s) => s.id === subTaskId);
    if (st) {
      st.completed = !st.completed;
      if (state.selectedTask?.id === taskId) {
        state.selectedTask = { ...task };
      }
      taskService.saveTasks(state.tasks);
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
    if (state.selectedTask?.id === taskId) {
      state.selectedTask = { ...task };
    }
    taskService.saveTasks(state.tasks);
  }

  function setFilters({ status, priority, search }) {
    if (status !== undefined) state.statusFilter = status;
    if (priority !== undefined) state.priorityFilter = priority;
    if (search !== undefined) state.searchQuery = search;
  }

  function exportDatabase() {
    taskService.exportDatabase();
  }

  function importDatabase(jsonString) {
    taskService.importDatabase(jsonString);
    state.tasks = taskService.getTasks();
    state.projects = taskService.getProjects();
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
