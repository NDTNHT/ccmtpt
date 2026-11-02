<template>
  <div class="task-ops-view">
    <div class="tactical-toolbar">
      <label><input type="checkbox" :checked="state.localMode" @change="setLocalMode($event.target.checked)" />
        Chế độ local + AI mock (lưu riêng trên trình duyệt)</label>
      <span>{{ state.localMode ? 'LOCAL: không gửi dữ liệu tới máy chủ' : 'SERVER: SQLite + cache localStorage' }}</span>
      <button v-if="!state.localMode" class="filter-chip" :disabled="state.loading" @click="fetchTasks">Tải lại từ máy chủ</button>
      <p v-if="state.error" role="alert">{{ state.error }}</p>
      <p v-if="state.storageError" role="alert">{{ state.storageError }}</p>
    </div>
    <!-- Telemetry Metric Row -->
    <div class="telemetry-stat-row">
      <div class="stat-cell">
        <span class="cell-label">TOTAL TASKS</span>
        <span class="cell-value">{{ taskStats.total }}</span>
        <span class="cell-sub">[ALL REGISTERED]</span>
      </div>
      <div class="stat-cell">
        <span class="cell-label">IN PROGRESS</span>
        <span class="cell-value text-amber">{{ taskStats.inProgress }}</span>
        <span class="cell-sub">[ACTIVE]</span>
      </div>
      <div class="stat-cell">
        <span class="cell-label">COMPLETED</span>
        <span class="cell-value text-green">{{ taskStats.completed }}</span>
        <span class="cell-sub">[RESOLVED]</span>
      </div>
      <div class="stat-cell">
        <span class="cell-label">COMPLETION RATE</span>
        <span class="cell-value">{{ taskStats.completionRate }}%</span>
        <span class="cell-sub">[OVERALL PROGRESS]</span>
      </div>
    </div>

    <!-- Main Workspace Split: Controls + Grid vs Real-time Telemetry Inspector -->
    <div class="workspace-grid">
      <div class="board-column">
        <!-- Action & Mission Dispatch Bar -->
        <div class="mission-action-bar">
          <div class="action-bar-label">
            <span class="ascii-badge text-alert">[TASK BOARD]</span>
            <span class="matrix-status">{{ filteredTasks.length }} TASKS</span>
          </div>
          <button class="primary-dispatch-btn" @click="isCreateModalOpen = true">
            <span class="dispatch-bracket">┌</span>
            <span class="dispatch-plus">+</span>
            <span class="dispatch-label">CREATE NEW TASK</span>
            <span class="dispatch-bracket">┘</span>
          </button>
        </div>

        <!-- Filter and Search Toolbar -->
        <div class="tactical-toolbar">
          <div class="search-box">
            <span class="search-prefix">&gt;&gt;</span>
            <input
              type="text"
              class="search-input"
              placeholder="SEARCH BY TITLE, ID, OR KEYWORD..."
              :value="state.searchQuery"
              @input="onSearchInput"
            />
          </div>

          <div class="filter-group">
            <span class="filter-label">STATUS:</span>
            <button
              v-for="st in statusOptions"
              :key="st"
              class="filter-chip"
              :class="{ active: state.statusFilter === st }"
              @click="setFilters({ status: st })"
            >
              {{ st }}
            </button>
          </div>

          <div class="filter-group">
            <span class="filter-label">PRIORITY:</span>
            <button
              v-for="pr in priorityOptions"
              :key="pr"
              class="filter-chip"
              :class="{ active: state.priorityFilter === pr }"
              @click="setFilters({ priority: pr })"
            >
              {{ pr }}
            </button>
          </div>
        </div>

        <div class="filter-group">
          <label for="deadline-filter">DEADLINE:</label>
          <select id="deadline-filter" class="tactical-select" :value="state.deadlineFilter" @change="setFilters({ deadline: $event.target.value })">
            <option value="ALL">Tất cả</option><option value="OVERDUE">Quá hạn</option>
            <option value="TODAY">Hôm nay</option><option value="WEEK">Trong 7 ngày</option><option value="NONE">Chưa đặt hạn</option>
          </select>
        </div>
        <!-- Task Creation Quick Bar / Drawer -->
        <div class="creation-banner">
          <div class="banner-header" @click="showCreateForm = !showCreateForm">
            <div class="header-left">
              <span class="ascii-badge text-alert">[+]</span>
              <span class="banner-title">QUICK ADD TASK</span>
            </div>
            <div class="header-right">
              <button
                type="button"
                class="open-modal-link-btn"
                @click.stop="isCreateModalOpen = true"
              >
                [EXPAND FULL FORM ↗]
              </button>
              <span class="toggle-icon">{{ showCreateForm ? '[-] COLLAPSE' : '[+] EXPAND' }}</span>
            </div>
          </div>

          <form v-if="showCreateForm" class="create-task-form" @submit.prevent="handleCreateTask">
            <div class="form-row">
              <input
                v-model="newTask.title"
                type="text"
                class="tactical-input title-input"
                placeholder="Task title (e.g. Set up payment gateway)..."
                required
              />
              <select v-model="newTask.priority" class="tactical-select">
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
              </select>
            </div>
            <div class="form-row">
              <textarea
                v-model="newTask.description"
                class="tactical-textarea"
                rows="2"
                placeholder="Task description and acceptance criteria..."
              ></textarea>
            </div>
            <div class="form-row form-submit-row">
              <div class="date-input-group">
                <span class="input-label">DUE DATE:</span>
                <input v-model="newTask.dueDate" type="date" class="tactical-date-input" />
              </div>
              <button type="submit" class="tactical-btn-submit" :disabled="!newTask.title.trim() || newTask.title.trim().length < 3 || newTask.title.trim().length > 100">
                [CREATE TASK]
              </button>
            </div>
          </form>
        </div>

        <div class="kanban-board" aria-label="Kanban board">
          <section v-for="column in kanbanColumns" :key="column.status" class="kanban-column">
            <h3>{{ column.title }} <span>[{{ column.tasks.length }}]</span></h3>
            <TaskCard v-for="task in column.tasks" :key="task.id" :task="task"
              :isSelected="state.selectedTask?.id === task.id"
              @select="onSelectTask" @hover="onHoverTask" @inspect="openModalForTask"
              @start="runTaskAction(startTask, $event)" @complete="runTaskAction(completeTask, $event)" @cancel="runTaskAction(cancelTask, $event)" />
            <p v-if="!column.tasks.length" class="empty-hint">Không có công việc phù hợp.</p>
          </section>
        </div>
        <section v-if="cancelledTasks.length" class="kanban-column">
          <h3>Đã hủy [{{ cancelledTasks.length }}]</h3>
          <TaskCard v-for="task in cancelledTasks" :key="task.id" :task="task"
            @select="onSelectTask" @hover="onHoverTask" @inspect="openModalForTask" />
        </section>
      </div>

      <!-- Real-Time Telemetry & Spec Inspector (Updated on @mouseover & @click) -->
      <aside class="telemetry-inspector">
        <div class="inspector-header">
          <span class="ascii-badge text-alert">[INSPECTOR]</span>
          <span class="inspector-title">TASK DETAILS</span>
        </div>

        <div class="inspector-content" v-if="activePreviewTask">
          <div class="preview-header">
            <span class="preview-id">{{ activePreviewTask.id }}</span>
            <span class="ascii-badge">{{ activePreviewTask.status }}</span>
          </div>
          <h4 class="preview-title">{{ activePreviewTask.title }}</h4>

          <div class="preview-meta-block">
            <div class="meta-line">
              <span class="lbl">PRIORITY:</span>
              <span class="val text-alert">{{ activePreviewTask.priority }}</span>
            </div>
            <div class="meta-line">
              <span class="lbl">DUE DATE:</span>
              <span class="val">{{ activePreviewTask.dueDate || 'UNASSIGNED' }}</span>
            </div>
            <div class="meta-line">
              <span class="lbl">PROJECT:</span>
              <span class="val text-dim">{{ activePreviewTask.projectId || 'DEFAULT' }}</span>
            </div>
          </div>

          <div class="spec-section-header">/// DESCRIPTION</div>
          <p class="preview-desc">
            {{ activePreviewTask.description || 'No description provided.' }}
          </p>

          <div class="spec-section-header">
            /// SUBTASKS ({{ activePreviewTask.subTasks?.length || 0 }})
          </div>
          <div class="preview-subtasks">
            <div
              v-for="st in activePreviewTask.subTasks || []"
              :key="st.id"
              class="preview-st-row"
              :class="{ done: st.completed }"
            >
              <span class="chk">{{ st.completed ? '[x]' : '[ ]' }}</span>
              <span class="txt">{{ st.title }}</span>
            </div>
            <div v-if="!activePreviewTask.subTasks?.length" class="no-st">
              [ NO SUBTASKS ]
            </div>
          </div>

          <div v-if="activePreviewTask.aiSummary" class="ai-summary-card">
            <div class="summary-lbl">[AI SUMMARY]</div>
            <p class="summary-txt">{{ activePreviewTask.aiSummary }}</p>
          </div>

          <div class="inspector-action-bar">
            <button class="tactical-btn-block" @click="openModalForTask(activePreviewTask)">
              [OPEN FULL DETAILS &gt;&gt;]
            </button>
          </div>
        </div>

        <div v-else class="empty-inspector">
          <span class="empty-prompt">Select or hover over a task to view details</span>
        </div>
      </aside>
    </div>

    <!-- Task Detail & Checklist Modal -->
    <TaskDetailModal
      :isOpen="isModalOpen"
      :task="state.selectedTask"
      @close="router.push('/tasks')"
      @toggle-subtask="(id, subId) => runTaskAction(() => toggleSubTask(id, subId))"
      @start-task="runTaskAction(startTask, $event)"
      @complete-task="runTaskAction(completeTask, $event)"
      @cancel-task="runTaskAction(cancelTask, $event)"
      @notify="$emit('notify', $event)"
    />

    <!-- Dedicated Create Task Modal -->
    <CreateTaskModal
      :isOpen="isCreateModalOpen"
      @close="isCreateModalOpen = false"
      @created="handleTaskCreated"
      @notify="(msg, type) => $emit('notify', msg, type)"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useTaskStore } from '../../store/useTaskStore.js';
import { useRoute, useRouter } from 'vue-router';
import TaskCard from './TaskCard.vue';
import TaskDetailModal from './TaskDetailModal.vue';
import CreateTaskModal from './CreateTaskModal.vue';

const emit = defineEmits(['notify']);
const route = useRoute();
const router = useRouter();

const {
  state,
  filteredTasks,
  kanbanColumns,
  setLocalMode,
  fetchTasks,
  taskStats,
  createTask,
  startTask,
  completeTask,
  cancelTask,
  selectTask,
  setHoveredTask,
  toggleSubTask,
  setFilters
} = useTaskStore();

const cancelledTasks = computed(() => filteredTasks.value.filter(t => t.status === 'CANCELLED'));
async function runTaskAction(action, id) {
  try { await action(id); } catch (err) { emit('notify', err.message, 'error'); }
}

const statusOptions = ['ALL', 'TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED'];
const priorityOptions = ['ALL', 'LOW', 'MEDIUM', 'HIGH'];

const showCreateForm = ref(false);
const isModalOpen = ref(false);
const isCreateModalOpen = ref(false);
watch(() => [route.params.taskId, state.tasks, state.loading], () => {
  const id = route.params.taskId;
  const task = state.tasks.find(t => t.id === id);
  if (task) { selectTask(task); isModalOpen.value = true; }
  else isModalOpen.value = false;
}, { immediate: true });

const newTask = ref({
  title: '',
  description: '',
  priority: 'HIGH',
  dueDate: new Date(Date.now() + 86400000 * 3).toISOString().slice(0, 10),
  projectId: null
});

const activePreviewTask = computed(() => {
  return state.hoveredTask || state.selectedTask || filteredTasks.value[0] || null;
});

function onSearchInput(e) {
  setFilters({ search: e.target.value });
}

function onSelectTask(task) {
  selectTask(task);
}

function onHoverTask(task) {
  setHoveredTask(task);
}

function openModalForTask(task) {
  selectTask(task);
  router.push({ name: 'task-detail', params: { taskId: task.id } });
}

function handleTaskCreated(task) {
  selectTask(task);
}

async function handleCreateTask() {
  const title = newTask.value.title.trim();
  if (title.length < 3 || title.length > 100) {
    emit('notify', 'Tiêu đề task phải từ 3 đến 100 ký tự.', 'error');
    return;
  }
  try {
    const created = await createTask({ ...newTask.value, title });
    emit('notify', `Task "${created.title}" created successfully.`, 'success');
    selectTask(created);
    newTask.value = {
      title: '',
      description: '',
      priority: 'HIGH',
      dueDate: new Date(Date.now() + 86400000 * 3).toISOString().slice(0, 10),
      projectId: null
    };
    showCreateForm.value = false;
  } catch (err) {
    emit('notify', `Failed to create task: ${err.message}`, 'error');
  }
}
</script>

<style scoped>
.kanban-board { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.kanban-column { min-width: 0; padding: 12px; background: var(--color-surface); border: 1px solid var(--color-border); display: flex; flex-direction: column; gap: 12px; }
.kanban-column h3 { font-size: .9rem; margin: 0; color: var(--color-neon-amber); }
.board-column { min-width: 0; }
@media (max-width: 1200px) { .workspace-grid { grid-template-columns: minmax(0, 1fr); } .telemetry-inspector { position: static; } }
@media (max-width: 760px) { .kanban-board { grid-template-columns: 1fr; } }

.task-ops-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
  font-family: var(--font-mono-code);
}

.telemetry-stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-cell {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cell-label {
  font-size: 0.72rem;
  color: var(--color-text-muted);
  letter-spacing: 0.1em;
  font-weight: 700;
}

.cell-value {
  font-family: var(--font-mono-header);
  font-size: 1.6rem;
  font-weight: 800;
  color: var(--color-text-main);
}

.cell-sub {
  font-size: 0.68rem;
  color: var(--color-text-dim);
}

.workspace-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 20px;
}

.board-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Mission Action & Dispatch Bar */
.mission-action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-left: 3px solid var(--color-neon-amber);
  padding: 10px 16px;
  gap: 12px;
  flex-wrap: wrap;
}

.action-bar-label {
  display: flex;
  align-items: center;
  gap: 10px;
}

.matrix-status {
  font-size: 0.72rem;
  color: var(--color-text-dim);
  letter-spacing: 0.05em;
}

.primary-dispatch-btn {
  background: var(--color-neon-amber);
  border: 1px solid var(--color-neon-amber);
  color: #000000;
  font-family: var(--font-mono-code);
  font-size: 0.8rem;
  font-weight: 800;
  letter-spacing: 0.05em;
  padding: 7px 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.15s ease;
  position: relative;
}

.primary-dispatch-btn:hover {
  background: #FFCA28;
  box-shadow: 0 0 12px rgba(255, 184, 0, 0.45);
  transform: translateY(-1px);
}

.primary-dispatch-btn:active {
  transform: translateY(0);
}

.dispatch-bracket {
  color: #222222;
  font-weight: bold;
}

.dispatch-plus {
  font-weight: 900;
}

.tactical-toolbar {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  padding: 6px 12px;
}

.search-prefix {
  color: var(--color-neon-amber);
  font-weight: 800;
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  color: var(--color-text-main);
  font-family: inherit;
  font-size: 0.78rem;
}

.search-input:focus {
  outline: none;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 0.7rem;
  color: var(--color-text-muted);
  font-weight: 700;
}

.filter-chip {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  color: var(--color-text-dim);
  padding: 3px 8px;
  font-family: inherit;
  font-size: 0.7rem;
  cursor: pointer;
  transition: all 0.15s ease;
}

.filter-chip:hover {
  border-color: var(--color-border-heavy);
  color: var(--color-text-main);
}

.filter-chip.active {
  background: var(--color-surface-hover);
  border-color: var(--color-neon-amber);
  color: var(--color-neon-amber);
  font-weight: 700;
}

.creation-banner {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
}

.banner-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: var(--color-surface-sunken);
  cursor: pointer;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.banner-title {
  font-family: var(--font-mono-header);
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.open-modal-link-btn {
  background: transparent;
  border: 1px solid var(--color-neon-amber);
  color: var(--color-neon-amber);
  font-family: inherit;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 3px 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.open-modal-link-btn:hover {
  background: var(--color-neon-amber);
  color: #000000;
}

.toggle-icon {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.create-task-form {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form-row {
  display: flex;
  gap: 10px;
}

.title-input {
  flex: 1;
}

.tactical-select {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  font-family: inherit;
  font-size: 0.78rem;
  padding: 6px 10px;
}

.tactical-textarea {
  width: 100%;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  font-family: inherit;
  font-size: 0.78rem;
  padding: 8px 12px;
  resize: vertical;
}

.tactical-textarea:focus {
  outline: none;
  border-color: var(--color-neon-amber);
}

.form-submit-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.date-input-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-label {
  font-size: 0.7rem;
  color: var(--color-text-muted);
}

.tactical-date-input {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  font-family: inherit;
  font-size: 0.75rem;
  padding: 4px 8px;
}

.tactical-btn-submit {
  background: var(--color-neon-amber) !important;
  color: #000000 !important;
  border: 1px solid var(--color-neon-amber) !important;
  padding: 8px 18px;
  font-family: inherit;
  font-size: 0.78rem;
  font-weight: 900;
  cursor: pointer;
  position: relative;
}

.tactical-btn-submit::before {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  width: 6px;
  height: 6px;
  border-top: 2px solid #FFFFFF;
  border-left: 2px solid #FFFFFF;
}

.tactical-btn-submit::after {
  content: '';
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 6px;
  height: 6px;
  border-bottom: 2px solid #FFFFFF;
  border-right: 2px solid #FFFFFF;
}

.tactical-btn-submit:hover:not(:disabled) {
  background: #ffc400 !important;
  box-shadow: 0 0 10px rgba(255, 184, 0, 0.4);
}

.tactical-btn-submit:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.tasks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.empty-state-box {
  background: var(--color-surface);
  border: 1px dashed var(--color-border-heavy);
  padding: 40px 20px;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.empty-ascii {
  color: var(--color-text-muted);
  font-weight: 700;
  font-size: 0.85rem;
}

.empty-hint {
  color: var(--color-text-dim);
  font-size: 0.75rem;
  margin: 0;
}

.telemetry-inspector {
  background: var(--color-surface);
  border: 1px solid var(--color-border-heavy);
  display: flex;
  flex-direction: column;
  height: fit-content;
  position: sticky;
  top: 16px;
}

.inspector-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: var(--color-surface-sunken);
  border-bottom: 1px solid var(--color-border-heavy);
}

.inspector-title {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.inspector-content {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.preview-id {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.preview-title {
  font-family: var(--font-mono-header);
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
}

.preview-meta-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  padding: 8px;
  font-size: 0.72rem;
}

.meta-line {
  display: flex;
  justify-content: space-between;
}

.meta-line .lbl {
  color: var(--color-text-muted);
}

.meta-line .val {
  font-weight: 700;
}

.spec-section-header {
  font-size: 0.7rem;
  color: var(--color-text-muted);
  letter-spacing: 0.08em;
  font-weight: 800;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 3px;
}

.preview-desc {
  font-size: 0.78rem;
  color: var(--color-text-dim);
  line-height: 1.4;
  margin: 0;
}

.preview-subtasks {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 0.72rem;
}

.preview-st-row {
  display: flex;
  gap: 6px;
  color: var(--color-text-dim);
}

.preview-st-row.done {
  color: var(--color-text-muted);
  text-decoration: line-through;
}

.no-st {
  font-style: italic;
  color: var(--color-text-muted);
}

.ai-summary-card {
  background: rgba(255, 179, 0, 0.06);
  border: 1px solid var(--color-neon-amber);
  padding: 8px;
  font-size: 0.72rem;
}

.summary-lbl {
  color: var(--color-neon-amber);
  font-weight: 800;
  margin-bottom: 4px;
}

.summary-txt {
  color: var(--color-text-main);
  margin: 0;
  line-height: 1.4;
}

.inspector-action-bar {
  margin-top: 8px;
}

.tactical-btn-block {
  width: 100%;
  background: #181818 !important;
  border: 1px solid #444444 !important;
  color: var(--color-neon-amber) !important;
  padding: 10px;
  font-family: inherit;
  font-size: 0.78rem;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.15s ease;
  position: relative;
}

.tactical-btn-block::before {
  content: '';
  position: absolute;
  top: -1px;
  left: -1px;
  width: 6px;
  height: 6px;
  border-top: 2px solid var(--color-neon-amber);
  border-left: 2px solid var(--color-neon-amber);
}

.tactical-btn-block::after {
  content: '';
  position: absolute;
  bottom: -1px;
  right: -1px;
  width: 6px;
  height: 6px;
  border-bottom: 2px solid var(--color-neon-amber);
  border-right: 2px solid var(--color-neon-amber);
}

.tactical-btn-block:hover {
  background: #252525 !important;
  border-color: var(--color-neon-amber) !important;
  box-shadow: 0 0 10px rgba(255, 184, 0, 0.3);
}

.empty-inspector {
  padding: 40px 16px;
  text-align: center;
  color: var(--color-text-muted);
  font-size: 0.72rem;
  line-height: 1.5;
}
@media (max-width: 1200px) { .workspace-grid { grid-template-columns: minmax(0, 1fr); } }
@media (max-width: 640px) {
  .telemetry-stat-row { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .stat-cell { min-width: 0; padding: 12px; }
  .banner-header, .header-right, .form-row { flex-wrap: wrap; gap: 8px; }
  .title-input { min-width: 0; }
}
</style>
