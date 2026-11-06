<template>
  <div class="tactical-modal-backdrop" v-if="isOpen" @click.self="handleClose">
    <div class="tactical-modal-window">
      <!-- Modal Header -->
      <div class="modal-header">
        <div class="header-tag">
          <span class="ascii-badge text-alert">[TASK // NEW]</span>
          <span class="modal-title">CREATE NEW TASK</span>
        </div>
        <button class="modal-close-btn" @click="handleClose">[CLOSE / ESC]</button>
      </div>

      <!-- Modal Body / Form -->
      <form class="modal-body" @submit.prevent="handleSubmit">
        <!-- Section: Title -->
        <div class="form-section">
          <div class="section-label-row">
            <label class="form-label">
              <span class="label-prefix">&gt;&gt;</span>
              TASK TITLE
              <span class="required-star">*</span>
            </label>
            <span class="char-count" :class="{ 'text-hazard': form.title.length > 100 }">
              {{ form.title.length }} / 100
            </span>
          </div>
          <input
            ref="titleInputRef"
            v-model="form.title"
            type="text"
            class="tactical-input title-input"
            placeholder="e.g. Implement user authentication..."
            maxlength="100"
            required
            @keydown.enter.prevent
          />
          <div v-if="titleError" class="validation-error">
            <span class="error-badge">[!]</span> {{ titleError }}
          </div>
        </div>

        <!-- Section: Priority & Project Assignment -->
        <div class="form-grid-2">
          <!-- Priority Selector -->
          <div class="form-section">
            <label class="form-label">
              <span class="label-prefix">&gt;&gt;</span>
              PRIORITY
            </label>
            <div class="priority-chips-group">
              <button
                v-for="p in priorityOptions"
                :key="p.value"
                type="button"
                class="priority-chip"
                :class="[p.value.toLowerCase(), { active: form.priority === p.value }]"
                @click="form.priority = p.value"
              >
                <span class="chip-led"></span>
                <span class="chip-text">{{ p.label }}</span>
              </button>
            </div>
          </div>

          <!-- Project Selector -->
          <div class="form-section project-section">
            <div class="section-label-row">
              <label class="form-label">
                <span class="label-prefix">&gt;&gt;</span>
                PROJECT
              </label>
              <button
                type="button"
                class="new-project-toggle-btn"
                @click="showNewProjectForm = !showNewProjectForm"
              >
                {{ showNewProjectForm ? '[-] CANCEL' : '[+ NEW PROJECT]' }}
              </button>
            </div>

            <!-- Inline Regular Project Creation Box -->
            <div v-if="showNewProjectForm" class="new-project-inline-box">
              <div class="new-project-row">
                <input
                  v-model="newProjectName"
                  type="text"
                  class="tactical-input new-project-input"
                  placeholder="New project name (e.g. Mobile App)..."
                  maxlength="100"
                  @keydown.enter.prevent="handleCreateRegularProject"
                />
                <button
                  type="button"
                  class="tactical-btn-sm btn-create-proj"
                  :disabled="!newProjectName.trim() || newProjectLoading"
                  @click="handleCreateRegularProject"
                >
                  {{ newProjectLoading ? '[...]' : '[+ CREATE PROJECT]' }}
                </button>
              </div>
              <span class="new-project-note">
                [PROJECT TYPE: REGULAR]
              </span>
            </div>

            <select v-model="form.projectId" class="tactical-select project-select">
              <optgroup label="── REGULAR PROJECTS ──">
                <option
                  v-for="project in regularProjects"
                  :key="project.id"
                  :value="project.id"
                >
                  {{ project.name }} (Regular)
                </option>
                <option v-if="regularProjects.length === 0" disabled value="">
                  (No regular projects yet — click [+ NEW PROJECT])
                </option>
              </optgroup>
              <optgroup label="── INBOX (DEFAULT) ──">
                <option :value="inboxProject?.id || null">
                  {{ inboxProject ? inboxProject.name : 'INBOX' }} (Default Inbox)
                </option>
              </optgroup>
            </select>

            <!-- Project Type Indicator Badge -->
            <div class="project-type-indicator">
              <span
                class="type-badge"
                :class="selectedProjectType === 'REGULAR' ? 'badge-regular' : 'badge-inbox'"
              >
                PROJECT TYPE: [{{ selectedProjectType }}]
              </span>
              <span class="type-hint" v-if="selectedProjectType === 'REGULAR'">
                ✓ Organized inside a dedicated project
              </span>
              <span class="type-hint" v-else>
                ○ Quick triage queue for unassigned tasks
              </span>
            </div>
          </div>
        </div>

        <!-- Section: Deadline & Quick Presets -->
        <div class="form-section">
          <div class="section-label-row">
            <label class="form-label">
              <span class="label-prefix">&gt;&gt;</span>
              DUE DATE
            </label>
            <span class="sub-label">[DEADLINE]</span>
          </div>
          <div class="deadline-input-row">
            <input
              v-model="form.dueDate"
              type="date"
              class="tactical-date-input"
            />
            <div class="preset-btn-group">
              <button
                type="button"
                class="preset-btn"
                @click="applyDatePreset(1)"
              >
                [+1 DAY]
              </button>
              <button
                type="button"
                class="preset-btn"
                @click="applyDatePreset(3)"
              >
                [+3 DAYS]
              </button>
              <button
                type="button"
                class="preset-btn"
                @click="applyDatePreset(7)"
              >
                [+1 WEEK]
              </button>
              <button
                type="button"
                class="preset-btn"
                @click="applyDatePreset(14)"
              >
                [+2 WEEKS]
              </button>
            </div>
          </div>
        </div>

        <!-- Section: Description -->
        <div class="form-section">
          <div class="section-label-row">
            <label class="form-label">
              <span class="label-prefix">&gt;&gt;</span>
              DESCRIPTION
            </label>
            <span class="char-count" :class="{ 'text-hazard': form.description.length > 500 }">
              {{ form.description.length }} / 500
            </span>
          </div>
          <textarea
            v-model="form.description"
            class="tactical-textarea"
            rows="3"
            maxlength="500"
            placeholder="Add a detailed description, requirements, or acceptance criteria..."
          ></textarea>
        </div>

        <!-- Section: Checklist Builder -->
        <div class="form-section checklist-builder-section">
          <div class="section-label-row">
            <label class="form-label">
              <span class="label-prefix">&gt;&gt;</span>
              SUBTASKS / CHECKLIST ({{ form.subTasks.length }})
            </label>
            <button
              type="button"
              class="ai-suggest-btn"
              :disabled="!form.title.trim() || aiDraftLoading"
              @click="generateSmartCheckpoints"
            >
              {{ aiDraftLoading ? '[AI PROCESSING...]' : '[🤖 AI SUGGEST SUBTASKS]' }}
            </button>
          </div>

          <div class="subtask-add-row">
            <input
              v-model="newSubTaskTitle"
              type="text"
              class="tactical-input subtask-input"
              placeholder="Add a subtask (e.g. Write integration tests)..."
              @keydown.enter.prevent="handleAddSubTask"
            />
            <button
              type="button"
              class="tactical-btn-sm"
              :disabled="!newSubTaskTitle.trim()"
              @click="handleAddSubTask"
            >
              [+ ADD SUBTASK]
            </button>
          </div>

          <div v-if="form.subTasks.length > 0" class="subtask-list">
            <div
              v-for="(st, index) in form.subTasks"
              :key="index"
              class="subtask-item-row"
            >
              <span class="st-index">[{{ String(index + 1).padStart(2, '0') }}]</span>
              <span class="st-bracket">[ ]</span>
              <span class="st-title">{{ st.title }}</span>
              <button
                type="button"
                class="st-remove-btn"
                title="Remove subtask"
                @click="removeSubTask(index)"
              >
                [REMOVE]
              </button>
            </div>
          </div>
          <div v-else class="empty-subtasks-note">
            [ No subtasks added yet — enter above or use AI to generate ]
          </div>
        </div>

        <!-- Modal Footer Actions -->
        <div class="modal-footer">
          <div class="footer-telemetry">
            <span class="status-indicator"></span>
            <span class="telemetry-text">
              STATUS: {{ isSubmitting ? 'SAVING TASK...' : 'READY' }}
            </span>
          </div>

          <div class="footer-buttons">
            <button
              type="button"
              class="tactical-btn-cancel"
              :disabled="isSubmitting"
              @click="handleClose"
            >
              [CANCEL]
            </button>
            <button
              type="submit"
              class="tactical-btn-dispatch"
              :disabled="isSubmitting || !isFormValid"
            >
              <span v-if="isSubmitting">[SAVING...]</span>
              <span v-else>┌ [CREATE TASK] ┘</span>
            </button>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { useCreateTask } from '../../composables/useCreateTask.js';

const props = defineProps({
  isOpen: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['close', 'created', 'notify']);

const {
  form,
  titleInputRef,
  isSubmitting,
  newSubTaskTitle,
  showNewProjectForm,
  newProjectName,
  newProjectLoading,
  aiDraftLoading,
  inboxProject,
  regularProjects,
  selectedProjectType,
  priorityOptions,
  isFormValid,
  titleError,
  applyDatePreset,
  handleCreateRegularProject,
  handleAddSubTask,
  removeSubTask,
  generateSmartCheckpoints,
  handleClose,
  handleSubmit
} = useCreateTask({ props, emit });
</script>

<style scoped>
.tactical-modal-backdrop {
  position: fixed;
  inset: 0;
  background: var(--modal-backdrop);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  padding: 20px;
}

.tactical-modal-window {
  background: var(--color-surface) !important;
  border: 2px solid var(--color-border-heavy) !important;
  width: 780px;
  max-width: 95vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: var(--modal-shadow);
  font-family: var(--font-mono-code);
  position: relative;
}

/* Modal Window Corner Tech Brackets */
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
}

/* Header */
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: var(--bg-card);
  border-bottom: 2px solid var(--color-border);
}

.header-tag {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ascii-badge {
  font-size: 0.75rem;
  letter-spacing: 0.08em;
  font-weight: 700;
}

.text-alert {
  color: var(--color-neon-amber);
}

.modal-title {
  font-size: 0.95rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--color-text-main);
}

.modal-close-btn {
  background: transparent;
  border: 1px solid var(--color-border-heavy);
  color: var(--color-text-dim);
  font-family: var(--font-mono-code);
  font-size: 0.75rem;
  padding: 4px 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.modal-close-btn:hover {
  background: var(--color-alert-red);
  color: var(--color-on-accent);
  border-color: var(--color-alert-red);
}

/* Body */
.modal-body {
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.section-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-label {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-text-main);
  display: flex;
  align-items: center;
  gap: 6px;
}

.label-prefix {
  color: var(--color-neon-amber);
}

.required-star {
  color: var(--color-alert-red);
}

.sub-label {
  font-size: 0.7rem;
  color: var(--color-text-muted);
}

.char-count {
  font-size: 0.7rem;
  color: var(--color-text-muted);
}

.text-hazard {
  color: var(--color-alert-red) !important;
  font-weight: bold;
}

.validation-error {
  font-size: 0.7rem;
  color: var(--color-alert-red);
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
}

.error-badge {
  font-weight: bold;
}

/* Inputs */
.tactical-input,
.tactical-select,
.tactical-textarea,
.tactical-date-input {
  background: var(--color-surface-sunken);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  font-family: var(--font-mono-code);
  font-size: 0.85rem;
  padding: 8px 12px;
  outline: none;
  transition: border-color 0.15s ease;
}

.tactical-input:focus,
.tactical-select:focus,
.tactical-textarea:focus,
.tactical-date-input:focus {
  border-color: var(--color-neon-amber);
  box-shadow: 0 0 8px var(--accent-glow);
}

/* Project Sector & Regular Selector */
.project-section {
  position: relative;
}

.new-project-toggle-btn {
  background: transparent;
  border: 1px solid var(--color-neon-amber);
  color: var(--color-neon-amber);
  font-family: var(--font-mono-code);
  font-size: 0.68rem;
  font-weight: 700;
  padding: 2px 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.new-project-toggle-btn:hover {
  background: var(--color-neon-amber);
  color: var(--color-on-accent);
}

.new-project-inline-box {
  background: var(--color-surface);
  border: 1px dashed var(--color-neon-amber);
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 2px;
}

.new-project-row {
  display: flex;
  gap: 6px;
}

.new-project-input {
  flex: 1;
  font-size: 0.8rem;
  padding: 5px 8px;
  text-transform: uppercase;
}

.btn-create-proj {
  background: var(--color-neon-amber);
  color: var(--color-on-accent);
  border-color: var(--color-neon-amber);
  font-weight: 800;
  white-space: nowrap;
}

.btn-create-proj:hover:not(:disabled) {
  background: var(--color-neon-amber);
  color: var(--color-on-accent);
}

.new-project-note {
  font-size: 0.65rem;
  color: var(--color-text-dim);
  letter-spacing: 0.05em;
}

.project-select optgroup {
  background: var(--bg-card);
  color: var(--color-neon-amber);
  font-weight: 700;
  font-size: 0.75rem;
  padding: 4px;
}

.project-select option {
  background: var(--color-surface-sunken);
  color: var(--color-text-main);
  font-size: 0.82rem;
  padding: 4px;
}

.project-type-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.7rem;
  margin-top: 4px;
  flex-wrap: wrap;
}

.type-badge {
  font-weight: 700;
  font-size: 0.68rem;
  padding: 1px 6px;
  letter-spacing: 0.05em;
}

.badge-regular {
  background: var(--info-tint);
  border: 1px solid var(--accent-cyan);
  color: var(--accent-cyan);
}

.badge-inbox {
  background: var(--accent-tint);
  border: 1px solid var(--color-neon-amber);
  color: var(--color-neon-amber);
}

.type-hint {
  color: var(--color-text-dim);
  font-size: 0.68rem;
}

.title-input {
  font-size: 0.95rem;
  font-weight: bold;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  padding: 10px 12px;
}

.tactical-textarea {
  resize: vertical;
  line-height: 1.4;
}

/* Grid 2 Columns */
.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 640px) {
  .form-grid-2 {
    grid-template-columns: 1fr;
  }
}

/* Priority Chips */
.priority-chips-group {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

.priority-chip {
  background: var(--color-surface-sunken);
  border: 1px solid var(--color-border);
  color: var(--color-text-dim);
  padding: 8px 4px;
  font-family: var(--font-mono-code);
  font-size: 0.75rem;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.15s ease;
}

.chip-led {
  width: 6px;
  height: 6px;
  background: var(--color-border-heavy);
  display: inline-block;
}

.priority-chip.active {
  color: var(--color-text-main);
}

.priority-chip.low.active {
  border-color: var(--color-text-muted);
  background: var(--color-surface-hover);
}
.priority-chip.low.active .chip-led {
  background: var(--color-text-dim);
}

.priority-chip.medium.active {
  border-color: var(--accent-cyan);
  background: var(--info-tint);
  color: var(--accent-cyan);
}
.priority-chip.medium.active .chip-led {
  background: var(--accent-cyan);
}

.priority-chip.high.active {
  border-color: var(--color-neon-amber);
  background: var(--accent-tint);
  color: var(--color-neon-amber);
}
.priority-chip.high.active .chip-led {
  background: var(--color-neon-amber);
}

.priority-chip.critical.active {
  border-color: var(--color-alert-red);
  background: var(--danger-tint);
  color: var(--color-alert-red);
  animation: pulse-border 1.5s infinite;
}
.priority-chip.critical.active .chip-led {
  background: var(--color-alert-red);
}

@keyframes pulse-border {
  0%, 100% { border-color: var(--color-alert-red); }
  50% { border-color: var(--color-alert-red); }
}

/* Deadline & Presets */
.deadline-input-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tactical-date-input {
  flex: 1;
  min-width: 160px;
}

.preset-btn-group {
  display: flex;
  gap: 4px;
}

.preset-btn {
  background: var(--bg-card);
  border: 1px solid var(--color-border);
  color: var(--color-text-dim);
  font-family: var(--font-mono-code);
  font-size: 0.7rem;
  padding: 6px 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.preset-btn:hover {
  background: var(--color-surface-hover);
  color: var(--color-text-main);
  border-color: var(--color-neon-amber);
}

/* Checklist Builder */
.checklist-builder-section {
  background: var(--color-surface-sunken);
  border: 1px solid var(--color-surface-hover);
  padding: 12px;
}

.ai-suggest-btn {
  background: transparent;
  border: 1px solid var(--color-neon-amber);
  color: var(--color-neon-amber);
  font-family: var(--font-mono-code);
  font-size: 0.7rem;
  font-weight: bold;
  padding: 3px 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.ai-suggest-btn:hover:not(:disabled) {
  background: var(--color-neon-amber);
  color: var(--color-on-accent);
}

.ai-suggest-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  border-color: var(--color-border-heavy);
  color: var(--color-text-muted);
}

.subtask-add-row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.subtask-input {
  flex: 1;
}

.tactical-btn-sm {
  background: var(--bg-card);
  border: 1px solid var(--color-border-heavy);
  color: var(--color-text-main);
  font-family: var(--font-mono-code);
  font-size: 0.75rem;
  font-weight: bold;
  padding: 6px 12px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}

.tactical-btn-sm:hover:not(:disabled) {
  background: var(--color-surface-hover);
  border-color: var(--color-neon-amber);
  color: var(--color-neon-amber);
}

.tactical-btn-sm:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.subtask-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 140px;
  overflow-y: auto;
}

.subtask-item-row {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-surface);
  border: 1px solid var(--color-surface-hover);
  padding: 6px 10px;
  font-size: 0.8rem;
}

.st-index {
  color: var(--color-neon-amber);
  font-size: 0.75rem;
}

.st-bracket {
  color: var(--color-text-muted);
}

.st-title {
  flex: 1;
  color: var(--color-text-main);
  word-break: break-word;
}

.st-remove-btn {
  background: transparent;
  border: 1px solid transparent;
  color: var(--color-text-muted);
  font-family: var(--font-mono-code);
  font-size: 0.7rem;
  padding: 2px 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.st-remove-btn:hover {
  color: var(--color-alert-red);
  border-color: var(--color-alert-red);
  background: var(--danger-tint);
}

.empty-subtasks-note {
  font-size: 0.7rem;
  color: var(--color-text-muted);
  margin-top: 8px;
  text-align: center;
  padding: 8px;
  border: 1px dashed var(--color-surface-hover);
}

/* Modal Footer */
.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: var(--color-surface);
  border-top: 2px solid var(--color-border);
  gap: 12px;
  flex-wrap: wrap;
}

.footer-telemetry {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.75rem;
  color: var(--color-text-dim);
}

.status-indicator {
  width: 8px;
  height: 8px;
  background: var(--color-neon-green);
  display: inline-block;
  box-shadow: 0 0 6px var(--color-neon-green);
}

.footer-buttons {
  display: flex;
  gap: 10px;
}

.tactical-btn-cancel {
  background: transparent;
  border: 1px solid var(--color-border-heavy);
  color: var(--color-text-dim);
  font-family: var(--font-mono-code);
  font-size: 0.8rem;
  padding: 8px 16px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tactical-btn-cancel:hover:not(:disabled) {
  background: var(--color-surface-hover);
  color: var(--color-text-main);
  border-color: var(--color-text-muted);
}

.tactical-btn-dispatch {
  background: var(--color-neon-amber);
  border: 1px solid var(--color-neon-amber);
  color: var(--color-on-accent);
  font-family: var(--font-mono-code);
  font-size: 0.85rem;
  font-weight: 800;
  letter-spacing: 0.04em;
  padding: 8px 20px;
  cursor: pointer;
  transition: all 0.15s ease;
  position: relative;
}

.tactical-btn-dispatch:hover:not(:disabled) {
  background: var(--color-neon-amber);
  box-shadow: 0 0 14px var(--accent-glow);
}

.tactical-btn-dispatch:disabled {
  background: var(--color-border);
  border-color: var(--color-border-heavy);
  color: var(--color-text-muted);
  cursor: not-allowed;
  box-shadow: none;
}
</style>
