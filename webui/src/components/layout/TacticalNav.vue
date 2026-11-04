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
      <div class="status-indicator">
        <span class="status-led" :class="backendOnline ? 'led-online' : 'led-offline'"></span>
        <span class="status-code">
          {{ backendOnline ? 'SYSTEM READY // BACKEND CONNECTED' : 'SYSTEM OFFLINE // CONNECTING' }}
        </span>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { useTaskStore } from '../../store/useTaskStore.js';

defineProps({
  currentTab: {
    type: String,
    required: true
  }
});

defineEmits(['update:currentTab']);

const { backendOnline } = useTaskStore();
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
  font-family: var(--font-mono-code);
  font-size: 0.72rem;
  letter-spacing: 0.08em;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  border-left: 2px solid var(--color-border-heavy);
  padding-left: 12px;
}

.status-led {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}

.led-online {
  background: #00E676;
  box-shadow: 0 0 6px #00E676;
}

.led-offline {
  background: #FF3D00;
  box-shadow: 0 0 6px #FF3D00;
}

.status-code {
  color: var(--color-text-muted);
}
</style>
