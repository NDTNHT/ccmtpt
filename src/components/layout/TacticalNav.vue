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
      <div class="db-actions">
        <button class="db-action-btn" title="Export database to JSON file" @click="handleExport">
          [EXPORT DB ⤓]
        </button>
        <label class="db-action-btn file-label" title="Import database from JSON file">
          [IMPORT DB ⤒]
          <input type="file" accept=".json" class="hidden-file-input" @change="handleImportFile" />
        </label>
      </div>

      <div class="status-indicator">
        <span class="status-led led-online"></span>
        <span class="status-code">
          LOCAL-FIRST CLIENT // PERSISTENCE: LOCALSTORAGE
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

const emit = defineEmits(['update:currentTab', 'notify']);

const { exportDatabase, importDatabase } = useTaskStore();

function handleExport() {
  exportDatabase();
  emit('notify', 'Database exported as JSON backup.', 'success');
}

function handleImportFile(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = (e) => {
    try {
      importDatabase(e.target.result);
      emit('notify', 'Database backup successfully imported!', 'success');
    } catch (err) {
      emit('notify', `Import failed: ${err.message}`, 'error');
    }
  };
  reader.readAsText(file);
  event.target.value = '';
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
  gap: 16px;
  font-family: var(--font-mono-code);
  font-size: 0.72rem;
  letter-spacing: 0.08em;
}

.db-actions {
  display: flex;
  gap: 8px;
}

.db-action-btn {
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text-dim);
  font-family: var(--font-mono-code);
  font-size: 0.68rem;
  padding: 4px 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  transition: all 0.15s ease;
}

.db-action-btn:hover {
  border-color: var(--color-neon-amber);
  color: var(--color-neon-amber);
}

.file-label {
  position: relative;
  cursor: pointer;
}

.hidden-file-input {
  display: none;
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
  background: var(--color-neon-green);
  box-shadow: 0 0 6px var(--color-neon-green);
}

.status-code {
  color: var(--color-text-muted);
}
</style>
