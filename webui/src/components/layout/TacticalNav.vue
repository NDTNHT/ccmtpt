<template>
  <nav class="tactical-nav">
    <div class="nav-track">
      <button
        class="nav-tab"
        :class="{ active: currentTab === 'tasks' }"
        @click="$emit('update:currentTab', 'tasks')"
      >
        <span class="tab-index">01//</span>
        <span class="tab-title">TASKS &amp; PROJECTS</span>
        <span class="tab-indicator" v-if="currentTab === 'tasks'">[ACTIVE]</span>
      </button>

      <button
        class="nav-tab"
        :class="{ active: currentTab === 'ai' }"
        @click="$emit('update:currentTab', 'ai')"
      >
        <span class="tab-index">02//</span>
        <span class="tab-title">AI COPILOT &amp; SETTINGS</span>
        <span class="tab-indicator" v-if="currentTab === 'ai'">[ACTIVE]</span>
      </button>
    </div>

    <div class="nav-system-status">
      <button
        type="button"
        class="backend-status-pill"
        :class="backendOnline ? 'status-online' : 'status-offline'"
        @click="showConfigModal = true"
        title="Click to configure Backend Gateway URL"
      >
        <span class="status-led"></span>
        <span class="status-text">
          {{ backendOnline ? 'BACKEND ONLINE' : 'BACKEND OFFLINE [CONFIGURE URL]' }}
        </span>
        <span class="status-tag">⚙</span>
      </button>
    </div>

    <!-- Backend Gateway Configuration Modal -->
    <div v-if="showConfigModal" class="tactical-modal-backdrop" @click.self="showConfigModal = false">
      <div class="tactical-modal-window">
        <div class="modal-header">
          <div class="header-tag">
            <span class="ascii-badge text-alert">[SYS // GATEWAY]</span>
            <span class="modal-title">BACKEND GATEWAY CONFIGURATION</span>
          </div>
          <button class="modal-close-btn" @click="showConfigModal = false">[CLOSE / ESC]</button>
        </div>

        <div class="modal-body">
          <p class="config-desc">
            Configure the Spring Boot REST API origin URL. When running locally, <code>/api</code> proxies to <code>http://localhost:8081</code>. When running on Vercel, specify your deployed cloud backend or tunnel URL.
          </p>

          <div class="config-form-group">
            <label class="form-label">
              <span class="label-prefix">&gt;&gt;</span> BACKEND API BASE URL:
            </label>
            <input
              v-model="backendInputUrl"
              type="text"
              class="tactical-input"
              placeholder="e.g. /api or https://task-management-backend.onrender.com/api"
            />
            <span class="field-hint">
              Examples: <code>/api</code> (Local), <code>https://task-backend.onrender.com/api</code>, <code>https://your-tunnel.loca.lt/api</code>
            </span>
          </div>

          <div class="status-summary-box" :class="testResult?.success ? 'box-success' : (testResult ? 'box-error' : '')">
            <div class="summary-line">
              <strong>STATUS:</strong>
              <span :class="backendOnline ? 'text-green' : 'text-hazard'">
                {{ backendOnline ? 'CONNECTED' : 'OFFLINE / UNREACHABLE' }}
              </span>
            </div>
            <div class="summary-line">
              <strong>ACTIVE URL:</strong>
              <code>{{ activeUrl }}</code>
            </div>
            <div v-if="testResult" class="test-feedback">
              {{ testResult.message }}
            </div>
          </div>

          <div class="modal-actions-row">
            <button
              type="button"
              class="tactical-btn-sm"
              :disabled="testingConnection"
              @click="handleTestConnection"
            >
              {{ testingConnection ? '[TESTING...]' : '[TEST CONNECTION]' }}
            </button>
            <button
              type="button"
              class="tactical-btn-sm btn-save"
              @click="handleSaveBackendUrl"
            >
              [APPLY &amp; RECONNECT]
            </button>
            <button
              type="button"
              class="tactical-btn-sm btn-reset"
              @click="handleResetDefault"
            >
              [RESET DEFAULT (/api)]
            </button>
          </div>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useTaskStore } from '../../store/useTaskStore.js';
import { apiService, getApiBaseUrl, setApiBaseUrl } from '../../services/apiService.js';

defineProps({
  currentTab: {
    type: String,
    required: true
  }
});

defineEmits(['update:currentTab']);

const { backendOnline, fetchTasks } = useTaskStore();

const showConfigModal = ref(false);
const backendInputUrl = ref(getApiBaseUrl());
const testingConnection = ref(false);
const testResult = ref(null);

const activeUrl = computed(() => getApiBaseUrl());

async function handleTestConnection() {
  testingConnection.value = true;
  testResult.value = null;
  const original = getApiBaseUrl();
  try {
    setApiBaseUrl(backendInputUrl.value.trim());
    await apiService.getProjects();
    testResult.value = { success: true, message: 'Connection successful! Remote Spring Boot backend responded.' };
  } catch (err) {
    testResult.value = { success: false, message: `Connection failed: ${err.message}` };
    setApiBaseUrl(original);
  } finally {
    testingConnection.value = false;
  }
}

async function handleSaveBackendUrl() {
  setApiBaseUrl(backendInputUrl.value.trim());
  showConfigModal.value = false;
  await fetchTasks();
}

async function handleResetDefault() {
  backendInputUrl.value = '/api';
  setApiBaseUrl('/api');
  testResult.value = null;
  showConfigModal.value = false;
  await fetchTasks();
}
</script>

<style scoped>
.tactical-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: var(--color-surface-sunken);
  border-bottom: 1px solid var(--color-border);
  padding: 0 24px;
}

.nav-track {
  display: flex;
  gap: 2px;
}

.nav-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: transparent;
  border: none;
  border-right: 1px solid var(--color-border);
  color: var(--color-text-dim);
  font-family: var(--font-mono-code);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  position: relative;
  transition: all 0.15s ease;
}

.nav-tab:hover {
  background: var(--color-surface);
  color: var(--color-text-main);
}

.nav-tab.active {
  background: var(--color-bg);
  color: var(--color-neon-amber);
  border-bottom: 2px solid var(--color-neon-amber);
}

.tab-index {
  color: var(--color-text-muted);
  font-size: 0.75rem;
}

.tab-indicator {
  font-size: 0.7rem;
  color: var(--color-neon-amber);
  font-weight: 900;
}

.nav-system-status {
  display: flex;
  align-items: center;
}

.backend-status-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #121212;
  border: 1px solid var(--color-border);
  padding: 5px 12px;
  font-family: var(--font-mono-code);
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--color-text-dim);
  cursor: pointer;
  transition: all 0.15s ease;
}

.backend-status-pill:hover {
  border-color: var(--color-neon-amber);
  color: var(--color-text-main);
  background: #1a1a1a;
}

.status-online {
  border-color: rgba(0, 230, 118, 0.4);
  color: #00E676;
}

.status-offline {
  border-color: rgba(255, 61, 0, 0.5);
  color: #FF7043;
}

.status-led {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}

.status-online .status-led {
  background: #00E676;
  box-shadow: 0 0 6px #00E676;
}

.status-offline .status-led {
  background: #FF3D00;
  box-shadow: 0 0 6px #FF3D00;
}

.status-tag {
  font-size: 0.75rem;
  opacity: 0.7;
}

/* Modal styling */
.tactical-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 16px;
}

.tactical-modal-window {
  background: #0E0E0E;
  border: 2px solid var(--color-neon-amber);
  box-shadow: 0 0 30px rgba(255, 184, 0, 0.2);
  width: 100%;
  max-width: 600px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
  background: #141414;
}

.header-tag {
  display: flex;
  align-items: center;
  gap: 10px;
}

.modal-title {
  font-family: var(--font-mono-code);
  font-weight: 800;
  font-size: 0.85rem;
  letter-spacing: 0.05em;
  color: var(--color-text-main);
}

.modal-close-btn {
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
  font-family: var(--font-mono-code);
  font-size: 0.72rem;
  padding: 3px 8px;
  cursor: pointer;
}

.modal-close-btn:hover {
  color: #FFFFFF;
  border-color: #FFFFFF;
}

.modal-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  font-family: var(--font-mono-code);
}

.config-desc {
  font-size: 0.75rem;
  color: var(--color-text-dim);
  line-height: 1.4;
  margin: 0;
}

.config-desc code, .field-hint code {
  color: var(--color-neon-amber);
  background: #181818;
  padding: 1px 4px;
}

.config-form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-hint {
  font-size: 0.68rem;
  color: var(--color-text-muted);
}

.status-summary-box {
  background: #141414;
  border: 1px solid var(--color-border);
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 0.75rem;
}

.box-success {
  border-color: #00E676;
}

.box-error {
  border-color: #FF3D00;
}

.test-feedback {
  margin-top: 4px;
  padding-top: 6px;
  border-top: 1px dashed var(--color-border);
  font-size: 0.72rem;
}

.modal-actions-row {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.btn-save {
  background: var(--color-neon-amber);
  color: #000000;
  font-weight: 800;
  border-color: var(--color-neon-amber);
}

.btn-reset {
  color: var(--color-text-muted);
}
</style>
