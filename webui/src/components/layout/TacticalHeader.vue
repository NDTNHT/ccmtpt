<template>
  <header class="tactical-header">
    <div class="brand-zone">
      <div class="brand-logo">
        <span class="ascii-badge text-alert">[TASK OPS]</span>
        <h1 class="system-title">TASK MANAGEMENT SYSTEM</h1>
      </div>
      <div class="system-telemetry">
        <span class="telemetry-item">
          <span class="status-led" :class="aiState.verification.valid ? 'green' : 'amber'"></span>
          <span class="telemetry-label">AI GATEWAY:</span>
          <span v-if="taskState.localMode" class="telemetry-val text-green">LOCAL MOCK</span>
          <span v-else-if="aiState.verification.valid" class="telemetry-val text-green">
            ONLINE [{{ aiState.verification.discoveredModelsCount }} MODELS]
          </span>
          <span v-else class="telemetry-val text-amber">
            STANDBY [NO KEY]
          </span>
        </span>
        <span class="telemetry-divider">///</span>
        <span class="telemetry-item">
          <span class="telemetry-label">ACTIVE MODEL:</span>
          <span class="telemetry-val text-amber">{{ activeEngine }}</span>
        </span>
        <span class="telemetry-divider">///</span>
        <span class="telemetry-item">
          <span class="telemetry-label">TIME:</span>
          <span class="telemetry-val text-dim">{{ currentTimestamp }}</span>
        </span>
      </div>
    </div>

    <div class="header-actions">
      <div class="status-badge-group">
        <span class="ascii-badge">TASKS: {{ taskStats.total }}</span>
        <span class="ascii-badge text-amber">IN PROGRESS: {{ taskStats.inProgress }}</span>
        <span class="ascii-badge text-green">DONE: {{ taskStats.completed }}</span>
      </div>

      <div class="engine-indicator-badge">
        <span class="indicator-prefix">[MODEL]</span>
        <span class="indicator-val text-amber">{{ taskState.localMode ? 'LOCAL MOCK' : aiState.config.model }}</span>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useTaskStore } from '../../store/useTaskStore.js';
import { useAiStore } from '../../store/useAiStore.js';

const { taskStats, state: taskState, setLocalMode } = useTaskStore();
const { state: aiState, activeProvider } = useAiStore();

const activeEngine = computed(() => {
  if (taskState.localMode) return 'LOCAL MOCK / RULE-BASED DEMO';
  return `${activeProvider.value.name.toUpperCase()} (${aiState.config.model})`;
});

const currentTimestamp = ref(new Date().toTimeString().slice(0, 8) + ' UTC');
let timer = null;

onMounted(() => {
  timer = setInterval(() => {
    currentTimestamp.value = new Date().toTimeString().slice(0, 8) + ' UTC';
  }, 1000);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<style scoped>
.tactical-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background-color: var(--color-surface);
  border-bottom: 2px solid var(--color-border-heavy);
  font-family: var(--font-mono-code);
  position: relative;
  z-index: 100;
}

.brand-zone {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.system-title {
  font-family: var(--font-mono-header);
  font-size: 1.15rem;
  letter-spacing: 0.15em;
  font-weight: 800;
  margin: 0;
  color: var(--color-text-main);
  text-transform: uppercase;
}

.system-telemetry {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 0.72rem;
  color: var(--color-text-dim);
  letter-spacing: 0.05em;
}

.telemetry-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.telemetry-divider {
  color: var(--color-border-heavy);
  font-weight: bold;
}

.status-led {
  width: 7px;
  height: 7px;
  display: inline-block;
}

.status-led.green {
  background-color: var(--color-neon-green);
  box-shadow: 0 0 6px var(--color-neon-green);
}

.status-led.amber {
  background-color: var(--color-neon-amber);
  box-shadow: 0 0 6px var(--color-neon-amber);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-badge-group {
  display: flex;
  gap: 8px;
}

.engine-indicator-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: #181818;
  border: 1px solid #444444;
  font-size: 0.75rem;
  font-weight: 800;
}

.indicator-prefix {
  color: var(--color-text-muted);
}
@media (max-width: 640px) {
  .tactical-header { flex-direction: column; align-items: stretch; gap: 12px; padding: 12px; }
  .brand-logo { flex-wrap: wrap; gap: 8px; }
  .system-title { font-size: .9rem; white-space: normal; }
  .system-telemetry { flex-wrap: wrap; gap: 8px; }
  .telemetry-item { flex-wrap: wrap; }
  .telemetry-divider { display: none; }
  .header-actions { align-items: flex-start; }
  .status-badge-group { flex-wrap: wrap; }
}

.workspace-toggle-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #141414;
  border: 1px solid #444444;
  padding: 6px 12px;
  font-family: var(--font-mono-code);
  font-size: 0.75rem;
  color: #DDDDDD;
  cursor: pointer;
  transition: all 0.2s ease;
}

.workspace-toggle-btn:hover {
  border-color: var(--color-neon-amber);
  background: #1E1E1E;
}

.workspace-toggle-btn.mode-local {
  border-color: rgba(52, 199, 89, 0.6);
}

.workspace-toggle-btn.mode-server {
  border-color: rgba(255, 179, 0, 0.6);
}

.mode-led {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
}

.mode-led.led-local {
  background: #34C759;
  box-shadow: 0 0 6px #34C759;
}

.mode-led.led-server {
  background: var(--color-neon-amber);
  box-shadow: 0 0 6px var(--color-neon-amber);
}

.mode-label {
  font-weight: 600;
  letter-spacing: 0.05em;
}

.mode-action {
  font-size: 0.7rem;
  color: #888888;
  padding-left: 4px;
  border-left: 1px solid #333333;
}

.workspace-toggle-btn:hover .mode-action {
  color: #FFFFFF;
}

</style>
