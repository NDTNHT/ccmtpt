<template>
  <div
    class="tactical-task-card"
    :class="{
      'card-selected': isSelected,
      'card-completed': task.status === 'COMPLETED',
      'card-cancelled': task.status === 'CANCELLED',
      'card-critical': task.priority === 'CRITICAL'
    }"
    @click="$emit('select', task)"
    @mouseover="$emit('hover', task)"
  >
    <div class="card-header">
      <span class="task-id">{{ task.id }}</span>
      <div class="badge-cluster">
        <!-- Status Directive Branching -->
        <span v-if="task.status === 'TODO'" class="ascii-badge">[TODO]</span>
        <span v-else-if="task.status === 'IN_PROGRESS'" class="ascii-badge badge-warning">[ACTIVE]</span>
        <span v-else-if="task.status === 'COMPLETED'" class="ascii-badge badge-success">[COMPLETED]</span>
        <span v-else class="ascii-badge badge-dim">[CANCELLED]</span>

        <!-- Priority Directive Branching -->
        <span v-if="task.priority === 'CRITICAL'" class="ascii-badge badge-danger">!CRIT!</span>
        <span v-else-if="task.priority === 'HIGH'" class="ascii-badge badge-warning">HIGH</span>
        <span v-else class="ascii-badge badge-dim">{{ task.priority }}</span>
      </div>
    </div>

    <div class="card-body">
      <h3 class="task-title">{{ task.title }}</h3>
      <p class="task-desc" v-if="task.description">{{ task.description }}</p>
      
      <!-- Subtasks Checklist Snapshot -->
      <div class="checklist-preview" v-if="task.subTasks && task.subTasks.length > 0">
        <div class="checklist-header">
          <span>SUBTASKS // {{ completedCount }}/{{ task.subTasks.length }}</span>
          <span>{{ completionPercent }}%</span>
        </div>
        <div class="checklist-progress-bar">
          <div class="progress-fill" :style="{ width: `${completionPercent}%` }"></div>
        </div>
        <ul class="mini-subtasks-list">
          <li 
            v-for="(st, idx) in task.subTasks.slice(0, 3)" 
            :key="st.id || idx" 
            class="mini-st-item"
            :class="{ 'item-done': st.completed }"
          >
            <span class="bullet">{{ st.completed ? '[x]' : '[ ]' }}</span>
            <span class="subtask-label">{{ st.title }}</span>
          </li>
          <li v-if="task.subTasks.length > 3" class="more-subtasks">
            + {{ task.subTasks.length - 3 }} more subtasks...
          </li>
        </ul>
      </div>

      <div class="card-meta">
        <span class="meta-item">
          <span class="meta-label">DUE:</span>
          <span class="meta-val" :class="{ 'text-alert': isOverdue }">{{ task.dueDate || 'N/A' }}</span>
        </span>
        <span class="meta-divider">|</span>
        <span class="meta-item">
          <span class="meta-label">PROJECT:</span>
          <span class="meta-val text-dim">{{ task.projectId || 'DEFAULT' }}</span>
        </span>
      </div>
    </div>

    <div class="card-footer">
      <div class="action-btn-group">
        <button 
          v-if="task.status === 'TODO'" 
          class="mini-btn text-amber"
          title="Start Task"
          @click.stop="$emit('start', task.id)"
        >
          [START]
        </button>
        <button 
          v-if="task.status === 'IN_PROGRESS'" 
          class="mini-btn text-green"
          title="Complete Task"
          @click.stop="$emit('complete', task.id)"
        >
          [COMPLETE]
        </button>
        <button 
          v-if="task.status !== 'COMPLETED' && task.status !== 'CANCELLED'" 
          class="mini-btn text-dim"
          title="Cancel Task"
          @click.stop="$emit('cancel', task.id)"
        >
          [CANCEL]
        </button>
      </div>

      <button class="mini-btn ai-inspect-btn" @click.stop="$emit('inspect', task)">
        [AI COPILOT &gt;&gt;]
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  task: {
    type: Object,
    required: true
  },
  isSelected: {
    type: Boolean,
    default: false
  }
});

defineEmits(['select', 'hover', 'start', 'complete', 'cancel', 'inspect']);

const completedCount = computed(() => {
  if (!props.task.subTasks) return 0;
  return props.task.subTasks.filter((s) => s.completed).length;
});

const completionPercent = computed(() => {
  if (!props.task.subTasks || props.task.subTasks.length === 0) return 0;
  return Math.round((completedCount.value / props.task.subTasks.length) * 100);
});

const isOverdue = computed(() => {
  if (!props.task.dueDate || props.task.status === 'COMPLETED') return false;
  return new Date(props.task.dueDate).getTime() < Date.now();
});
</script>

<style scoped>
.tactical-task-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
  position: relative;
}

.tactical-task-card:hover {
  background: var(--color-surface-hover);
  border-color: var(--color-border-focus);
}

.card-selected {
  border: 1px solid var(--color-neon-amber) !important;
  background: var(--color-surface-hover);
}

.card-completed {
  border-left: 4px solid var(--color-neon-green);
  opacity: 0.85;
}

.card-cancelled {
  border-left: 4px solid var(--color-text-muted);
  opacity: 0.6;
}

.card-critical {
  border-left: 4px solid var(--color-alert-red);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-id {
  font-family: var(--font-mono-code);
  font-size: 0.72rem;
  color: var(--color-text-muted);
  letter-spacing: 0.05em;
}

.badge-cluster {
  display: flex;
  gap: 6px;
}

.badge-warning {
  border-color: var(--color-neon-amber);
  color: var(--color-neon-amber);
}

.badge-success {
  border-color: var(--color-neon-green);
  color: var(--color-neon-green);
}

.badge-danger {
  border-color: var(--color-alert-red);
  color: var(--color-alert-red);
  font-weight: 900;
}

.badge-dim {
  border-color: var(--color-border);
  color: var(--color-text-dim);
}

.task-title {
  font-family: var(--font-mono-header);
  font-size: 0.92rem;
  letter-spacing: 0.05em;
  font-weight: 700;
  margin: 0 0 6px 0;
  color: var(--color-text-main);
}

.task-desc {
  font-family: var(--font-mono-body);
  font-size: 0.78rem;
  color: var(--color-text-dim);
  line-height: 1.4;
  margin: 0 0 10px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.checklist-preview {
  background: var(--color-surface-sunken);
  border: 1px solid var(--color-border);
  padding: 8px 10px;
  font-family: var(--font-mono-code);
  font-size: 0.72rem;
}

.checklist-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  color: var(--color-text-dim);
  font-weight: 700;
}

.checklist-progress-bar {
  width: 100%;
  height: 4px;
  background: var(--color-bg);
  margin-bottom: 6px;
}

.progress-fill {
  height: 100%;
  background: var(--color-neon-green);
  transition: width 0.3s ease;
}

.mini-subtasks-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.mini-st-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--color-text-dim);
}

.mini-st-item.item-done {
  color: var(--color-text-muted);
  text-decoration: line-through;
}

.bullet {
  color: var(--color-text-muted);
  font-size: 0.68rem;
}

.subtask-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.more-subtasks {
  font-size: 0.68rem;
  color: var(--color-text-muted);
  font-style: italic;
  padding-left: 14px;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-mono-code);
  font-size: 0.72rem;
  margin-top: 8px;
}

.meta-label {
  color: var(--color-text-muted);
}

.meta-val {
  color: var(--color-text-dim);
}

.meta-divider {
  color: var(--color-border);
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid var(--color-border);
  padding-top: 10px;
}

.action-btn-group {
  display: flex;
  gap: 8px;
}

.mini-btn {
  background: var(--bg-card) !important;
  border: 1px solid var(--color-border) !important;
  padding: 4px 10px;
  font-family: var(--font-mono-code);
  font-size: 0.72rem;
  font-weight: 800;
  cursor: pointer;
  color: var(--color-text-main);
  transition: all 0.15s ease;
}

.mini-btn:hover {
  background: var(--color-surface-hover) !important;
  border-color: var(--color-neon-amber) !important;
  color: var(--color-text-main) !important;
}

.mini-btn.text-amber:hover {
  color: var(--color-neon-amber);
  border-color: var(--color-neon-amber);
}

.mini-btn.text-green:hover {
  color: var(--color-neon-green);
  border-color: var(--color-neon-green);
}

.ai-inspect-btn {
  background: var(--bg-card) !important;
  border: 1px solid var(--color-border-heavy) !important;
  color: var(--color-neon-amber) !important;
  padding: 5px 12px;
  font-weight: 800;
  position: relative;
}

/* Corner bracket accents on AI inspect button (Image 1 style) */
.ai-inspect-btn::before {
  content: '';
  position: absolute;
  top: -1px;
  left: -1px;
  width: 5px;
  height: 5px;
  border-top: 2px solid var(--color-neon-amber);
  border-left: 2px solid var(--color-neon-amber);
}

.ai-inspect-btn::after {
  content: '';
  position: absolute;
  bottom: -1px;
  right: -1px;
  width: 5px;
  height: 5px;
  border-bottom: 2px solid var(--color-neon-amber);
  border-right: 2px solid var(--color-neon-amber);
}

.ai-inspect-btn:hover {
  background: var(--color-surface-hover) !important;
  border-color: var(--color-neon-amber) !important;
  box-shadow: 0 0 10px var(--accent-glow);
}
</style>
