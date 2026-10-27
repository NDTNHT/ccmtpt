<template>
  <div class="telemetry-notifier-container" v-if="notifications.length > 0">
    <transition-group name="telemetry-toast" tag="div" class="toast-stack">
      <div
        v-for="item in notifications"
        :key="item.id"
        class="telemetry-toast-box"
        :class="`toast-${item.level}`"
      >
        <div class="toast-header">
          <span class="ascii-badge">[{{ item.level.toUpperCase() }}]</span>
          <span class="toast-time">{{ item.timestamp }}</span>
          <button class="toast-close" @click="dismiss(item.id)">[X]</button>
        </div>
        <div class="toast-body">
          {{ item.message }}
        </div>
      </div>
    </transition-group>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const notifications = ref([]);
let nextId = 1;

function notify(message, level = 'info', durationMs = 4000) {
  const id = nextId++;
  const item = {
    id,
    message,
    level,
    timestamp: new Date().toLocaleTimeString()
  };
  notifications.value.push(item);

  if (durationMs > 0) {
    setTimeout(() => {
      dismiss(id);
    }, durationMs);
  }
}

function dismiss(id) {
  notifications.value = notifications.value.filter((n) => n.id !== id);
}

defineExpose({
  notify
});
</script>

<style scoped>
.telemetry-notifier-container {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  width: 380px;
  max-width: 90vw;
  pointer-events: none;
}

.toast-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.telemetry-toast-box {
  pointer-events: auto;
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  padding: 12px 16px;
  box-shadow: 4px 4px 0px #000;
  font-family: var(--font-mono-code);
  font-size: 0.78rem;
}

.toast-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.toast-time {
  color: var(--color-text-muted);
  font-size: 0.7rem;
}

.toast-close {
  background: transparent;
  border: none;
  color: var(--color-text-dim);
  cursor: pointer;
  font-family: inherit;
  font-size: 0.75rem;
}

.toast-close:hover {
  color: var(--color-alert-red);
}

.toast-body {
  color: var(--color-text-main);
  line-height: 1.4;
}

.toast-info {
  border-left: 4px solid var(--color-border-heavy);
}

.toast-success {
  border-left: 4px solid var(--color-neon-green);
}

.toast-warn {
  border-left: 4px solid var(--color-neon-amber);
}

.toast-error {
  border-left: 4px solid var(--color-alert-red);
}

.telemetry-toast-enter-active,
.telemetry-toast-leave-active {
  transition: all 0.25s ease;
}

.telemetry-toast-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.telemetry-toast-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
