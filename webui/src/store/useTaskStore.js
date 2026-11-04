import { reactive, computed, ref } from 'vue';

const backendOnline = ref(true);
import { apiService } from '../services/apiService.js';

const state = reactive({
  tasks: [
    {
      id: 'task-sys-001',
      title: 'CALIBRATE NEURAL LINK PROTOCOLS',
      description: 'Run automated parity and health checks against Anthropic/OpenAI compatible backend gateways.',
      status: 'IN_PROGRESS',
      priority: 'HIGH',
      dueDate: new Date(Date.now() + 86400000 * 2).toISOString().slice(0, 10),
      projectId: 'proj-omega-01',
      subTasks: [
        { id: 'st-01', title: 'Verify TLS Handshake on v1/models', completed: true },
        { id: 'st-02', title: 'Parse Token Consumption Stream', completed: true },
        { id: 'st-03', title: 'Benchmark Latency Ceiling (<350ms)', completed: false }
      ],
      aiSummary: 'Parity check underway. 2 of 3 verification benchmarks satisfied.'
    },
    {
      id: 'task-sys-002',
      title: 'SYNCHRONIZE TACTICAL INVENTORY MATRIX',
      description: 'Audit server cluster cooling modules and industrial power relays.',
      status: 'TODO',
      priority: 'CRITICAL',
      dueDate: new Date(Date.now() + 86400000 * 4).toISOString().slice(0, 10),
      projectId: 'proj-omega-01',
      subTasks: [
        { id: 'st-04', title: 'Inspect Cryogenic Heat Sinks', completed: false },
        { id: 'st-05', title: 'Measure Bus Voltage Under Load', completed: false }
      ],
      aiSummary: 'High priority alert: potential heat threshold breach detected.'
    },
    {
      id: 'task-sys-003',
      title: 'DEPLOY REFACTORED CLEAN ARCH CONTROLLER',
      description: 'Verify Spring Boot 3 endpoint mappings with JUnit 5 and verify complete output.',
      status: 'COMPLETED',
      priority: 'MEDIUM',
      dueDate: new Date(Date.now() - 86400000).toISOString().slice(0, 10),
      projectId: 'proj-omega-01',
      subTasks: [
        { id: 'st-06', title: 'Execute 145 Domain/Adapter Tests', completed: true },
        { id: 'st-07', title: 'Purge Temporary Sandbox Keys', completed: true }
      ],
      aiSummary: 'All unit test suites executed with 100% pass rate.'
    }
  ],
  selectedTask: null,
  hoveredTask: null,
  statusFilter: 'ALL',
  priorityFilter: 'ALL',
  searchQuery: '',
  loading: false,
  error: null
});

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
      console.warn('Backend tasks offline, maintaining local cache:', err.message);
      backendOnline.value = false;
      state.error = `OFFLINE MODE: ${err.message}`;
    } finally {
      state.loading = false;
    }
  }

  async function createTask(payload) {
    state.loading = true;
    try {
      const sanitizedTitle = (payload.title || '').trim();
      const sanitizedDescription = (payload.description || '').trim();
      
      let formattedDueDate = null;
      if (payload.dueDate) {
        try {
          const d = new Date(payload.dueDate);
          if (!isNaN(d.getTime())) {
            formattedDueDate = d.toISOString();
          }
        } catch (e) {
          console.warn('Invalid dueDate format:', payload.dueDate);
        }
      }

      const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
      const validProjectId = (payload.projectId && uuidRegex.test(payload.projectId)) 
        ? payload.projectId 
        : null;

      const apiPayload = {
        title: sanitizedTitle,
        description: sanitizedDescription,
        priority: payload.priority || 'MEDIUM',
        dueDate: formattedDueDate,
        projectId: validProjectId
      };

      let createdTask;
      try {
        createdTask = await apiService.createTask(apiPayload);
      } catch (err) {
        console.warn('Simulating task creation locally:', err.message);
        createdTask = {
          taskId: `task-local-${Date.now().toString().slice(-4)}`,
          title: sanitizedTitle.toUpperCase(),
          description: sanitizedDescription || 'Task created in offline mode',
          status: 'TODO',
          priority: payload.priority || 'MEDIUM',
          dueDate: formattedDueDate || new Date(Date.now() + 86400000 * 3).toISOString().slice(0, 10),
          projectId: validProjectId || null,
          subTasks: []
        };
      }

      const normalized = {
        ...createdTask,
        id: (createdTask.taskId || createdTask.id).toString(),
        subTasks: (payload.subTasks && payload.subTasks.length > 0)
          ? payload.subTasks.map((st, i) => {
              if (typeof st === 'string') {
                return { id: `st-${Date.now()}-${i}`, title: st, completed: false };
              }
              return {
                id: (st.subTaskId || st.id || `st-${Date.now()}-${i}`).toString(),
                title: st.title || 'Subtask',
                completed: !!st.completed
              };
            })
          : (createdTask.subTasks || [])
      };

      state.tasks.unshift(normalized);
      state.selectedTask = normalized;
      return normalized;
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
  }

  function setFilters({ status, priority, search }) {
    if (status !== undefined) state.statusFilter = status;
    if (priority !== undefined) state.priorityFilter = priority;
    if (search !== undefined) state.searchQuery = search;
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
    setFilters
  };
}
