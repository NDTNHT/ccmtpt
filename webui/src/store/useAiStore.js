import { reactive, computed } from 'vue';
import { defineStore, storeToRefs } from 'pinia';
import { useTaskStore } from './useTaskStore.js';
import { mockAnalysis } from '../services/mockAi.js';
import { apiService } from '../services/apiService.js';
const DEFAULT_SYSTEM_PROMPT = `You are an AI Task Management Assistant.
Your role is to decompose complex tasks into clear, actionable subtasks, estimate realistic completion dates, suggest helpful implementation steps, and recommend priorities based on project impact.
Be concise, clear, and professional.`;
const useInternalAiStore = defineStore('ai', () => {
const state = reactive({
  config: {
    provider: 'POLLINATIONS',
    baseUrl: 'https://gen.pollinations.ai/v1',
    model: 'qwen/qwen3.7-flash',
    apiKey: '',
    timeoutSeconds: 15
  },
  providers: [
    {
      id: 'pollinations',
      name: 'Pollinations AI Gateway',
      protocol: 'OPENAI',
      baseUrl: 'https://gen.pollinations.ai/v1',
      defaultModel: 'qwen/qwen3.7-flash',
      availableModels: ['qwen/qwen3.7-flash', 'openai/gpt-4o', 'anthropic/claude-3-5-sonnet', 'deepseek/deepseek-chat'],
      active: true
    },
    {
      id: 'openai',
      name: 'OpenAI Direct API',
      protocol: 'OPENAI',
      baseUrl: 'https://api.openai.com/v1',
      defaultModel: 'gpt-4o-mini',
      availableModels: ['gpt-4o', 'gpt-4o-mini', 'o1-mini'],
      active: false
    },
    {
      id: 'anthropic',
      name: 'Anthropic Claude API',
      protocol: 'ANTHROPIC',
      baseUrl: 'https://api.anthropic.com/v1',
      defaultModel: 'claude-3-5-sonnet-20241022',
      availableModels: ['claude-3-5-sonnet-20241022', 'claude-3-5-haiku-20241022'],
      active: false
    },
    {
      id: 'gemini',
      name: 'Google Gemini API',
      protocol: 'GEMINI',
      baseUrl: 'https://generativelanguage.googleapis.com/v1beta',
      defaultModel: 'gemini-2.5-flash',
      availableModels: ['gemini-2.5-flash', 'gemini-2.5-pro', 'gemini-2.0-flash', 'gemini-1.5-flash', 'gemini-1.5-pro'],
      active: false
    }
  ],
  activeProviderId: 'pollinations',
  customSystemPrompt: DEFAULT_SYSTEM_PROMPT,
  telemetryLogs: [
    {
      timestamp: new Date().toLocaleTimeString(),
      type: 'INIT',
      status: 'SUCCESS',
      message: 'AI Task Assistant initialized and ready.'
    }
  ],
  verification: {
    loading: false,
    valid: false,
    discoveredModelsCount: 0,
    discoveredModelsList: [],
    lastChecked: null,
    latencyMs: 0,
    message: 'No API key configured. Enter API key to test connection.'
  },
  executingAiOp: false,
  activeAiResult: null
});
  const activeProvider = computed(() => {
    return state.providers.find((p) => p.id === state.activeProviderId) || state.providers[0];
  });
  function addLog(type, message, status = 'INFO') {
    state.telemetryLogs.unshift({
      timestamp: new Date().toLocaleTimeString(),
      type,
      message,
      status
    });
    if (state.telemetryLogs.length > 50) {
      state.telemetryLogs.pop();
    }
  }
  async function fetchConfig() {
    try {
      const data = await apiService.getAiConfig();
      if (data) {
        state.config = { ...state.config, ...data, apiKey: state.config.apiKey }; // Never send a masked server key back as a credential.
      }
    } catch {
      console.warn('Backend AI config fetch offline, using cached tactical config');
    }
  }
  async function fetchProviders() {
    try {
      const list = await apiService.getProviders();
      if (Array.isArray(list) && list.length > 0) {
        state.providers = list.map(p => ({
          ...p,
          availableModels: (p.availableModels && p.availableModels.length > 0)
            ? p.availableModels
            : (p.defaultModel ? [p.defaultModel] : [])
        }));
        const active = list.find((p) => p.active);
        if (active) state.activeProviderId = active.id;
      }
    } catch {
      console.warn('Backend AI providers fetch offline');
    }
  }
  async function activateProvider(providerId) {
    try {
      if (!useTaskStore().state.localMode) await apiService.activateProvider(providerId);
      state.activeProviderId = providerId;
      state.providers.forEach(p => { p.active = p.id === providerId; });
      const provider = state.providers.find(p => p.id === providerId);
      if (provider) state.config = { ...state.config, baseUrl: provider.baseUrl, model: provider.defaultModel, apiKey: provider.apiKey || '' };
      state.verification.valid = false;
      state.verification.discoveredModelsList = [];
      addLog('PROVIDER', `Selected provider [${providerId}]`, 'SUCCESS');
      return true;
    } catch (err) { addLog('PROVIDER', err.message, 'ERROR'); return false; }
  }
  async function registerCustomProvider(newProvider) {
    const exists = state.providers.find((p) => p.id === newProvider.id);
    if (!exists) {
      state.providers.push({
        ...newProvider,
        active: false,
        availableModels: newProvider.availableModels || [newProvider.defaultModel]
      });
      addLog('PROVIDER_REG', `Registered custom neural gateway: ${newProvider.name}`, 'SUCCESS');
      try {
        await apiService.registerProvider(newProvider);
      } catch {
        console.warn('Register provider local fallback');
      }
    }
  }
  async function verifyProvider(baseUrl, apiKey, protocol = 'OPENAI', timeoutSeconds = 15) {
    state.verification.loading = true;
    addLog('TEST_KEY', `Pinging verification gateway: ${baseUrl}/models...`, 'RUNNING');
    try {
      if (!apiKey || apiKey.trim() === '') {
        throw new Error('API Key is required to verify provider models.');
      }
      const result = await apiService.verifyProvider({ baseUrl, apiKey, protocol, timeoutSeconds });
      if (!result || !result.valid) {
        throw new Error(result?.message || 'Provider connection rejected by remote gateway.');
      }
      const models = result.availableModels || result.discoveredModels || [];
      state.verification = {
        loading: false,
        valid: true,
        discoveredModelsCount: models.length,
        discoveredModelsList: models,
        lastChecked: new Date().toLocaleTimeString(),
        latencyMs: result.latencyMs || 290,
        message: result.message || `Discovered ${models.length} models on endpoint.`
      };
      const provider = state.providers.find((p) => p.id === state.activeProviderId);
      if (provider) {
        if (models.length > 0) {
          provider.availableModels = models;
          if (!provider.availableModels.includes(state.config.model)) {
            state.config.model = models[0];
            provider.defaultModel = models[0];
          }
        }
      }
      const configured = { ...provider, baseUrl, apiKey, protocol, timeoutSeconds,
        defaultModel: state.config.model, availableModels: models };
      await apiService.registerProvider(configured);
      await apiService.activateProvider(configured.id);
      Object.assign(provider, configured);
      state.config = { ...state.config, baseUrl, apiKey, model: configured.defaultModel, timeoutSeconds };
      addLog('VERIFIED', `Key verified! ${state.verification.discoveredModelsCount} models discovered. Latency: ${state.verification.latencyMs}ms`, 'SUCCESS');
      return result;
    } catch (err) {
      state.verification = {
        loading: false,
        valid: false,
        discoveredModelsCount: 0,
        discoveredModelsList: [],
        lastChecked: new Date().toLocaleTimeString(),
        latencyMs: 0,
        message: `Handshake failed: ${err.message}`
      };
      addLog('VERIFY_FAIL', `Key verification rejected: ${err.message}`, 'ERROR');
      throw err;
    } finally {
      state.verification.loading = false;
    }
  }
  async function selectModel(modelName) {
    if (!modelName) return false;
    const provider = state.providers.find(p => p.id === state.activeProviderId);
    try {
      if (!useTaskStore().state.localMode) {
        if (provider?.apiKey) {
          await apiService.registerProvider({ ...provider, defaultModel: modelName });
        } else {
          await apiService.updateAiConfig({ model: modelName });
        }
      }
      state.config.model = modelName;
      if (provider) provider.defaultModel = modelName;
      addLog('MODEL_SELECT', `Active model [${modelName}]`, 'SUCCESS');
      return true;
    } catch (err) { addLog('MODEL_SELECT', err.message, 'ERROR'); return false; }
  }
  function setCustomPrompt(prompt) {
    state.customSystemPrompt = prompt;
    addLog('PROMPT_UPDATE', 'System prompt directive buffer revised.', 'INFO');
  }
  function resetCustomPrompt() {
    state.customSystemPrompt = DEFAULT_SYSTEM_PROMPT;
    addLog('PROMPT_RESET', 'System prompt reset to tactical baseline standard.', 'INFO');
  }
  // AI Operations
  async function runAiOp(opName, apiFn, taskId) {
    state.executingAiOp = true;
    addLog('AI_DISPATCH', `Dispatching ${opName} for Target [${taskId}]...`, 'RUNNING');
    try {
      const tasks = useTaskStore();
      const task = tasks.state.tasks.find(t => t.id === taskId);
      const result = tasks.state.localMode
        ? mockAnalysis(opName, task)
        : await apiFn(taskId, state.customSystemPrompt);
      state.activeAiResult = {
        type: opName,
        taskId,
        data: result,
        timestamp: new Date().toLocaleTimeString()
      };
      addLog('AI_RESOLVED', `${opName} completed successfully. Telemetry payload bound.`, 'SUCCESS');
      return result;
    } catch (error) {
      addLog('AI_ERR', `${opName} failed: ${error.message}`, 'ERROR');
      throw error;
    } finally {
      state.executingAiOp = false;
    }
  }
  async function decomposeTask(taskId) {
    return runAiOp('DECOMPOSE', apiService.decomposeTask, taskId);
  }
  async function suggestSubTasks(taskId) {
    return runAiOp('SUGGEST_SUBTASKS', apiService.suggestSubTasks, taskId);
  }
  async function estimateDeadline(taskId) {
    return runAiOp('ESTIMATE_DEADLINE', apiService.estimateDeadline, taskId);
  }
  async function analyzePriority(taskId) {
    return runAiOp('AUTO_PRIORITY', apiService.analyzePriority, taskId);
  }
  async function generateSummary(taskId) {
    return runAiOp('SUMMARY', apiService.generateSummary, taskId);
  }
  async function runFullAnalysis(taskId) {
    return runAiOp('FULL_ANALYSIS', apiService.analyzeTask, taskId);
  }
  return {
    state,
    activeProvider,
    addLog,
    fetchConfig,
    fetchProviders,
    activateProvider,
    registerCustomProvider,
    verifyProvider,
    selectModel,
    setCustomPrompt,
    resetCustomPrompt,
    decomposeTask,
    suggestSubTasks,
    estimateDeadline,
    analyzePriority,
    generateSummary,
    runFullAnalysis
  };
});
export function useAiStore() {
  const store = useInternalAiStore();
  return { ...store, ...storeToRefs(store), state: store.state };
}
