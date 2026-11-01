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
      <RouterView v-slot="{ Component }">
        <component :is="Component" @notify="handleNotify" />
      </RouterView>
    </main>

    <!-- Telemetry Notification Stack -->
    <TelemetryNotifier ref="notifierRef" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import TacticalHeader from './components/layout/TacticalHeader.vue';
import TacticalNav from './components/layout/TacticalNav.vue';
import TelemetryNotifier from './components/layout/TelemetryNotifier.vue';
import { RouterView, useRoute, useRouter } from 'vue-router';
import { useTaskStore } from './store/useTaskStore.js';
import { useAiStore } from './store/useAiStore.js';

const route = useRoute();
const router = useRouter();
const activeTab = computed({
  get: () => route.name === 'ai' ? 'ai' : 'tasks',
  set: tab => router.push(tab === 'ai' ? '/ai' : '/tasks')
});
const notifierRef = ref(null);

const { fetchTasks, state: taskState } = useTaskStore();
const { fetchConfig, fetchProviders } = useAiStore();

onMounted(async () => {
  try {
    await Promise.allSettled([
      fetchTasks(),
      ...(taskState.localMode ? [] : [fetchConfig(), fetchProviders()])
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
@media (max-width: 640px) { .tactical-main-content { padding: 12px; } }
</style>
