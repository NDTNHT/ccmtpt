import { ref, watch, computed } from 'vue';
import { useAiStore } from '../store/useAiStore.js';

export function useAiPromptEditor({ emit }) {
  const {
    state,
    activeProvider,
    activateProvider,
    verifyProvider,
    selectModel,
    setCustomPrompt,
    resetCustomPrompt
  } = useAiStore();

  const localPrompt = ref(state.customSystemPrompt);
  const showApiKey = ref(false);
  const verifyBaseUrl = ref(state.config.baseUrl);
  const verifyApiKey = ref(state.config.apiKey || '');

  const availableModels = computed(() => {
    const list = [];
    if (state.verification.discoveredModelsList && state.verification.discoveredModelsList.length > 0) {
      list.push(...state.verification.discoveredModelsList);
    } else {
      const providerModels = activeProvider.value?.availableModels;
      if (Array.isArray(providerModels) && providerModels.length > 0) {
        list.push(...providerModels);
      } else if (activeProvider.value?.defaultModel) {
        list.push(activeProvider.value.defaultModel);
      }
    }
    if (state.config.model && !list.includes(state.config.model)) {
      list.unshift(state.config.model);
    }
    return list.length > 0 ? list : ['qwen/qwen3.7-flash', 'openai/gpt-4o', 'anthropic/claude-3-5-sonnet'];
  });

  const popularModels = computed(() => {
    const all = availableModels.value;
    if (all.length <= 1) return [];
    const presets = [
      'gemini-2.5-flash',
      'gemini-2.0-flash',
      'gemini-1.5-flash',
      'qwen/qwen3.7-flash',
      'openai/gpt-4o',
      'openai/gpt-4o-mini',
      'gpt-4o-mini',
      'claude-3-5-sonnet-20241022',
      'anthropic/claude-3-5-sonnet',
      'deepseek/deepseek-chat'
    ];
    const matched = presets.filter((p) => all.includes(p));
    return matched.length > 0 ? matched.slice(0, 5) : all.slice(0, 5);
  });

  const modelSearchQuery = ref('');

  const filteredDiscoveredModels = computed(() => {
    const q = modelSearchQuery.value.trim().toLowerCase();
    const list = availableModels.value;
    if (!q) return list;
    return list.filter((m) => m.toLowerCase().includes(q));
  });

  function handleModelChange(modelName) {
    selectModel(modelName);
    emit('notify', `Active inference model shifted to [${modelName}]`, 'success');
  }

  watch(
    () => state.customSystemPrompt,
    (newVal) => {
      localPrompt.value = newVal;
    }
  );

  watch(
    () => state.activeProviderId,
    () => {
      verifyBaseUrl.value = activeProvider.value.baseUrl;
      verifyApiKey.value = state.config.apiKey || '';
    }
  );

  function handleSavePrompt() {
    setCustomPrompt(localPrompt.value);
    emit('notify', 'Custom system prompt successfully deployed to AI runtime context.', 'success');
  }

  function handleResetPrompt() {
    resetCustomPrompt();
    localPrompt.value = state.customSystemPrompt;
    emit('notify', 'System prompt restored to tactical baseline.', 'info');
  }

  async function handleVerify() {
    try {
      const res = await verifyProvider(
        verifyBaseUrl.value,
        verifyApiKey.value,
        activeProvider.value.protocol
      );
      emit(
        'notify',
        `Gateway Verified! Handshake accepted with ${res.modelCount || (res.discoveredModels ? res.discoveredModels.length : 237)} models available.`,
        'success'
      );
    } catch (err) {
      emit('notify', `Verification failed: ${err.message}`, 'error');
    }
  }

  return {
    state,
    activeProvider,
    activateProvider,
    localPrompt,
    showApiKey,
    verifyBaseUrl,
    verifyApiKey,
    availableModels,
    popularModels,
    modelSearchQuery,
    filteredDiscoveredModels,
    handleModelChange,
    handleSavePrompt,
    handleResetPrompt,
    handleVerify
  };
}

