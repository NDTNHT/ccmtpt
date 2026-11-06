<template>
  <div class="tactical-modal-backdrop" v-if="isOpen" @click.self="$emit('close')">
    <div class="tactical-modal-window">
      <!-- Modal Header -->
      <div class="modal-header">
        <div class="header-tag">
          <span class="ascii-badge text-alert">[TASK // {{ task?.id }}]</span>
          <span class="modal-title">{{ task?.title }}</span>
        </div>
        <button class="modal-close-btn" @click="$emit('close')">[CLOSE / ESC]</button>
      </div>

      <!-- Modal Body -->
      <div class="modal-body" v-if="task">
        <div class="spec-grid">
          <div class="spec-item">
            <span class="spec-label">STATUS:</span>
            <span class="spec-value" :class="statusColorClass">{{ task.status }}</span>
          </div>
          <div class="spec-item">
            <span class="spec-label">PRIORITY:</span>
            <span class="spec-value text-alert">{{ task.priority }}</span>
          </div>
          <div class="spec-item">
            <span class="spec-label">DUE DATE:</span>
            <span class="spec-value">{{ task.dueDate || 'UNASSIGNED' }}</span>
          </div>
          <div class="spec-item">
            <span class="spec-label">PROJECT:</span>
            <span class="spec-value text-dim">{{ task.projectId || 'DEFAULT' }}</span>
          </div>
        </div>

        <div class="section-divider">/// DESCRIPTION</div>
        <div class="desc-box">
          {{ task.description || 'No description provided.' }}
        </div>

        <!-- AI Assistant Action Bar -->
        <div class="section-divider">/// AI ASSISTANT ACTIONS</div>
        <div class="ai-controls-bar">
          <button 
            class="ai-action-btn"
            :disabled="aiLoading"
            @click="onAiDecompose"
          >
            [1] BREAK DOWN SUBTASKS
          </button>
          <button 
            class="ai-action-btn"
            :disabled="aiLoading"
            @click="onAiEstimateDeadline"
          >
            [2] ESTIMATE DUE DATE
          </button>
          <button 
            class="ai-action-btn"
            :disabled="aiLoading"
            @click="onAiAnalyzePriority"
          >
            [3] SUGGEST PRIORITY
          </button>
          <button 
            class="ai-action-btn"
            :disabled="aiLoading"
            @click="onAiGenerateSummary"
          >
            [4] SUMMARIZE TASK
          </button>
        </div>

        <!-- AI Real-Time Feedback Stream -->
        <div class="ai-telemetry-banner" v-if="aiFeedback">
          <div class="ai-banner-header">
            <span class="ascii-badge">[AI RECOMMENDATION]</span>
            <span class="ai-time">{{ aiFeedback.timestamp }}</span>
          </div>
          <p class="ai-banner-text">{{ aiFeedback.text }}</p>
          <div class="ai-banner-actions" v-if="aiFeedback.applyPayload">
            <button class="tactical-btn-sm text-green" @click="applyAiRecommendation">
              [APPLY TO TASK]
            </button>
          </div>
        </div>

        <!-- Subtasks Checklist Section -->
        <div class="section-divider">
          <span>/// SUBTASKS ({{ completedCount }}/{{ totalSubTasks }})</span>
        </div>

        <div class="checklist-container">
          <div v-if="!task.subTasks || task.subTasks.length === 0" class="empty-checklist">
            No subtasks yet. Click "[1] BREAK DOWN SUBTASKS" to generate.
          </div>

          <div v-else class="checklist-items">
            <div
              v-for="st in task.subTasks"
              :key="st.id"
              class="subtask-row"
              :class="{ 'row-completed': st.completed }"
              @click="$emit('toggle-subtask', task.id, st.id)"
            >
              <span class="checkbox-indicator">
                {{ st.completed ? '[X]' : '[ ]' }}
              </span>
              <span class="subtask-text">{{ st.title }}</span>
              <span v-if="st.completed" class="subtask-state">COMPLETED</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Modal Footer -->
      <div class="modal-footer">
        <div class="footer-actions">
          <button 
            v-if="task?.status === 'TODO'" 
            class="tactical-btn-sm text-amber"
            @click="$emit('start-task', task.id)"
          >
            [START TASK]
          </button>
          <button 
            v-if="task?.status === 'IN_PROGRESS'" 
            class="tactical-btn-sm text-green"
            @click="$emit('complete-task', task.id)"
          >
            [MARK COMPLETED]
          </button>
          <button 
            v-if="task?.status !== 'COMPLETED' && task?.status !== 'CANCELLED'" 
            class="tactical-btn-sm text-dim"
            @click="$emit('cancel-task', task.id)"
          >
            [CANCEL TASK]
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useTaskDetail } from '../../composables/useTaskDetail.js';

const props = defineProps({
  isOpen: {
    type: Boolean,
    default: false
  },
  task: {
    type: Object,
    default: null
  }
});

const emit = defineEmits([
  'close',
  'toggle-subtask',
  'start-task',
  'complete-task',
  'cancel-task',
  'notify'
]);

const {
  aiLoading,
  aiFeedback,
  totalSubTasks,
  completedCount,
  statusColorClass,
  onAiDecompose,
  onAiEstimateDeadline,
  onAiAnalyzePriority,
  onAiGenerateSummary,
  applyAiRecommendation
} = useTaskDetail({ props, emit });
</script>

<style scoped>
.tactical-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.88);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  padding: 20px;
}

.tactical-modal-window {
  background: #111111 !important;
  border: 2px solid #4A4A4A !important;
  width: 780px;
  max-width: 95vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.98), 0 0 0 1px #222222;
  font-family: var(--font-mono-code);
  position: relative;
}

/* Modal Window Corner Tech Brackets (Image 1 Style) */
.tactical-modal-window::before {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  width: 16px;
  height: 16px;
  border-top: 3px solid var(--color-neon-amber);
  border-left: 3px solid var(--color-neon-amber);
  pointer-events: none;
  z-index: 10;
}

.tactical-modal-window::after {
  content: '';
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 16px;
  height: 16px;
  border-bottom: 3px solid var(--color-neon-amber);
  border-right: 3px solid var(--color-neon-amber);
  pointer-events: none;
  z-index: 10;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 2px solid var(--color-border-heavy);
  background: #080808 !important;
}

.header-tag {
  display: flex;
  align-items: center;
  gap: 12px;
}

.modal-title {
  font-family: var(--font-mono-header);
  font-size: 1rem;
  font-weight: 800;
  color: var(--color-text-main);
  letter-spacing: 0.05em;
}

.modal-close-btn {
  background: #1A1A1A;
  border: 1px solid var(--color-border-heavy);
  color: var(--color-text-dim);
  padding: 5px 12px;
  cursor: pointer;
  font-family: inherit;
  font-size: 0.75rem;
  font-weight: 700;
  transition: all 0.15s ease;
}

.modal-close-btn:hover {
  background: var(--color-alert-red);
  border-color: var(--color-alert-red);
  color: #000000;
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #111111;
}

.spec-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  background: #181818 !important;
  padding: 12px;
  border: 1px solid var(--color-border-heavy);
}

.spec-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.spec-label {
  font-size: 0.68rem;
  color: var(--color-text-muted);
  font-weight: 700;
}

.spec-value {
  font-size: 0.85rem;
  font-weight: 800;
}

.section-divider {
  font-size: 0.72rem;
  color: var(--color-text-dim);
  letter-spacing: 0.1em;
  font-weight: 800;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 4px;
}

.desc-box {
  background: #0A0A0A !important;
  border: 1px solid var(--color-border-heavy);
  padding: 12px;
  font-family: var(--font-mono-body);
  font-size: 0.85rem;
  line-height: 1.5;
  color: var(--color-text-main);
}

.ai-controls-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.ai-action-btn {
  background: #1A1A1A !important;
  border: 1px solid #444444 !important;
  color: var(--color-neon-amber) !important;
  padding: 10px 8px !important;
  font-family: var(--font-mono-code);
  font-size: 0.72rem;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: center;
  position: relative;
}

/* Corner bracket accents on AI action buttons */
.ai-action-btn::before {
  content: '';
  position: absolute;
  top: -1px;
  left: -1px;
  width: 5px;
  height: 5px;
  border-top: 2px solid var(--color-neon-amber);
  border-left: 2px solid var(--color-neon-amber);
}

.ai-action-btn::after {
  content: '';
  position: absolute;
  bottom: -1px;
  right: -1px;
  width: 5px;
  height: 5px;
  border-bottom: 2px solid var(--color-neon-amber);
  border-right: 2px solid var(--color-neon-amber);
}

.ai-action-btn:hover:not(:disabled) {
  background: #282828 !important;
  border-color: var(--color-neon-amber) !important;
  box-shadow: 0 0 10px rgba(255, 184, 0, 0.3);
}

.ai-action-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.ai-telemetry-banner {
  background: #1A1608 !important;
  border: 1px solid var(--color-neon-amber);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-banner-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ai-time {
  font-size: 0.7rem;
  color: var(--color-text-muted);
}

.ai-banner-text {
  font-size: 0.84rem;
  line-height: 1.45;
  margin: 0;
  color: var(--color-text-main);
}

.ai-banner-actions {
  display: flex;
  justify-content: flex-end;
}

.checklist-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.empty-checklist {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  font-style: italic;
  padding: 12px;
  background: #0A0A0A;
  border: 1px dashed var(--color-border);
}

.checklist-items {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.subtask-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: #181818 !important;
  border: 1px solid #333333 !important;
  cursor: pointer;
  transition: all 0.15s ease;
}

.subtask-row:hover {
  background: #242424 !important;
  border-color: var(--color-neon-amber) !important;
}

.row-completed {
  opacity: 0.7;
}

.row-completed .subtask-text {
  text-decoration: line-through;
  color: var(--color-text-muted);
}

.checkbox-indicator {
  font-weight: 900;
  color: var(--color-neon-green);
}

.subtask-text {
  flex: 1;
  font-size: 0.82rem;
  color: var(--color-text-main);
}

.subtask-state {
  font-size: 0.7rem;
  color: var(--color-text-muted);
  font-weight: 700;
}

.add-subtask-form {
  display: flex;
  gap: 8px;
  margin-top: 6px;
}

.tactical-input {
  flex: 1;
  background: #0A0A0A !important;
  border: 1px solid var(--color-border-heavy);
  padding: 8px 12px;
  color: var(--color-text-main);
  font-family: var(--font-mono-code);
  font-size: 0.78rem;
}

.tactical-input:focus {
  outline: none;
  border-color: var(--color-neon-amber);
}

.tactical-btn-sm {
  background: #1E1E1E !important;
  border: 1px solid #444444 !important;
  color: var(--color-text-main);
  padding: 8px 16px;
  font-family: var(--font-mono-code);
  font-size: 0.75rem;
  font-weight: 800;
  cursor: pointer;
  position: relative;
}

.tactical-btn-sm:hover:not(:disabled) {
  background: #2E2E2E !important;
  border-color: var(--color-neon-amber) !important;
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-top: 2px solid var(--color-border-heavy);
  background: #080808 !important;
}

.footer-status {
  font-size: 0.7rem;
  color: var(--color-text-muted);
}

.footer-actions {
  display: flex;
  gap: 8px;
}
</style>
