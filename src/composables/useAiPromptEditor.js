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
      const provider = state.providers.find((p) => p.id === state.activeProviderId) || activeProvider.value;
      const providerModels = provider?.availableModels;
      if (Array.isArray(providerModels) && providerModels.length > 0) {
        list.push(...providerModels);
      } else if (provider?.defaultModel) {
        list.push(provider.defaultModel);
      }
    }
    if (state.config.model && !list.includes(state.config.model)) {
      list.unshift(state.config.model);
    }
    return list;
  });

  const popularModels = computed(() => {
    const provider = state.providers.find((p) => p.id === state.activeProviderId) || activeProvider.value;
    if (provider && Array.isArray(provider.availableModels)) {
      return provider.availableModels.slice(0, 5);
    }
    return availableModels.value.slice(0, 5);
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
    (newId) => {
      const provider = state.providers.find((p) => p.id === newId) || activeProvider.value;
      verifyBaseUrl.value = provider ? provider.baseUrl : state.config.baseUrl;
      verifyApiKey.value = state.config.apiKey || '';
      modelSearchQuery.value = '';
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
      const provider = state.providers.find((p) => p.id === state.activeProviderId) || activeProvider.value;
      const res = await verifyProvider(
        verifyBaseUrl.value,
        verifyApiKey.value,
        provider.protocol
      );
      emit(
        'notify',
        `Gateway Verified! Handshake accepted with ${res.modelCount || (res.discoveredModels ? res.discoveredModels.length : 4)} models available.`,
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

