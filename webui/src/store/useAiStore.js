import { reactive, computed } from 'vue';
import { apiService } from '../services/apiService.js';
import { storageService } from '../services/storageService.js';

const DEFAULT_SYSTEM_PROMPT = `You are a Tactical AI Task Management Assistant.
Your mission is to decompose operational objectives into clear, actionable checklist subtasks, estimate realistic completion timelines and effort, evaluate priority and risk levels, and produce executive briefings.
Be concise, clear, and structured in your responses.`;

const initialSavedPrompt = storageService.getSystemPrompt() || DEFAULT_SYSTEM_PROMPT;
const initialSavedConfig = storageService.getAiConfig() || {};
const initialActiveProviderId = initialSavedConfig.providerId || 'gemini';
const initialSavedKey = storageService.getAiKey(initialActiveProviderId);

const state = reactive({
  config: {
    provider: initialActiveProviderId.toUpperCase(),
    baseUrl: initialSavedConfig.baseUrl || 'https://generativelanguage.googleapis.com/v1beta',
    model: initialSavedConfig.model || 'gemini-2.5-flash',
    apiKey: initialSavedKey || '',
    timeoutSeconds: 15
  },
  providers: [
    {
      id: 'gemini',
      name: 'Google Gemini API',
      protocol: 'GEMINI',
      baseUrl: 'https://generativelanguage.googleapis.com/v1beta',
      defaultModel: 'gemini-2.5-flash',
      availableModels: ['gemini-2.5-flash', 'gemini-2.5-pro', 'gemini-2.0-flash', 'gemini-1.5-flash', 'gemini-1.5-pro'],
      active: initialActiveProviderId === 'gemini'
    },
    {
      id: 'openai',
      name: 'OpenAI Direct API',
      protocol: 'OPENAI',
      baseUrl: 'https://api.openai.com/v1',
      defaultModel: 'gpt-4o-mini',
      availableModels: ['gpt-4o', 'gpt-4o-mini', 'o1-mini'],
      active: initialActiveProviderId === 'openai'
    },
    {
      id: 'anthropic',
      name: 'Anthropic Claude API',
      protocol: 'ANTHROPIC',
      baseUrl: 'https://api.anthropic.com/v1',
      defaultModel: 'claude-3-5-sonnet-20241022',
      availableModels: ['claude-3-5-sonnet-20241022', 'claude-3-5-haiku-20241022', 'claude-3-opus-20240229'],
      active: initialActiveProviderId === 'anthropic'
    },
    {
      id: 'pollinations',
      name: 'Pollinations AI Gateway (OpenAI-Compatible)',
      protocol: 'OPENAI',
      baseUrl: 'https://gen.pollinations.ai/v1',
      defaultModel: 'qwen/qwen3.7-flash',
      availableModels: ['qwen/qwen3.7-flash', 'openai/gpt-4o', 'anthropic/claude-3-5-sonnet', 'deepseek/deepseek-chat'],
      active: initialActiveProviderId === 'pollinations'
    }
  ],
  activeProviderId: initialActiveProviderId,
  customSystemPrompt: initialSavedPrompt,
  telemetryLogs: [
    {
      timestamp: new Date().toLocaleTimeString(),
      type: 'INIT',
      status: 'SUCCESS',
      message: 'Tactical Client AI Engine initialized in local-first standalone mode.'
    }
  ],
  verification: {
    loading: false,
    valid: !!initialSavedKey,
    discoveredModelsCount: 0,
    discoveredModelsList: [],
    lastChecked: initialSavedKey ? new Date().toLocaleTimeString() : null,
    latencyMs: 0,
    message: initialSavedKey
      ? 'Stored API Key loaded from local browser memory.'
      : 'No API key configured for active provider. Enter API key to activate AI operations.'
  },
  executingAiOp: false,
  activeAiResult: null
});

export function useAiStore() {
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
        state.config = { ...state.config, ...data };
        const savedKey = storageService.getAiKey(state.activeProviderId);
        if (savedKey) {
          state.config.apiKey = savedKey;
          state.verification.valid = true;
        }
      }
    } catch (err) {
      console.warn('Local AI config loaded:', err.message);
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
    } catch (err) {
      console.warn('Providers loaded from cache');
    }
  }

  async function activateProvider(providerId) {
    state.activeProviderId = providerId;
    state.providers.forEach((p) => {
      p.active = p.id === providerId;
    });

    const provider = activeProvider.value;
    state.config.provider = providerId.toUpperCase();
    state.config.baseUrl = provider.baseUrl;

    // Load saved key for this provider
    const key = storageService.getAiKey(providerId);
    state.config.apiKey = key || '';

    if (key) {
      state.verification.valid = true;
      state.verification.message = `Stored API Key loaded for [${provider.name}].`;
    } else {
      state.verification.valid = false;
      state.verification.message = `No API key saved for [${provider.name}]. Please enter your API key.`;
    }

    if (!provider.availableModels.includes(state.config.model)) {
      state.config.model = provider.defaultModel;
    }

    storageService.saveAiConfig({
      providerId,
      baseUrl: provider.baseUrl,
      model: state.config.model,
      timeoutSeconds: 15
    });

    addLog('PROVIDER', `Switched active AI engine to [${providerId.toUpperCase()}]`, 'SUCCESS');
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
    }
  }

  async function verifyProvider(baseUrl, apiKey, protocol = 'OPENAI', timeoutSeconds = 15) {
    state.verification.loading = true;
    addLog('TEST_KEY', `Pinging verification gateway: ${baseUrl || activeProvider.value.baseUrl}...`, 'RUNNING');
    try {
      if (!apiKey || apiKey.trim() === '') {
        throw new Error('API Key is required to verify provider models.');
      }

      const result = await apiService.verifyProvider({ baseUrl, apiKey, protocol, timeoutSeconds });
      if (!result || !result.valid) {
        throw new Error(result?.message || 'Provider connection rejected by remote gateway.');
      }

      const models = result.discoveredModels || result.availableModels || [];
      state.verification = {
        loading: false,
        valid: true,
        discoveredModelsCount: models.length,
        discoveredModelsList: models,
        lastChecked: new Date().toLocaleTimeString(),
        latencyMs: result.latencyMs || 250,
        message: result.message || `Discovered ${models.length} models on endpoint.`
      };

      state.config.apiKey = apiKey.trim();
      state.config.baseUrl = baseUrl || activeProvider.value.baseUrl;
      storageService.saveAiKey(state.activeProviderId, apiKey.trim());

      const provider = state.providers.find((p) => p.id === state.activeProviderId);
      if (provider && models.length > 0) {
        provider.availableModels = models;
        if (!provider.availableModels.includes(state.config.model)) {
          state.config.model = models[0];
          provider.defaultModel = models[0];
        }
      }

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
    if (!modelName) return;
    state.config.model = modelName;
    const provider = state.providers.find((p) => p.id === state.activeProviderId);
    if (provider) {
      provider.defaultModel = modelName;
    }
    storageService.saveAiConfig({
      providerId: state.activeProviderId,
      baseUrl: state.config.baseUrl,
      model: modelName,
      timeoutSeconds: 15
    });
    addLog('MODEL_SELECT', `Active model set to [${modelName}]`, 'SUCCESS');
  }

  function setCustomPrompt(prompt) {
    state.customSystemPrompt = prompt;
    storageService.saveSystemPrompt(prompt);
    addLog('PROMPT_UPDATE', 'System prompt directive buffer saved to local storage.', 'INFO');
  }

  function resetCustomPrompt() {
    state.customSystemPrompt = DEFAULT_SYSTEM_PROMPT;
    storageService.saveSystemPrompt(DEFAULT_SYSTEM_PROMPT);
    addLog('PROMPT_RESET', 'System prompt reset to tactical baseline standard.', 'INFO');
  }

  // AI Operations (Direct client execution)
  async function runAiOp(opName, apiFn, taskId) {
    const currentKey = storageService.getAiKey(state.activeProviderId) || state.config.apiKey;
    if (!currentKey || currentKey.trim() === '') {
      const err = new Error(`NO API KEY: Please switch to AI COPILOT & SETTINGS [02//] and provide your API key for [${activeProvider.value.name}].`);
      addLog('AI_ERR', err.message, 'ERROR');
      throw err;
    }

    state.executingAiOp = true;
    addLog('AI_DISPATCH', `Dispatching ${opName} for Target [${taskId}] to ${state.config.model}...`, 'RUNNING');
    try {
      const result = await apiFn(taskId, state.customSystemPrompt);
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
}
