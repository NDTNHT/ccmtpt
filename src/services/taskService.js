/**
 * Task Service - Local-first task and project management.
 * Provides reactive persistence, lifecycle transitions, and JSON backup/restore.
 */

import { calculatePriorityFromDueDate } from '../domain/taskRules.js';

const STORAGE_KEYS = {
  TASKS: 'tactical_tasks_v1',
  PROJECTS: 'tactical_projects_v1'
};

const DEFAULT_PROJECTS = [
  {
    id: 'proj-inbox',
    name: 'TACTICAL INBOX',
    description: 'Default incoming operational inbox',
    projectType: 'INBOX'
  },
  {
    id: 'proj-omega-01',
    name: 'OMEGA CYBERNETICS',
    description: 'Core neural systems, quantum telemetry & robotics',
    projectType: 'ACTIVE'
  },
  {
    id: 'proj-alpha-02',
    name: 'ALPHA CORE SYSTEMS',
    description: 'Infrastructure, edge security & communications',
    projectType: 'ACTIVE'
  }
];

const DEFAULT_TASKS = [
  {
    id: 'task-sys-001',
    title: 'CALIBRATE NEURAL LINK PROTOCOLS',
    description: 'Run automated parity and health checks against Anthropic/OpenAI/Gemini compatible client gateways.',
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
    description: 'Verify endpoint mappings and verify client-side domain persistence.',
    status: 'COMPLETED',
    priority: 'MEDIUM',
    dueDate: new Date(Date.now() - 86400000).toISOString().slice(0, 10),
    projectId: 'proj-omega-01',
    subTasks: [
      { id: 'st-06', title: 'Execute Domain Test Scenarios', completed: true },
      { id: 'st-07', title: 'Purge Temporary Sandbox Keys', completed: true }
    ],
    aiSummary: 'All test suites executed with 100% pass rate.'
  }
];

function readStorage(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return fallback;
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) && parsed.length > 0 ? parsed : fallback;
  } catch (e) {
    console.warn(`Failed to parse stored ${key}, restoring defaults:`, e);
    return fallback;
  }
}

function writeStorage(key, data) {
  try {
    localStorage.setItem(key, JSON.stringify(data));
  } catch (e) {
    console.error(`Failed to persist ${key} to localStorage:`, e);
  }
}

function findTaskIndex(tasks, id) {
  return tasks.findIndex(t => t.id === id || t.taskId === id);
}

export const taskService = {
  // Tasks Management
  getTasks() {
    return readStorage(STORAGE_KEYS.TASKS, DEFAULT_TASKS);
  },

  saveTasks(tasks) {
    writeStorage(STORAGE_KEYS.TASKS, tasks);
  },

  getTaskById(id) {
    const tasks = this.getTasks();
    const task = tasks.find(t => t.id === id || t.taskId === id);
    if (!task) {
      throw new Error(`Task not found: [${id}]`);
    }
    return task;
  },

  createTask(payload) {
    const tasks = this.getTasks();
    const newId = `task-${Date.now().toString().slice(-6)}`;
    const newTask = {
      id: newId,
      taskId: newId,
      title: (payload.title || 'UNTITLED MISSION').trim().toUpperCase(),
      description: (payload.description || '').trim(),
      status: 'TODO',
      priority: payload.priority || 'MEDIUM',
      dueDate: payload.dueDate || new Date(Date.now() + 86400000 * 3).toISOString().slice(0, 10),
      projectId: payload.projectId || 'proj-inbox',
      subTasks: (payload.subTasks || []).map((st, i) => ({
        id: (st.id || `st-${Date.now()}-${i}`).toString(),
        title: typeof st === 'string' ? st : (st.title || 'Subtask'),
        completed: typeof st === 'string' ? false : !!st.completed
      })),
      aiSummary: 'Task initialized in local tactical matrix.',
      createdAt: new Date().toISOString()
    };

    tasks.unshift(newTask);
    this.saveTasks(tasks);
    return newTask;
  },

  startTask(id) {
    return this.updateTask(id, { status: 'IN_PROGRESS' });
  },

  completeTask(id) {
    return this.updateTask(id, { status: 'COMPLETED' });
  },

  cancelTask(id) {
    return this.updateTask(id, { status: 'CANCELLED' });
  },

  updateTask(id, partial) {
    const tasks = this.getTasks();
    const idx = findTaskIndex(tasks, id);
    if (idx !== -1) {
      tasks[idx] = { ...tasks[idx], ...partial };
      this.saveTasks(tasks);
      return tasks[idx];
    }
    throw new Error(`Task not found: [${id}]`);
  },

  refreshPriority(id) {
    const tasks = this.getTasks();
    const idx = findTaskIndex(tasks, id);
    if (idx !== -1) {
      tasks[idx].priority = calculatePriorityFromDueDate(tasks[idx].dueDate);
      this.saveTasks(tasks);
      return tasks[idx];
    }
    throw new Error(`Task not found: [${id}]`);
  },

  toggleSubTask(taskId, subTaskId) {
    const tasks = this.getTasks();
    const idx = findTaskIndex(tasks, taskId);
    if (idx !== -1 && tasks[idx].subTasks) {
      const st = tasks[idx].subTasks.find(s => s.id === subTaskId);
      if (st) {
        st.completed = !st.completed;
        this.saveTasks(tasks);
        return tasks[idx];
      }
    }
    return tasks[idx] || null;
  },

  addSubTasks(taskId, newSubTasks) {
    const tasks = this.getTasks();
    const idx = findTaskIndex(tasks, taskId);
    if (idx !== -1) {
      if (!tasks[idx].subTasks) tasks[idx].subTasks = [];
      const formatted = newSubTasks.map((title, i) => ({
        id: `st-ai-${Date.now()}-${i}`,
        title,
        completed: false
      }));
      tasks[idx].subTasks.push(...formatted);
      this.saveTasks(tasks);
      return tasks[idx];
    }
    throw new Error(`Task not found: [${taskId}]`);
  },

  // Projects Management
  getProjects() {
    return readStorage(STORAGE_KEYS.PROJECTS, DEFAULT_PROJECTS);
  },

  saveProjects(projects) {
    writeStorage(STORAGE_KEYS.PROJECTS, projects);
  },

  createProject(payload) {
    const projects = this.getProjects();
    const newProj = {
      id: `proj-${Date.now().toString().slice(-4)}`,
      name: (payload.name || 'NEW PROJECT').trim().toUpperCase(),
      description: (payload.description || '').trim(),
      projectType: 'ACTIVE'
    };
    projects.push(newProj);
    this.saveProjects(projects);
    return newProj;
  },

  // Database Backup Export & Import
  exportDatabase() {
    const backup = {
      version: '1.0',
      exportedAt: new Date().toISOString(),
      tasks: this.getTasks(),
      projects: this.getProjects()
    };
    const blob = new Blob([JSON.stringify(backup, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `tactical_tasks_backup_${new Date().toISOString().slice(0, 10)}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  },

  importDatabase(jsonString) {
    try {
      const data = JSON.parse(jsonString);
      if (data && Array.isArray(data.tasks)) {
        this.saveTasks(data.tasks);
      }
      if (data && Array.isArray(data.projects)) {
        this.saveProjects(data.projects);
      }
      return true;
    } catch (e) {
      console.error('Failed to import database:', e);
      throw new Error('Invalid JSON backup file structure.');
    }
  }
};
