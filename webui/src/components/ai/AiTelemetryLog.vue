<template>
  <div class="ai-telemetry-log">
    <div class="log-header">
      <div class="header-left">
        <span class="ascii-badge text-alert">[TERMINAL LOG]</span>
        <span class="log-title">NEURAL GATEWAY TELEMETRY FEED</span>
      </div>
      <div class="header-right">
        <span class="log-count">{{ state.telemetryLogs.length }} EVENTS BUFFERED</span>
        <button class="clear-log-btn" @click="clearLogs">[PURGE LOG]</button>
      </div>
    </div>

    <div class="terminal-screen" ref="terminalRef">
      <div
        v-for="(item, idx) in state.telemetryLogs"
        :key="idx"
        class="log-line"
        :class="`log-${item.status.toLowerCase()}`"
      >
        <span class="log-time">{{ item.timestamp }}</span>
        <span class="log-type">[{{ item.type }}]</span>
        <span class="log-status" :class="statusColorClass(item.status)">
          {{ item.status }}
        </span>
        <span class="log-msg">&gt;&gt; {{ item.message }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useAiStore } from '../../store/useAiStore.js';

const { state } = useAiStore();
const terminalRef = ref(null);

function clearLogs() {
  state.telemetryLogs.splice(0, state.telemetryLogs.length);
}

function statusColorClass(status) {
  switch (status) {
    case 'SUCCESS': return 'text-green';
    case 'ERROR': return 'text-alert';
    case 'RUNNING': return 'text-amber';
    default: return 'text-dim';
  }
}
</script>

<style scoped>
.ai-telemetry-log {
  background: var(--color-surface);
  border: 1px solid var(--color-border-heavy);
  font-family: var(--font-mono-code);
  display: flex;
  flex-direction: column;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: var(--color-surface-sunken);
  border-bottom: 1px solid var(--color-border-heavy);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.log-title {
  font-family: var(--font-mono-header);
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.log-count {
  font-size: 0.68rem;
  color: var(--color-text-muted);
}

.clear-log-btn {
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text-dim);
  padding: 2px 6px;
  font-family: inherit;
  font-size: 0.68rem;
  cursor: pointer;
}

.clear-log-btn:hover {
  border-color: var(--color-alert-red);
  color: var(--color-alert-red);
}

.terminal-screen {
  background: #050505;
  padding: 12px 16px;
  max-height: 240px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 0.72rem;
  line-height: 1.4;
}

.log-line {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.log-time {
  color: var(--color-text-muted);
  font-size: 0.68rem;
}

.log-type {
  color: var(--color-text-dim);
  font-weight: 700;
}

.log-status {
  font-weight: 800;
  font-size: 0.68rem;
}

.log-msg {
  color: var(--color-text-main);
  flex: 1;
  word-break: break-all;
}

.log-error .log-msg {
  color: var(--color-alert-red);
}

.log-success .log-msg {
  color: var(--color-neon-green);
}
</style>
