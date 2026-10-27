<template>
  <div class="tactical-app-root">
    <!-- CRT Screen Scanline Overlay -->
    <div class="crt-overlay"></div>
    <div class="crt-scanline"></div>

    <!-- System Header -->
    <TacticalHeader />

    <!-- Module Navigation -->
    <TacticalNav v-model:currentTab="activeTab" />

    <!-- Main Operational Workspace -->
    <main class="tactical-main-content">
      <transition name="view-fade" mode="out-in">
        <!-- Module 01: Tasks & Checklists -->
        <TaskOpsView
          v-if="activeTab === 'tasks'"
          @notify="handleNotify"
        />

        <!-- Module 02: AI Copilot & Neural Gateway -->
        <AiCopilotView
          v-else-if="activeTab === 'ai'"
          @notify="handleNotify"
        />
      </transition>
    </main>

    <!-- Telemetry Notification Stack -->
    <TelemetryNotifier ref="notifierRef" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import TacticalHeader from './components/layout/TacticalHeader.vue';
import TacticalNav from './components/layout/TacticalNav.vue';
import TelemetryNotifier from './components/layout/TelemetryNotifier.vue';
import TaskOpsView from './components/tasks/TaskOpsView.vue';
import AiCopilotView from './components/ai/AiCopilotView.vue';
import { useTaskStore } from './store/useTaskStore.js';
import { useAiStore } from './store/useAiStore.js';

const activeTab = ref('tasks');
const notifierRef = ref(null);

const { fetchTasks } = useTaskStore();
const { fetchConfig, fetchProviders } = useAiStore();

onMounted(async () => {
  try {
    await Promise.allSettled([
      fetchTasks(),
      fetchConfig(),
      fetchProviders()
    ]);
  } catch (err) {
    console.warn('Initial telemetry synchronization note:', err);
  }
});

function handleNotify(message, level = 'info') {
  if (notifierRef.value) {
    notifierRef.value.notify(message, level);
  }
}
</script>

<style scoped>
.tactical-app-root {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--color-bg);
  color: var(--color-text-main);
  position: relative;
  overflow-x: hidden;
}

.tactical-main-content {
  flex: 1;
  padding: 24px;
  max-width: 1600px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}

.view-fade-enter-active,
.view-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.view-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.view-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
