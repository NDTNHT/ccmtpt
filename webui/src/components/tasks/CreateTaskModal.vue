<template>
  <div class="tactical-modal-backdrop" v-if="isOpen" @click.self="handleClose">
    <div class="tactical-modal-window">
      <!-- Modal Header -->
      <div class="modal-header">
        <div class="header-tag">
          <span class="ascii-badge text-alert">[MISSION DISPATCH PROTOCOL // 01]</span>
          <span class="modal-title">AUTHORIZE NEW TACTICAL TASK BRIEF</span>
        </div>
        <button class="modal-close-btn" @click="handleClose">[ABORT / ESC]</button>
      </div>

      <!-- Modal Body / Form -->
      <form class="modal-body" @submit.prevent="handleSubmit">
        <!-- Section: Title -->
        <div class="form-section">
          <div class="section-label-row">
            <label class="form-label">
              <span class="label-prefix">&gt;&gt;</span>
              MISSION IDENTIFIER / TITLE
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
            placeholder="E.G. DEPLOY PROMETHEUS TELEMETRY RELAY..."
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
              PRIORITY RATING
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

          <!-- Project Sector Selector -->
          <div class="form-section project-section">
            <div class="section-label-row">
              <label class="form-label">
                <span class="label-prefix">&gt;&gt;</span>
                TARGET PROJECT / SECTOR
              </label>
              <button
                type="button"
                class="new-project-toggle-btn"
                @click="showNewProjectForm = !showNewProjectForm"
              >
                {{ showNewProjectForm ? '[-] CANCEL' : '[+ NEW REGULAR SECTOR]' }}
              </button>
            </div>

            <!-- Inline Regular Project Creation Box -->
            <div v-if="showNewProjectForm" class="new-project-inline-box">
              <div class="new-project-row">
                <input
                  v-model="newProjectName"
                  type="text"
                  class="tactical-input new-project-input"
                  placeholder="NEW REGULAR PROJECT (E.G. OMEGA CLUSTER)..."
                  maxlength="100"
                  @keydown.enter.prevent="handleCreateRegularProject"
                />
                <button
                  type="button"
                  class="tactical-btn-sm btn-create-proj"
                  :disabled="!newProjectName.trim() || newProjectLoading"
                  @click="handleCreateRegularProject"
                >
                  {{ newProjectLoading ? '[...]' : '[+ CREATE REGULAR]' }}
                </button>
              </div>
              <span class="new-project-note">
                [DOMAIN AGGREGATE // TYPE = REGULAR]
              </span>
            </div>

            <select v-model="form.projectId" class="tactical-select project-select">
              <optgroup label="── REGULAR MISSION SECTORS (STRUCTURED) ──">
                <option
                  v-for="project in regularProjects"
                  :key="project.id"
                  :value="project.id"
                >
                  [{{ project.name }}] // REGULAR PROJECT
                </option>
                <option v-if="regularProjects.length === 0" disabled value="">
                  (NO REGULAR PROJECTS RECORDED — CLICK [+ NEW REGULAR SECTOR])
                </option>
              </optgroup>
              <optgroup label="── INBOX QUEUE (TRIAGE) ──">
                <option :value="inboxProject?.id || null">
                  [{{ inboxProject ? inboxProject.name : 'INBOX' }}] // DEFAULT TRIAGE QUEUE
                </option>
              </optgroup>
            </select>

            <!-- Project Type Indicator Badge -->
            <div class="project-type-indicator">
              <span
                class="type-badge"
                :class="selectedProjectType === 'REGULAR' ? 'badge-regular' : 'badge-inbox'"
              >
                SECTOR TYPE: [{{ selectedProjectType }}]
              </span>
              <span class="type-hint" v-if="selectedProjectType === 'REGULAR'">
                ✓ Classified under structured domain project (Regular)
              </span>
              <span class="type-hint" v-else>
                ○ Default inbox for unclassified missions
              </span>
            </div>
          </div>
        </div>

        <!-- Section: Deadline & Quick Presets -->
        <div class="form-section">
          <div class="section-label-row">
            <label class="form-label">
              <span class="label-prefix">&gt;&gt;</span>
              TARGET DEADLINE
            </label>
            <span class="sub-label">[PROJECTED COMPLETION WINDOW]</span>
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

        <!-- Section: Operational Description -->
        <div class="form-section">
          <div class="section-label-row">
            <label class="form-label">
              <span class="label-prefix">&gt;&gt;</span>
              MISSION SPECIFICATIONS &amp; PARAMETERS
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
            placeholder="DOCUMENT MISSION SPECIFICATIONS, OPERATIONAL CONSTRAINTS, HARDWARE TARGETS, AND ACCEPTANCE CRITERIA..."
          ></textarea>
        </div>

        <!-- Section: Pre-dispatch Checklist Builder -->
        <div class="form-section checklist-builder-section">
          <div class="section-label-row">
            <label class="form-label">
              <span class="label-prefix">&gt;&gt;</span>
              INITIAL MISSION CHECKPOINTS ({{ form.subTasks.length }})
            </label>
            <button
              type="button"
              class="ai-suggest-btn"
              :disabled="!form.title.trim()"
              @click="generateSmartCheckpoints"
            >
              [🤖 AI DRAFT CHECKPOINTS]
            </button>
          </div>

          <div class="subtask-add-row">
            <input
              v-model="newSubTaskTitle"
              type="text"
              class="tactical-input subtask-input"
              placeholder="ENTER CHECKPOINT (E.G. CONDUCT TLS BENCHMARK TEST)..."
              @keydown.enter.prevent="handleAddSubTask"
            />
            <button
              type="button"
              class="tactical-btn-sm"
              :disabled="!newSubTaskTitle.trim()"
              @click="handleAddSubTask"
            >
              [+ ADD CHECKPOINT]
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
                title="Remove checkpoint"
                @click="removeSubTask(index)"
              >
                [REMOVE / X]
              </button>
            </div>
          </div>
          <div v-else class="empty-subtasks-note">
            [ NO INITIAL CHECKPOINTS ADDED — OPTIONALLY ADD ABOVE OR DECOMPOSE WITH AI AFTER DISPATCH ]
          </div>
        </div>

        <!-- Modal Footer Actions -->
        <div class="modal-footer">
          <div class="footer-telemetry">
            <span class="status-indicator"></span>
            <span class="telemetry-text">
              STATUS: {{ isSubmitting ? 'TRANSMITTING BRIEF TO CORE...' : 'READY FOR DISPATCH' }}
            </span>
          </div>

          <div class="footer-buttons">
            <button
              type="button"
              class="tactical-btn-cancel"
              :disabled="isSubmitting"
              @click="handleClose"
            >
              [CANCEL / ABORT]
            </button>
            <button
              type="submit"
              class="tactical-btn-dispatch"
              :disabled="isSubmitting || !isFormValid"
            >
              <span v-if="isSubmitting">[TRANSMITTING...]</span>
              <span v-else>┌ [AUTHORIZE &amp; DISPATCH MISSION] ┘</span>
            </button>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue';
import { useTaskStore } from '../../store/useTaskStore.js';
import { apiService } from '../../services/apiService.js';

const props = defineProps({
  isOpen: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['close', 'created', 'notify']);

const { createTask } = useTaskStore();

const titleInputRef = ref(null);
const isSubmitting = ref(false);
const newSubTaskTitle = ref('');
const projectList = ref([]);
const showNewProjectForm = ref(false);
const newProjectName = ref('');
const newProjectLoading = ref(false);

const inboxProject = computed(() => {
  return projectList.value.find(p => p.type === 'INBOX') || null;
});

const regularProjects = computed(() => {
  return projectList.value.filter(p => p.type === 'REGULAR');
});

const selectedProject = computed(() => {
  return projectList.value.find(p => p.id === form.projectId) || null;
});

const selectedProjectType = computed(() => {
  return selectedProject.value ? selectedProject.value.type : 'INBOX';
});

const priorityOptions = [
  { value: 'LOW', label: 'LOW' },
  { value: 'MEDIUM', label: 'MEDIUM' },
  { value: 'HIGH', label: 'HIGH' },
  { value: 'CRITICAL', label: 'CRITICAL' }
];

const form = reactive({
  title: '',
  description: '',
  priority: 'HIGH',
  dueDate: getFormattedDate(3),
  projectId: null,
  subTasks: []
});

function getFormattedDate(daysOffset = 0) {
  const target = new Date(Date.now() + daysOffset * 86400000);
  return target.toISOString().slice(0, 10);
}

function applyDatePreset(days) {
  form.dueDate = getFormattedDate(days);
}

const isFormValid = computed(() => {
  return form.title.trim().length >= 3 && form.title.trim().length <= 100;
});

const titleError = computed(() => {
  const len = form.title.trim().length;
  if (len === 0) return null;
  if (len < 3) return 'Title must contain at least 3 characters.';
  if (len > 100) return 'Title cannot exceed 100 characters.';
  return null;
});

async function loadProjects() {
  try {
    const data = await apiService.getProjects();
    if (Array.isArray(data)) {
      projectList.value = data;
      // Default to the first REGULAR project if available, or first available project
      if (!form.projectId || !data.some(p => p.id === form.projectId)) {
        const firstRegular = data.find(p => p.type === 'REGULAR');
        form.projectId = firstRegular ? firstRegular.id : (data[0] ? data[0].id : null);
      }
    }
  } catch (err) {
    console.warn('Projects endpoint unreachable, using default inbox:', err.message);
  }
}

async function handleCreateRegularProject() {
  const name = newProjectName.value.trim();
  if (!name) return;
  newProjectLoading.value = true;
  try {
    const created = await apiService.createProject({
      name: name.toUpperCase(),
      description: `Tactical regular mission sector`
    });
    await loadProjects();
    form.projectId = created.id;
    showNewProjectForm.value = false;
    newProjectName.value = '';
    emit('notify', `Regular Sector "${created.name}" registered and assigned.`, 'success');
  } catch (err) {
    emit('notify', `Failed to create regular project: ${err.message}`, 'error');
  } finally {
    newProjectLoading.value = false;
  }
}

onMounted(() => {
  loadProjects();
});

watch(() => props.isOpen, (open) => {
  if (open) {
    loadProjects();
    nextTick(() => {
      if (titleInputRef.value) {
        titleInputRef.value.focus();
      }
    });
  }
});

function handleAddSubTask() {
  const title = newSubTaskTitle.value.trim();
  if (!title) return;
  form.subTasks.push({
    title,
    completed: false
  });
  newSubTaskTitle.value = '';
}

function removeSubTask(index) {
  form.subTasks.splice(index, 1);
}

function generateSmartCheckpoints() {
  const title = form.title.trim().toUpperCase();
  if (!title) return;

  const suggestions = [];
  if (title.includes('AUTH') || title.includes('KEY') || title.includes('SECURITY')) {
    suggestions.push(
      'Audit cryptographic cipher suite and handshake protocols',
      'Validate TLS 1.3 certificate expiration thresholds',
      'Purge sandbox telemetry tokens and rotate keys'
    );
  } else if (title.includes('TEST') || title.includes('VERIFY') || title.includes('PARITY')) {
    suggestions.push(
      'Execute automated unit & integration test suites',
      'Analyze code coverage & regression delta',
      'Capture and record baseline telemetry logs'
    );
  } else if (title.includes('DEPLOY') || title.includes('RELEASE') || title.includes('SERVER')) {
    suggestions.push(
      'Verify container health check and port binding',
      'Run smoke tests against target ingress controller',
      'Monitor cluster CPU & memory metrics for 15 minutes'
    );
  } else if (title.includes('DATABASE') || title.includes('MIGRATE') || title.includes('DATA')) {
    suggestions.push(
      'Execute schema migration script in dry-run mode',
      'Validate foreign key constraints and index integrity',
      'Verify data backup snapshot before cutover'
    );
  } else {
    suggestions.push(
      `Initialize operational workspace for ${form.title.trim()}`,
      'Perform subsystem parity verification & parameter validation',
      'Run end-to-end integration test and finalize dispatch log'
    );
  }

  suggestions.forEach((st) => {
    if (!form.subTasks.some((existing) => existing.title === st)) {
      form.subTasks.push({ title: st, completed: false });
    }
  });

  emit('notify', `Generated ${suggestions.length} tactical checkpoints from mission brief.`, 'info');
}

function resetForm() {
  form.title = '';
  form.description = '';
  form.priority = 'HIGH';
  form.dueDate = getFormattedDate(3);
  form.subTasks = [];
  newSubTaskTitle.value = '';
}

function handleClose() {
  if (isSubmitting.value) return;
  emit('close');
}

async function handleSubmit() {
  if (!isFormValid.value || isSubmitting.value) return;

  isSubmitting.value = true;
  try {
    const payload = {
      title: form.title.trim().toUpperCase(),
      description: form.description.trim(),
      priority: form.priority,
      dueDate: form.dueDate,
      projectId: form.projectId,
      subTasks: form.subTasks
    };

    const created = await createTask(payload);
    emit('notify', `Mission "${created.title}" successfully dispatched to core telemetry.`, 'success');
    emit('created', created);
    resetForm();
    emit('close');
  } catch (err) {
    console.error('Failed to dispatch mission:', err);
    emit('notify', `Dispatch failed: ${err.message || 'Unknown network error'}`, 'error');
  } finally {
    isSubmitting.value = false;
  }
}
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
  background: #181818;
  border-bottom: 2px solid #333333;
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
  color: #FFFFFF;
}

.modal-close-btn {
  background: transparent;
  border: 1px solid #444444;
  color: #888888;
  font-family: var(--font-mono-code);
  font-size: 0.75rem;
  padding: 4px 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.modal-close-btn:hover {
  background: #FF2A2A;
  color: #000000;
  border-color: #FF2A2A;
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
  color: #CCCCCC;
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
  color: #666666;
}

.char-count {
  font-size: 0.7rem;
  color: #777777;
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
  background: #080808;
  border: 1px solid #333333;
  color: #EEEEEE;
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
  box-shadow: 0 0 8px rgba(255, 184, 0, 0.2);
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
  color: #000000;
}

.new-project-inline-box {
  background: #141414;
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
  color: #000000;
  border-color: var(--color-neon-amber);
  font-weight: 800;
  white-space: nowrap;
}

.btn-create-proj:hover:not(:disabled) {
  background: #FFCA28;
  color: #000000;
}

.new-project-note {
  font-size: 0.65rem;
  color: #888888;
  letter-spacing: 0.05em;
}

.project-select optgroup {
  background: #181818;
  color: var(--color-neon-amber);
  font-weight: 700;
  font-size: 0.75rem;
  padding: 4px;
}

.project-select option {
  background: #0D0D0D;
  color: #EEEEEE;
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
  background: rgba(0, 229, 255, 0.15);
  border: 1px solid #00E5FF;
  color: #00E5FF;
}

.badge-inbox {
  background: rgba(255, 184, 0, 0.12);
  border: 1px solid var(--color-neon-amber);
  color: var(--color-neon-amber);
}

.type-hint {
  color: #888888;
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
  background: #0D0D0D;
  border: 1px solid #333333;
  color: #888888;
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
  background: #444444;
  display: inline-block;
}

.priority-chip.active {
  color: #FFFFFF;
}

.priority-chip.low.active {
  border-color: #666666;
  background: #1E1E1E;
}
.priority-chip.low.active .chip-led {
  background: #AAAAAA;
}

.priority-chip.medium.active {
  border-color: #00E5FF;
  background: rgba(0, 229, 255, 0.12);
  color: #00E5FF;
}
.priority-chip.medium.active .chip-led {
  background: #00E5FF;
}

.priority-chip.high.active {
  border-color: var(--color-neon-amber);
  background: rgba(255, 184, 0, 0.12);
  color: var(--color-neon-amber);
}
.priority-chip.high.active .chip-led {
  background: var(--color-neon-amber);
}

.priority-chip.critical.active {
  border-color: var(--color-alert-red);
  background: rgba(255, 42, 42, 0.15);
  color: var(--color-alert-red);
  animation: pulse-border 1.5s infinite;
}
.priority-chip.critical.active .chip-led {
  background: var(--color-alert-red);
}

@keyframes pulse-border {
  0%, 100% { border-color: var(--color-alert-red); }
  50% { border-color: #FF8888; }
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
  background: #181818;
  border: 1px solid #333333;
  color: #AAAAAA;
  font-family: var(--font-mono-code);
  font-size: 0.7rem;
  padding: 6px 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.preset-btn:hover {
  background: #252525;
  color: #FFFFFF;
  border-color: var(--color-neon-amber);
}

/* Checklist Builder */
.checklist-builder-section {
  background: #090909;
  border: 1px solid #222222;
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
  color: #000000;
}

.ai-suggest-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  border-color: #444444;
  color: #666666;
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
  background: #1C1C1C;
  border: 1px solid #444444;
  color: #EEEEEE;
  font-family: var(--font-mono-code);
  font-size: 0.75rem;
  font-weight: bold;
  padding: 6px 12px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}

.tactical-btn-sm:hover:not(:disabled) {
  background: #2A2A2A;
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
  background: #121212;
  border: 1px solid #262626;
  padding: 6px 10px;
  font-size: 0.8rem;
}

.st-index {
  color: var(--color-neon-amber);
  font-size: 0.75rem;
}

.st-bracket {
  color: #666666;
}

.st-title {
  flex: 1;
  color: #DDDDDD;
  word-break: break-word;
}

.st-remove-btn {
  background: transparent;
  border: 1px solid transparent;
  color: #777777;
  font-family: var(--font-mono-code);
  font-size: 0.7rem;
  padding: 2px 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.st-remove-btn:hover {
  color: var(--color-alert-red);
  border-color: var(--color-alert-red);
  background: rgba(255, 42, 42, 0.1);
}

.empty-subtasks-note {
  font-size: 0.7rem;
  color: #555555;
  margin-top: 8px;
  text-align: center;
  padding: 8px;
  border: 1px dashed #222222;
}

/* Modal Footer */
.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: #141414;
  border-top: 2px solid #333333;
  gap: 12px;
  flex-wrap: wrap;
}

.footer-telemetry {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.75rem;
  color: #888888;
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
  border: 1px solid #444444;
  color: #AAAAAA;
  font-family: var(--font-mono-code);
  font-size: 0.8rem;
  padding: 8px 16px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tactical-btn-cancel:hover:not(:disabled) {
  background: #222222;
  color: #FFFFFF;
  border-color: #666666;
}

.tactical-btn-dispatch {
  background: var(--color-neon-amber);
  border: 1px solid var(--color-neon-amber);
  color: #000000;
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
  background: #FFCA28;
  box-shadow: 0 0 14px rgba(255, 184, 0, 0.4);
}

.tactical-btn-dispatch:disabled {
  background: #333333;
  border-color: #444444;
  color: #666666;
  cursor: not-allowed;
  box-shadow: none;
}
</style>
