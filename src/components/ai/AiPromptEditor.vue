<template>
  <div class="ai-prompt-editor">
    <div class="panel-header">
      <div class="header-tag">
        <span class="ascii-badge text-alert">[AI CONFIG]</span>
        <h3 class="panel-title">AI PROVIDER SETTINGS &amp; SYSTEM PROMPT</h3>
      </div>
      <span class="panel-protocol">PROTOCOL: {{ activeProvider.protocol }}</span>
    </div>

    <div class="panel-body">
      <!-- Active Provider Selection Bar -->
      <div class="provider-selector-section">
        <span class="section-lbl">/// ACTIVE AI PROVIDER:</span>
        <div class="provider-chips">
          <button
            v-for="p in state.providers"
            :key="p.id"
            class="provider-chip"
            :class="{ active: state.activeProviderId === p.id }"
            @click="activateProvider(p.id)"
          >
            <span class="chip-radio">{{ state.activeProviderId === p.id ? '(*)' : '( )' }}</span>
            <span class="chip-name">{{ p.name }}</span>
            <span class="chip-proto">[{{ p.protocol }}]</span>
          </button>
        </div>
      </div>

      <!-- Provider Verification & Test Key Section -->
      <div class="verification-box">
        <div class="verify-header">
          <span class="verify-lbl">/// PROVIDER ENDPOINT &amp; API KEY</span>
          <span v-if="state.verification.valid" class="ascii-badge badge-green">
            [VERIFIED: {{ state.verification.discoveredModels }} MODELS // {{ state.verification.latencyMs }}ms]
          </span>
          <span v-else class="ascii-badge badge-dim">[AWAITING API KEY]</span>
        </div>

        <div class="verify-form">
          <div class="verify-inputs">
            <div class="input-col">
              <span class="input-lbl">BASE URL:</span>
              <input
                v-model="verifyBaseUrl"
                type="text"
                class="tactical-input"
                placeholder="https://gen.pollinations.ai/v1"
              />
            </div>
            <div class="input-col">
              <span class="input-lbl">API KEY:</span>
              <div class="key-input-wrapper">
                <input
                  v-model="verifyApiKey"
                  :type="showApiKey ? 'text' : 'password'"
                  class="tactical-input key-input"
                  :placeholder="activeProvider.protocol === 'GEMINI' ? 'AIzaSy... (Google AI Studio Key)' : (activeProvider.id === 'anthropic' ? 'sk-ant-...' : 'sk-...')"
                />
                <button
                  type="button"
                  class="toggle-key-btn"
                  @click="showApiKey = !showApiKey"
                >
                  {{ showApiKey ? '[HIDE]' : '[SHOW]' }}
                </button>
              </div>
            </div>
          </div>

          <div class="verify-actions">
            <button
              class="tactical-btn-verify"
              :disabled="state.verification.loading"
              @click="handleVerify"
            >
              {{ state.verification.loading ? '[TESTING CONNECTION...]' : '[TEST KEY & FETCH MODELS]' }}
            </button>
            <span class="verify-msg">{{ state.verification.message }}</span>
          </div>
        </div>
      </div>

      <!-- Model Selector Section (After Key Verification or Discovered Models) -->
      <div class="model-selector-box">
        <div class="model-selector-header">
          <div class="header-left">
            <span class="section-lbl">/// MODEL SELECTION:</span>
            <span class="ascii-badge badge-green" v-if="state.verification.valid">
              [{{ state.verification.discoveredModelsCount || availableModels.length }} MODELS AVAILABLE]
            </span>
            <span class="ascii-badge badge-dim" v-else>
              [PRESET MODELS]
            </span>
          </div>
          <span class="current-model-tag">
            ACTIVE MODEL: <strong class="text-amber">{{ state.config.model }}</strong>
          </span>
        </div>

        <div class="model-selector-controls">
          <div class="model-dropdown-row">
            <div class="model-filter-bar">
              <span class="control-lbl">CHOOSE MODEL ({{ filteredDiscoveredModels.length }} / {{ availableModels.length }} AVAILABLE):</span>
              <input
                v-model="modelSearchQuery"
                type="text"
                class="tactical-input model-search-input"
                placeholder="FILTER MODELS (E.G. QWEN, GPT, CLAUDE, DEEPSEEK)..."
              />
            </div>
            <select
              :value="state.config.model"
              class="tactical-select model-select"
              @change="handleModelChange($event.target.value)"
            >
              <option v-for="m in filteredDiscoveredModels" :key="m" :value="m">
                {{ m }} {{ m === state.config.model ? ' ★ [ACTIVE]' : '' }}
              </option>
            </select>
          </div>

          <!-- Quick Select Preset Chips -->
          <div class="quick-models-group" v-if="popularModels.length > 0">
            <span class="control-lbl">QUICK PRESETS:</span>
            <div class="quick-model-chips">
              <button
                v-for="qm in popularModels"
                :key="qm"
                type="button"
                class="quick-model-btn"
                :class="{ active: state.config.model === qm }"
                @click="handleModelChange(qm)"
              >
                {{ qm }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Configurable System Prompt (Write-in) -->
      <div class="prompt-section">
        <div class="prompt-header">
          <div class="prompt-title-group">
            <span class="section-lbl">/// SYSTEM PROMPT INSTRUCTIONS:</span>
            <span class="prompt-char-count">{{ localPrompt.length }} CHARS</span>
          </div>
          <div class="prompt-btn-group">
            <button class="tactical-btn-xs" @click="handleResetPrompt">[RESET DEFAULT]</button>
            <button class="tactical-btn-xs text-amber" @click="handleSavePrompt">[SAVE PROMPT]</button>
          </div>
        </div>

        <textarea
          v-model="localPrompt"
          class="prompt-textarea"
          rows="6"
          placeholder="Enter system-level guidelines, domain rules, or formatting preferences for the AI assistant..."
        ></textarea>
        <span class="prompt-hint">
          * These instructions guide all subtask breakdowns, due date estimates, and priority evaluations.
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue';
import { useAiStore } from '../../store/useAiStore.js';

const emit = defineEmits(['notify']);

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
  const matched = presets.filter(p => all.includes(p));
  return matched.length > 0 ? matched.slice(0, 5) : all.slice(0, 5);
});

const modelSearchQuery = ref('');

const filteredDiscoveredModels = computed(() => {
  const q = modelSearchQuery.value.trim().toLowerCase();
  const list = availableModels.value;
  if (!q) return list;
  return list.filter(m => m.toLowerCase().includes(q));
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
</script>

<style scoped>
.ai-prompt-editor {
  background: var(--color-surface);
  border: 1px solid var(--color-border-heavy);
  font-family: var(--font-mono-code);
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 18px;
  background: var(--color-surface-sunken);
  border-bottom: 1px solid var(--color-border-heavy);
}

.header-tag {
  display: flex;
  align-items: center;
  gap: 10px;
}

.panel-title {
  font-family: var(--font-mono-header);
  font-size: 0.88rem;
  font-weight: 800;
  color: var(--color-text-main);
  margin: 0;
}

.panel-protocol {
  font-size: 0.72rem;
  color: var(--color-neon-amber);
  font-weight: 700;
}

.panel-body {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-lbl {
  font-size: 0.72rem;
  color: var(--color-text-muted);
  font-weight: 800;
  letter-spacing: 0.08em;
}

.provider-selector-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.provider-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.provider-chip {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  color: var(--color-text-dim);
  padding: 6px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-family: inherit;
  font-size: 0.75rem;
  transition: all 0.15s ease;
}

.provider-chip:hover {
  border-color: var(--color-border-heavy);
  color: var(--color-text-main);
}

.provider-chip.active {
  background: var(--color-surface-hover);
  border-color: var(--color-neon-amber);
  color: var(--color-neon-amber);
  font-weight: 800;
}

.chip-radio {
  font-weight: 900;
}

.chip-proto {
  font-size: 0.68rem;
  color: var(--color-text-muted);
}

.verification-box {
  background: var(--color-surface-sunken);
  border: 1px solid var(--color-border);
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.verify-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.verify-lbl {
  font-size: 0.7rem;
  color: var(--color-text-muted);
  font-weight: 800;
}

.badge-green {
  border-color: var(--color-neon-green);
  color: var(--color-neon-green);
}

.badge-alert {
  border-color: var(--color-alert-red);
  color: var(--color-alert-red);
}

.verify-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.verify-inputs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.input-col {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.input-lbl {
  font-size: 0.68rem;
  color: var(--color-text-muted);
}

.tactical-input {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  padding: 6px 10px;
  font-family: inherit;
  font-size: 0.75rem;
}

.tactical-input:focus {
  outline: none;
  border-color: var(--color-neon-amber);
}

.key-input-wrapper {
  display: flex;
}

.key-input {
  flex: 1;
}

.toggle-key-btn {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-left: none;
  color: var(--color-text-dim);
  padding: 0 8px;
  cursor: pointer;
  font-family: inherit;
  font-size: 0.68rem;
}

.verify-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tactical-btn-verify {
  background: var(--color-bg);
  border: 1px solid var(--color-neon-amber);
  color: var(--color-neon-amber);
  padding: 6px 14px;
  font-family: inherit;
  font-size: 0.72rem;
  font-weight: 800;
  cursor: pointer;
}

.tactical-btn-verify:hover:not(:disabled) {
  background: var(--color-neon-amber);
  color: #000;
}

.verify-msg {
  font-size: 0.7rem;
  color: var(--color-text-dim);
}

.model-selector-box {
  background: var(--color-surface-sunken);
  border: 1px solid var(--color-border);
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.model-selector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.current-model-tag {
  font-size: 0.74rem;
  color: var(--color-text-dim);
}

.model-selector-controls {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.model-dropdown-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.model-filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.model-search-input {
  max-width: 320px;
  background: #101010;
  border: 1px solid var(--color-border-heavy);
  padding: 4px 8px;
  font-size: 0.72rem;
}

.control-lbl {
  font-size: 0.68rem;
  color: var(--color-text-muted);
  font-weight: 700;
}

.model-select {
  background: #101010;
  border: 1px solid var(--color-border-heavy);
  color: var(--color-text-main);
  padding: 8px 10px;
  font-family: var(--font-mono-code);
  font-size: 0.78rem;
  cursor: pointer;
}

.model-select:focus {
  outline: none;
  border-color: var(--color-neon-amber);
}

.quick-models-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.quick-model-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.quick-model-btn {
  background: #181818;
  border: 1px solid var(--color-border);
  color: var(--color-text-dim);
  padding: 3px 8px;
  font-family: var(--font-mono-code);
  font-size: 0.7rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.quick-model-btn:hover {
  border-color: var(--color-neon-amber);
  color: var(--color-text-main);
}

.quick-model-btn.active {
  background: #252525;
  border-color: var(--color-neon-amber);
  color: var(--color-neon-amber);
}

.prompt-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.prompt-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.prompt-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.prompt-char-count {
  font-size: 0.68rem;
  color: var(--color-text-muted);
}

.prompt-btn-group {
  display: flex;
  gap: 8px;
}

.tactical-btn-xs {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  color: var(--color-text-dim);
  padding: 3px 8px;
  font-family: inherit;
  font-size: 0.7rem;
  cursor: pointer;
}

.tactical-btn-xs:hover {
  background: var(--color-surface-hover);
  color: var(--color-text-main);
}

.prompt-textarea {
  background: var(--color-bg);
  border: 1px solid var(--color-border-heavy);
  color: var(--color-text-main);
  padding: 10px 12px;
  font-family: var(--font-mono-code);
  font-size: 0.78rem;
  line-height: 1.5;
  resize: vertical;
}

.prompt-textarea:focus {
  outline: none;
  border-color: var(--color-neon-amber);
}

.prompt-hint {
  font-size: 0.68rem;
  color: var(--color-text-muted);
  font-style: italic;
}
</style>
