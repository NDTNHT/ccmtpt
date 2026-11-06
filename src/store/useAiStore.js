import { reactive, computed } from 'vue';
import { aiService } from '../services/aiService.js';
import { taskService } from '../services/taskService.js';

const DEFAULT_SYSTEM_PROMPT = `You are a Tactical AI Task Management Assistant.
Your mission is to decompose operational objectives into clear, actionable checklist subtasks, estimate realistic completion timelines and effort, evaluate priority and risk levels, and produce executive briefings.
Be concise, clear, and structured in your responses.`;

const initialSavedPrompt = aiService.getSystemPrompt() || DEFAULT_SYSTEM_PROMPT;
const initialConfig = aiService.getConfig();
const initialActiveId = initialConfig.providerId || 'gemini';

const state = reactive({
  config: {
    provider: initialConfig.provider || 'GEMINI',
    providerId: initialActiveId,
    baseUrl: initialConfig.baseUrl,
    model: initialConfig.model,
    apiKey: initialConfig.apiKey,
    timeoutSeconds: 15
  },
  providers: aiService.getProviders(),
  activeProviderId: initialActiveId,
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
    valid: !!initialConfig.apiKey,
    discoveredModelsCount: 0,
    discoveredModelsList: [],
    lastChecked: initialConfig.apiKey ? new Date().toLocaleTimeString() : null,
    latencyMs: 0,
    message: initialConfig.apiKey
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

  function fetchConfig() {
    const conf = aiService.getConfig();
    state.config = { ...state.config, ...conf };
    if (conf.apiKey) {
      state.verification.valid = true;
      state.verification.message = `Stored API Key loaded for [${activeProvider.value.name}].`;
    }
  }

  function fetchProviders() {
    state.providers = aiService.getProviders();
  }

  function activateProvider(providerId) {
    state.activeProviderId = providerId;
    state.providers.forEach((p) => {
      p.active = p.id === providerId;
    });

    const provider = state.providers.find((p) => p.id === providerId) || state.providers[0];
    const key = aiService.getKey(providerId);

    state.config.providerId = providerId;
    state.config.provider = providerId.toUpperCase();
    state.config.baseUrl = provider.baseUrl;
    state.config.apiKey = key || '';

    // Clear stale discovered models from previous provider
    state.verification.discoveredModelsList = [];
    state.verification.discoveredModelsCount = 0;

    if (key || providerId === 'pollinations') {
      state.verification.valid = true;
      state.verification.message = key
        ? `Stored API Key loaded for [${provider.name}].`
        : `Free public gateway active for [${provider.name}]. No API key required.`;
    } else {
      state.verification.valid = false;
      state.verification.message = `No API key saved for [${provider.name}]. Please enter your API key.`;
    }

    if (!provider.availableModels.includes(state.config.model)) {
      state.config.model = provider.defaultModel;
    }

    aiService.saveConfig({
      providerId,
      baseUrl: provider.baseUrl,
      model: state.config.model,
      apiKey: state.config.apiKey
    });

    addLog('PROVIDER', `Switched active AI engine to [${provider.name.toUpperCase()}] (${state.config.model})`, 'SUCCESS');
  }

  async function verifyProvider(baseUrl, apiKey, protocol = 'OPENAI', timeoutSeconds = 15) {
    state.verification.loading = true;
    addLog('TEST_KEY', `Pinging verification gateway: ${baseUrl || activeProvider.value.baseUrl}...`, 'RUNNING');
    try {
      if (!apiKey || apiKey.trim() === '') {
        throw new Error('API Key is required to verify provider models.');
      }

      const result = await aiService.verifyProvider({ baseUrl, apiKey, protocol, timeoutSeconds });
      if (!result || !result.valid) {
        throw new Error(result?.message || 'Provider connection rejected by remote gateway.');
      }

      const models = result.discoveredModels || [];
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
      aiService.saveKey(state.activeProviderId, apiKey.trim());

      const provider = state.providers.find((p) => p.id === state.activeProviderId);
      if (provider && models.length > 0) {
        provider.availableModels = models;
        if (!provider.availableModels.includes(state.config.model)) {
          state.config.model = models[0];
          provider.defaultModel = models[0];
        }
      }

      aiService.saveConfig({
        providerId: state.activeProviderId,
        baseUrl: state.config.baseUrl,
        model: state.config.model,
        apiKey: apiKey.trim()
      });

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

  function selectModel(modelName) {
    if (!modelName) return;

    // Auto-detect and switch provider if this model belongs to another provider
    const targetProviderId = aiService.findProviderForModel(modelName);
    if (targetProviderId && targetProviderId !== state.activeProviderId) {
      activateProvider(targetProviderId);
    }

    state.config.model = modelName;
    const provider = state.providers.find((p) => p.id === state.activeProviderId);
    if (provider) {
      provider.defaultModel = modelName;
    }
    aiService.saveConfig({
      providerId: state.activeProviderId,
      baseUrl: state.config.baseUrl,
      model: modelName
    });
    addLog('MODEL_SELECT', `Active model set to [${modelName}] on [${state.activeProviderId.toUpperCase()}]`, 'SUCCESS');
  }

  function setCustomPrompt(prompt) {
    state.customSystemPrompt = prompt;
    aiService.saveSystemPrompt(prompt);
    addLog('PROMPT_UPDATE', 'System prompt directive buffer saved to local storage.', 'INFO');
  }

  function resetCustomPrompt() {
    state.customSystemPrompt = DEFAULT_SYSTEM_PROMPT;
    aiService.saveSystemPrompt(DEFAULT_SYSTEM_PROMPT);
    addLog('PROMPT_RESET', 'System prompt reset to tactical baseline standard.', 'INFO');
  }

  // AI Operations
  async function runAiOp(opName, fnName, taskId) {
    const isFreePollinations = state.activeProviderId === 'pollinations';
    const currentKey = aiService.getKey(state.activeProviderId) || state.config.apiKey;
    if (!isFreePollinations && (!currentKey || currentKey.trim() === '')) {
      const err = new Error(`NO API KEY: Please switch to AI COPILOT & SETTINGS [02//] and provide your API key for [${activeProvider.value.name}].`);
      addLog('AI_ERR', err.message, 'ERROR');
      throw err;
    }

    const task = taskService.getTaskById(taskId);
    state.executingAiOp = true;
    addLog('AI_DISPATCH', `Dispatching ${opName} for Target [${taskId}] to [${state.activeProviderId.toUpperCase()}] (${state.config.model})...`, 'RUNNING');
    try {
      const result = await aiService[fnName](task, state.customSystemPrompt);
      state.activeAiResult = {
        type: opName,
        taskId,
        data: result,
        timestamp: new Date().toLocaleTimeString()
      };
      addLog('AI_RESOLVED', `${opName} completed successfully via [${state.activeProviderId.toUpperCase()}].`, 'SUCCESS');
      return result;
    } catch (error) {
      addLog('AI_ERR', `${opName} failed: ${error.message}`, 'ERROR');
      throw error;
    } finally {
      state.executingAiOp = false;
    }
  }

  async function decomposeTask(taskId) {
    return runAiOp('DECOMPOSE', 'decomposeTask', taskId);
  }

  async function suggestSubTasks(taskId) {
    return runAiOp('SUGGEST_SUBTASKS', 'suggestSubTasks', taskId);
  }

  async function estimateDeadline(taskId) {
    return runAiOp('ESTIMATE_DEADLINE', 'estimateDeadline', taskId);
  }

  async function analyzePriority(taskId) {
    return runAiOp('AUTO_PRIORITY', 'analyzePriority', taskId);
  }

  async function generateSummary(taskId) {
    return runAiOp('SUMMARY', 'generateSummary', taskId);
  }

  async function runFullAnalysis(taskId) {
    return runAiOp('FULL_ANALYSIS', 'analyzeTask', taskId);
  }

  return {
    state,
    activeProvider,
    addLog,
    fetchConfig,
    fetchProviders,
    activateProvider,
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
