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
          <span v-if="aiState.verification.valid" class="telemetry-val text-green">
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
      <button
        type="button"
        class="tactical-btn theme-toggle"
        :aria-pressed="theme === 'light'"
        :aria-label="theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'"
        :title="theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'"
        @click="toggleTheme"
      >
        <span aria-hidden="true">{{ theme === 'dark' ? '☀' : '☾' }}</span>
        {{ theme === 'dark' ? 'LIGHT MODE' : 'DARK MODE' }}
      </button>
      <div class="status-badge-group">
        <span class="ascii-badge">TASKS: {{ taskStats.total }}</span>
        <span class="ascii-badge text-amber">IN PROGRESS: {{ taskStats.inProgress }}</span>
        <span class="ascii-badge text-green">DONE: {{ taskStats.completed }}</span>
      </div>

      <div class="engine-indicator-badge">
        <span class="indicator-prefix">[MODEL]</span>
        <span class="indicator-val text-amber">{{ aiState.config.model }}</span>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useTheme } from '../../composables/useTheme.js';

import { useTacticalHeader } from '../../composables/useTacticalHeader.js';

const { theme, toggleTheme } = useTheme();

const {
  taskStats,
  aiState,
  activeEngine,
  currentTimestamp
} = useTacticalHeader();
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
  background: var(--bg-card);
  border: 1px solid var(--color-border-heavy);
  font-size: 0.75rem;
  font-weight: 800;
}

.indicator-prefix {
  color: var(--color-text-muted);
}
.theme-toggle {
  gap: 8px;
  padding: 8px 12px;
  white-space: nowrap;
  font-size: 0.75rem;
}

@media (max-width: 1200px) {
  .tactical-header, .header-actions, .system-telemetry, .brand-logo, .status-badge-group {
    flex-wrap: wrap;
  }

  .tactical-header {
    gap: 12px;
  }
}
</style>
