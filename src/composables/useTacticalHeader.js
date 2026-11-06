import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useTaskStore } from '../store/useTaskStore.js';
import { useAiStore } from '../store/useAiStore.js';
import { getCurrentUtcTime } from '../domain/dateUtils.js';

export function useTacticalHeader() {
  const { taskStats } = useTaskStore();
  const { state: aiState, activeProvider } = useAiStore();

  const activeEngine = computed(() => {
    return `${activeProvider.value.name.toUpperCase()} (${aiState.config.model})`;
  });

  const currentTimestamp = ref(getCurrentUtcTime());
  let timer = null;

  onMounted(() => {
    timer = setInterval(() => {
      currentTimestamp.value = getCurrentUtcTime();
    }, 1000);
  });

  onUnmounted(() => {
    if (timer) clearInterval(timer);
  });

  return {
    taskStats,
    aiState,
    activeProvider,
    activeEngine,
    currentTimestamp
  };
}

