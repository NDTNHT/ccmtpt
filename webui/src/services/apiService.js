/**
 * API Service for connecting to Task Management Spring Boot backend.
 */

const BASE_URL = '/api';

async function request(endpoint, options = {}) {
  const url = `${BASE_URL}${endpoint}`;
  const config = {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  };

  try {
    const response = await fetch(url, config);
    if (!response.ok) {
      let errorMsg = `HTTP Error ${response.status}: ${response.statusText}`;
      try {
        const errorJson = await response.json();
        if (errorJson.message) {
          errorMsg = errorJson.message;
        }
      } catch (ignored) {
      }
      throw new Error(errorMsg);
    }
    const json = await response.json();
    return json.data !== undefined ? json.data : json;
  } catch (error) {
    console.error(`API Request Failure [${endpoint}]:`, error);
    throw error;
  }
}

export const apiService = {
  // Tasks Endpoints
  getTasks: () => request('/tasks'),
  getTaskById: (id) => request(`/tasks/${id}`),
  createTask: (payload) => request('/tasks', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  startTask: (id) => request(`/tasks/${id}/start`, { method: 'POST' }),
  completeTask: (id) => request(`/tasks/${id}/complete`, { method: 'POST' }),
  cancelTask: (id) => request(`/tasks/${id}/cancel`, { method: 'POST' }),
  refreshPriority: (id) => request(`/tasks/${id}/refresh-priority`, { method: 'POST' }),

  // Projects Endpoints
  getProjects: () => request('/projects'),
  createProject: (payload) => request('/projects', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),

  // AI Configuration & Providers
  getAiConfig: () => request('/tasks/ai/config'),
  updateAiConfig: (payload) => request('/tasks/ai/config', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  getProviders: () => request('/tasks/ai/providers'),
  registerProvider: (payload) => request('/tasks/ai/providers', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  activateProvider: (providerId) => request(`/tasks/ai/providers/${providerId}/activate`, {
    method: 'POST'
  }),
  verifyProvider: (payload) => request('/tasks/ai/providers/verify', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  verifyRegisteredProvider: (providerId) => request(`/tasks/ai/providers/${providerId}/verify`, {
    method: 'POST'
  }),

  // AI Task Copilot Endpoints
  analyzeTask: (taskId, customPrompt) => request(`/tasks/${taskId}/ai/analyze`, {
    method: 'POST',
    body: JSON.stringify({ customSystemPrompt: customPrompt })
  }),
  decomposeTask: (taskId, customPrompt) => request(`/tasks/${taskId}/ai/decompose`, {
    method: 'POST',
    body: JSON.stringify({ customSystemPrompt: customPrompt })
  }),
  suggestSubTasks: (taskId, customPrompt) => request(`/tasks/${taskId}/ai/suggest-subtasks`, {
    method: 'POST',
    body: JSON.stringify({ customSystemPrompt: customPrompt })
  }),
  estimateDeadline: (taskId, customPrompt) => request(`/tasks/${taskId}/ai/estimate-deadline`, {
    method: 'POST',
    body: JSON.stringify({ customSystemPrompt: customPrompt })
  }),
  analyzePriority: (taskId, customPrompt) => request(`/tasks/${taskId}/ai/priority`, {
    method: 'POST',
    body: JSON.stringify({ customSystemPrompt: customPrompt })
  }),
  generateSummary: (taskId, customPrompt) => request(`/tasks/${taskId}/ai/summary`, {
    method: 'POST',
    body: JSON.stringify({ customSystemPrompt: customPrompt })
  }),

  // AI Apply Endpoints
  applyDecomposition: (taskId, subTasks) => request(`/tasks/${taskId}/ai/apply-decomposition`, {
    method: 'POST',
    body: JSON.stringify({ subTasks })
  }),
  applyDeadline: (taskId, recommendedDeadline) => request(`/tasks/${taskId}/ai/apply-deadline`, {
    method: 'POST',
    body: JSON.stringify({ recommendedDeadline })
  }),
  applyPriority: (taskId, priority) => request(`/tasks/${taskId}/ai/apply-priority`, {
    method: 'POST',
    body: JSON.stringify({ priority })
  })
};
