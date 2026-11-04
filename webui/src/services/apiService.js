/**
 * API Service Adapter - 100% Client-Side Local-First Architecture.
 * Persists data to localStorage and connects directly to user-supplied AI APIs.
 * Eliminates remote backend cold boots, 404s, and server hosting requirements.
 */

import { storageService } from './storageService.js';
import { clientAiService } from './clientAiService.js';

function getTaskOrThrow(taskId) {
  const tasks = storageService.getTasks();
  const task = tasks.find(t => t.id === taskId || t.taskId === taskId);
  if (!task) {
    throw new Error(`Task not found in tactical local storage: [${taskId}]`);
  }
  return task;
}

export const apiService = {
  // Tasks Endpoints (Local-First Persistence)
  async getTasks() {
    return storageService.getTasks();
  },

  async getTaskById(id) {
    return getTaskOrThrow(id);
  },

  async createTask(payload) {
    const tasks = storageService.getTasks();
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
      subTasks: (payload.subTasks || []).map((st, i) => {
        if (typeof st === 'string') {
          return { id: `st-${Date.now()}-${i}`, title: st, completed: false };
        }
        return {
          id: (st.id || `st-${Date.now()}-${i}`).toString(),
          title: st.title || 'Subtask',
          completed: !!st.completed
        };
      }),
      aiSummary: 'Task initialized in local tactical matrix.',
      createdAt: new Date().toISOString()
    };

    tasks.unshift(newTask);
    storageService.saveTasks(tasks);
    return newTask;
  },

  async startTask(id) {
    const tasks = storageService.getTasks();
    const idx = tasks.findIndex(t => t.id === id || t.taskId === id);
    if (idx !== -1) {
      tasks[idx].status = 'IN_PROGRESS';
      storageService.saveTasks(tasks);
      return tasks[idx];
    }
    throw new Error(`Task not found: ${id}`);
  },

  async completeTask(id) {
    const tasks = storageService.getTasks();
    const idx = tasks.findIndex(t => t.id === id || t.taskId === id);
    if (idx !== -1) {
      tasks[idx].status = 'COMPLETED';
      storageService.saveTasks(tasks);
      return tasks[idx];
    }
    throw new Error(`Task not found: ${id}`);
  },

  async cancelTask(id) {
    const tasks = storageService.getTasks();
    const idx = tasks.findIndex(t => t.id === id || t.taskId === id);
    if (idx !== -1) {
      tasks[idx].status = 'CANCELLED';
      storageService.saveTasks(tasks);
      return tasks[idx];
    }
    throw new Error(`Task not found: ${id}`);
  },

  async refreshPriority(id) {
    const tasks = storageService.getTasks();
    const idx = tasks.findIndex(t => t.id === id || t.taskId === id);
    if (idx !== -1) {
      const task = tasks[idx];
      if (task.dueDate) {
        const diffDays = (new Date(task.dueDate) - new Date()) / (1000 * 60 * 60 * 24);
        if (diffDays < 1) task.priority = 'CRITICAL';
        else if (diffDays < 3) task.priority = 'HIGH';
        else if (diffDays < 7) task.priority = 'MEDIUM';
        else task.priority = 'LOW';
      }
      storageService.saveTasks(tasks);
      return tasks[idx];
    }
    throw new Error(`Task not found: ${id}`);
  },

  // Projects Endpoints
  async getProjects() {
    return storageService.getProjects();
  },

  async createProject(payload) {
    const projects = storageService.getProjects();
    const newProj = {
      id: `proj-${Date.now().toString().slice(-4)}`,
      name: (payload.name || 'NEW PROJECT').trim().toUpperCase(),
      description: (payload.description || '').trim(),
      projectType: 'ACTIVE'
    };
    projects.push(newProj);
    storageService.saveProjects(projects);
    return newProj;
  },

  // AI Configuration & Providers
  async getAiConfig() {
    const saved = storageService.getAiConfig() || {};
    const activeProviderId = saved.providerId || 'gemini';
    const apiKey = storageService.getAiKey(activeProviderId) || '';

    return {
      provider: activeProviderId.toUpperCase(),
      baseUrl: saved.baseUrl || (activeProviderId === 'gemini'
        ? 'https://generativelanguage.googleapis.com/v1beta'
        : 'https://api.openai.com/v1'),
      model: saved.model || (activeProviderId === 'gemini' ? 'gemini-2.5-flash' : 'gpt-4o-mini'),
      apiKey,
      timeoutSeconds: saved.timeoutSeconds || 15
    };
  },

  async updateAiConfig(payload) {
    const providerId = (payload.provider || 'gemini').toLowerCase();
    if (payload.apiKey !== undefined) {
      storageService.saveAiKey(providerId, payload.apiKey);
    }
    storageService.saveAiConfig({
      providerId,
      baseUrl: payload.baseUrl,
      model: payload.model,
      timeoutSeconds: payload.timeoutSeconds || 15
    });
    return payload;
  },

  async getProviders() {
    const savedConfig = storageService.getAiConfig() || {};
    const activeId = savedConfig.providerId || 'gemini';

    return [
      {
        id: 'gemini',
        name: 'Google Gemini API',
        protocol: 'GEMINI',
        baseUrl: 'https://generativelanguage.googleapis.com/v1beta',
        defaultModel: 'gemini-2.5-flash',
        availableModels: ['gemini-2.5-flash', 'gemini-2.5-pro', 'gemini-2.0-flash', 'gemini-1.5-flash', 'gemini-1.5-pro'],
        active: activeId === 'gemini'
      },
      {
        id: 'openai',
        name: 'OpenAI Direct API',
        protocol: 'OPENAI',
        baseUrl: 'https://api.openai.com/v1',
        defaultModel: 'gpt-4o-mini',
        availableModels: ['gpt-4o', 'gpt-4o-mini', 'o1-mini'],
        active: activeId === 'openai'
      },
      {
        id: 'anthropic',
        name: 'Anthropic Claude API',
        protocol: 'ANTHROPIC',
        baseUrl: 'https://api.anthropic.com/v1',
        defaultModel: 'claude-3-5-sonnet-20241022',
        availableModels: ['claude-3-5-sonnet-20241022', 'claude-3-5-haiku-20241022'],
        active: activeId === 'anthropic'
      },
      {
        id: 'pollinations',
        name: 'Pollinations AI Gateway',
        protocol: 'OPENAI',
        baseUrl: 'https://gen.pollinations.ai/v1',
        defaultModel: 'qwen/qwen3.7-flash',
        availableModels: ['qwen/qwen3.7-flash', 'openai/gpt-4o', 'anthropic/claude-3-5-sonnet', 'deepseek/deepseek-chat'],
        active: activeId === 'pollinations'
      }
    ];
  },

  async registerProvider(payload) {
    return payload;
  },

  async activateProvider(providerId) {
    const config = storageService.getAiConfig() || {};
    config.providerId = providerId.toLowerCase();
    storageService.saveAiConfig(config);
    return { success: true, activeProviderId: providerId };
  },

  async verifyProvider(payload) {
    const result = await clientAiService.verifyProvider(payload);
    // If successfully verified, save key in localStorage for current provider
    const config = storageService.getAiConfig() || {};
    const providerId = config.providerId || 'gemini';
    if (payload.apiKey) {
      storageService.saveAiKey(providerId, payload.apiKey);
    }
    return result;
  },

  async verifyRegisteredProvider(providerId) {
    const key = storageService.getAiKey(providerId);
    return clientAiService.verifyProvider({
      apiKey: key,
      protocol: providerId === 'gemini' ? 'GEMINI' : (providerId === 'anthropic' ? 'ANTHROPIC' : 'OPENAI')
    });
  },

  // AI Task Copilot Operations (Real-time Client Inference with User Key)
  async draftSubtasks(payload) {
    const pseudoTask = {
      title: payload.title,
      description: payload.description,
      status: 'TODO',
      priority: 'MEDIUM',
      subTasks: []
    };
    return clientAiService.decomposeTask(pseudoTask, payload.customSystemPrompt);
  },

  async decomposeTask(taskId, customPrompt) {
    const task = getTaskOrThrow(taskId);
    return clientAiService.decomposeTask(task, customPrompt);
  },

  async suggestSubTasks(taskId, customPrompt) {
    const task = getTaskOrThrow(taskId);
    return clientAiService.suggestSubTasks(task, customPrompt);
  },

  async estimateDeadline(taskId, customPrompt) {
    const task = getTaskOrThrow(taskId);
    return clientAiService.estimateDeadline(task, customPrompt);
  },

  async analyzePriority(taskId, customPrompt) {
    const task = getTaskOrThrow(taskId);
    return clientAiService.analyzePriority(task, customPrompt);
  },

  async generateSummary(taskId, customPrompt) {
    const task = getTaskOrThrow(taskId);
    return clientAiService.generateSummary(task, customPrompt);
  },

  async analyzeTask(taskId, customPrompt) {
    const task = getTaskOrThrow(taskId);
    return clientAiService.analyzeTask(task, customPrompt);
  },

  // AI Apply Endpoints (Instant LocalStorage Application)
  async applyDecomposition(taskId, subTasks) {
    const tasks = storageService.getTasks();
    const task = tasks.find(t => t.id === taskId || t.taskId === taskId);
    if (task) {
      if (!task.subTasks) task.subTasks = [];
      const newItems = subTasks.map((title, i) => ({
        id: `st-ai-${Date.now()}-${i}`,
        title,
        completed: false
      }));
      task.subTasks.push(...newItems);
      storageService.saveTasks(tasks);
      return task;
    }
    throw new Error(`Task not found: ${taskId}`);
  },

  async applyDeadline(taskId, recommendedDeadline) {
    const tasks = storageService.getTasks();
    const task = tasks.find(t => t.id === taskId || t.taskId === taskId);
    if (task) {
      task.dueDate = recommendedDeadline;
      storageService.saveTasks(tasks);
      return task;
    }
    throw new Error(`Task not found: ${taskId}`);
  },

  async applyPriority(taskId, priority) {
    const tasks = storageService.getTasks();
    const task = tasks.find(t => t.id === taskId || t.taskId === taskId);
    if (task) {
      task.priority = priority;
      storageService.saveTasks(tasks);
      return task;
    }
    throw new Error(`Task not found: ${taskId}`);
  }
};
