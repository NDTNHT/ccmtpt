/**
 * Storage Service - Local-first persistence using browser localStorage.
 * Enables 100% standalone, zero-backend execution with export/import capabilities.
 */

const STORAGE_KEYS = {
  TASKS: 'tactical_tasks_v1',
  PROJECTS: 'tactical_projects_v1',
  AI_KEYS: 'tactical_ai_keys_v1',
  AI_CONFIG: 'tactical_ai_config_v1',
  SYSTEM_PROMPT: 'tactical_ai_system_prompt_v1'
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

export const storageService = {
  // Tasks Persistence
  getTasks() {
    try {
      const raw = localStorage.getItem(STORAGE_KEYS.TASKS);
      if (!raw) {
        this.saveTasks(DEFAULT_TASKS);
        return DEFAULT_TASKS;
      }
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) && parsed.length > 0 ? parsed : DEFAULT_TASKS;
    } catch (e) {
      console.warn('Failed to parse stored tasks, restoring defaults:', e);
      return DEFAULT_TASKS;
    }
  },

  saveTasks(tasks) {
    try {
      localStorage.setItem(STORAGE_KEYS.TASKS, JSON.stringify(tasks));
    } catch (e) {
      console.error('Failed to persist tasks to localStorage:', e);
    }
  },

  // Projects Persistence
  getProjects() {
    try {
      const raw = localStorage.getItem(STORAGE_KEYS.PROJECTS);
      if (!raw) {
        this.saveProjects(DEFAULT_PROJECTS);
        return DEFAULT_PROJECTS;
      }
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) && parsed.length > 0 ? parsed : DEFAULT_PROJECTS;
    } catch (e) {
      console.warn('Failed to parse stored projects, restoring defaults:', e);
      return DEFAULT_PROJECTS;
    }
  },

  saveProjects(projects) {
    try {
      localStorage.setItem(STORAGE_KEYS.PROJECTS, JSON.stringify(projects));
    } catch (e) {
      console.error('Failed to persist projects to localStorage:', e);
    }
  },

  // AI API Keys (Stored per provider ID: { openai: '...', gemini: '...', anthropic: '...' })
  getAiKeys() {
    try {
      const raw = localStorage.getItem(STORAGE_KEYS.AI_KEYS);
      return raw ? JSON.parse(raw) : {};
    } catch (e) {
      return {};
    }
  },

  getAiKey(providerId) {
    const keys = this.getAiKeys();
    return keys[providerId] || '';
  },

  saveAiKey(providerId, apiKey) {
    try {
      const keys = this.getAiKeys();
      keys[providerId] = (apiKey || '').trim();
      localStorage.setItem(STORAGE_KEYS.AI_KEYS, JSON.stringify(keys));
    } catch (e) {
      console.error('Failed to save AI key to localStorage:', e);
    }
  },

  // AI Configuration
  getAiConfig() {
    try {
      const raw = localStorage.getItem(STORAGE_KEYS.AI_CONFIG);
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null;
    }
  },

  saveAiConfig(config) {
    try {
      localStorage.setItem(STORAGE_KEYS.AI_CONFIG, JSON.stringify(config));
    } catch (e) {
      console.error('Failed to save AI config to localStorage:', e);
    }
  },

  getSystemPrompt() {
    return localStorage.getItem(STORAGE_KEYS.SYSTEM_PROMPT);
  },

  saveSystemPrompt(prompt) {
    localStorage.setItem(STORAGE_KEYS.SYSTEM_PROMPT, prompt);
  },

  // Database Export & Import
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
