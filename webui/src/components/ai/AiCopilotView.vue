<template>
  <div class="ai-copilot-view">
    <!-- Top Section: AI Engine Configuration & Custom Prompt Directive -->
    <AiPromptEditor @notify="$emit('notify', $event)" />

    <!-- Middle Section: Task Execution Workbench -->
    <div class="ai-workbench">
      <div class="workbench-header">
        <div class="header-tag">
          <span class="ascii-badge text-alert">[EXECUTION WORKBENCH]</span>
          <h3 class="workbench-title">MISSION-CRITICAL TASK ANALYSIS DISPATCH</h3>
        </div>
        <span class="workbench-status" v-if="aiState.executingAiOp">
          <span class="status-led amber"></span>
          [AI STREAM PROCESSING...]
        </span>
      </div>

      <div class="workbench-body">
        <!-- Target Task Selection Bar -->
        <div class="target-task-bar">
          <span class="target-lbl">SELECT TARGET MISSION:</span>
          <select v-model="selectedTaskId" class="target-task-select">
            <option v-for="t in taskState.tasks" :key="t.id" :value="t.id">
              [{{ t.id }}] {{ t.title }} ({{ t.status }} / {{ t.priority }})
            </option>
          </select>

          <button
            class="tactical-btn-run-all"
            :disabled="!selectedTask || aiState.executingAiOp"
            @click="handleRunFullAudit"
          >
            [EXECUTE FULL AI MISSION AUDIT]
          </button>
        </div>

        <!-- Targeted Quick Actions Grid -->
        <div class="quick-ops-grid" v-if="selectedTask">
          <button
            class="op-card-btn"
            :disabled="aiState.executingAiOp"
            @click="handleDecompose"
          >
            <span class="op-num">01//</span>
            <span class="op-name">DECOMPOSE CHECKLIST</span>
            <span class="op-desc">Bóc tách công việc thành các subtasks nhỏ</span>
          </button>

          <button
            class="op-card-btn"
            :disabled="aiState.executingAiOp"
            @click="handleEstimate"
          >
            <span class="op-num">02//</span>
            <span class="op-name">ESTIMATE DEADLINE</span>
            <span class="op-desc">Dự toán thời hạn hoàn thành tối ưu</span>
          </button>

          <button
            class="op-card-btn"
            :disabled="aiState.executingAiOp"
            @click="handlePriority"
          >
            <span class="op-num">03//</span>
            <span class="op-name">AUTO-PRIORITIZATION</span>
            <span class="op-desc">Đánh giá và đề xuất độ ưu tiên dựa trên rủi ro</span>
          </button>

          <button
            class="op-card-btn"
            :disabled="aiState.executingAiOp"
            @click="handleSummary"
          >
            <span class="op-num">04//</span>
            <span class="op-name">TASK SUMMARY</span>
            <span class="op-desc">Tạo tóm tắt ngắn gọn báo cáo tiến độ</span>
          </button>
        </div>

        <!-- Result Payload Display & Confirmation Box -->
        <div v-if="aiState.activeAiResult" class="ai-result-panel">
          <div class="result-header">
            <div class="result-tag">
              <span class="ascii-badge badge-green">[TELEMETRY PAYLOAD BOUND]</span>
              <span class="result-type">OPERATION: {{ aiState.activeAiResult.type }}</span>
            </div>
            <span class="result-timestamp">{{ aiState.activeAiResult.timestamp }}</span>
          </div>

          <div class="result-content">
            <!-- Decomposition Payload -->
            <div v-if="aiState.activeAiResult.type === 'DECOMPOSE' || aiState.activeAiResult.type === 'SUGGEST_SUBTASKS'" class="payload-block">
              <div class="payload-sub-title">GENERATED CHECKLIST SUBTASKS:</div>
              <ul class="payload-list">
                <li v-for="(item, idx) in aiState.activeAiResult.data.subTasks" :key="idx">
                  [ ] {{ item }}
                </li>
              </ul>
              <button class="tactical-btn-apply" @click="applyDecompositionResult">
                [COMMIT CHECKLIST TO TASK]
              </button>
            </div>

            <!-- Deadline Payload -->
            <div v-else-if="aiState.activeAiResult.type === 'ESTIMATE_DEADLINE'" class="payload-block">
              <div class="payload-metric-line">
                <span class="metric-lbl">ESTIMATED DEADLINE:</span>
                <span class="metric-val text-amber">{{ aiState.activeAiResult.data.recommendedDeadline }}</span>
              </div>
              <div class="payload-metric-line">
                <span class="metric-lbl">HOURS CALCULATION:</span>
                <span class="metric-val">{{ aiState.activeAiResult.data.estimatedHours }}h (Confidence: {{ Math.round(aiState.activeAiResult.data.confidenceScore * 100) }}%)</span>
              </div>
              <p class="payload-reasoning">{{ aiState.activeAiResult.data.reasoning }}</p>
              <button class="tactical-btn-apply" @click="applyDeadlineResult">
                [APPLY DEADLINE TO TASK]
              </button>
            </div>

            <!-- Priority Payload -->
            <div v-else-if="aiState.activeAiResult.type === 'AUTO_PRIORITY'" class="payload-block">
              <div class="payload-metric-line">
                <span class="metric-lbl">RECOMMENDED PRIORITY:</span>
                <span class="metric-val text-alert">[{{ aiState.activeAiResult.data.recommendedPriority }}]</span>
              </div>
              <div class="payload-metric-line">
                <span class="metric-lbl">URGENCY / IMPACT SCORES:</span>
                <span class="metric-val">{{ aiState.activeAiResult.data.urgencyScore }}/100 | {{ aiState.activeAiResult.data.impactScore }}/100</span>
              </div>
              <p class="payload-reasoning">{{ aiState.activeAiResult.data.reasoning }}</p>
              <button class="tactical-btn-apply" @click="applyPriorityResult">
                [UPDATE TASK PRIORITY LEVEL]
              </button>
            </div>

            <!-- Summary Payload -->
            <div v-else-if="aiState.activeAiResult.type === 'SUMMARY'" class="payload-block">
              <div class="payload-sub-title">EXECUTIVE BRIEF SUMMARY:</div>
              <p class="summary-paragraph">{{ aiState.activeAiResult.data.summary }}</p>
              <button class="tactical-btn-apply" @click="applySummaryResult">
                [SAVE SUMMARY TO TASK DOSSIER]
              </button>
            </div>

            <!-- Full Analysis Payload -->
            <div v-else-if="aiState.activeAiResult.type === 'FULL_ANALYSIS'" class="payload-block">
              <div class="full-analysis-summary">
                <p><strong>SUMMARY:</strong> {{ aiState.activeAiResult.data.summary?.summary }}</p>
                <p><strong>RECOMMENDED DEADLINE:</strong> {{ aiState.activeAiResult.data.deadlineEstimation?.recommendedDeadline }}</p>
                <p><strong>RECOMMENDED PRIORITY:</strong> [{{ aiState.activeAiResult.data.priorityRecommendation?.recommendedPriority }}]</p>
                <div class="payload-sub-title">GENERATED CHECKLIST:</div>
                <ul class="payload-list">
                  <li v-for="(item, idx) in aiState.activeAiResult.data.decomposition?.subTasks || []" :key="idx">
                    [ ] {{ item }}
                  </li>
                </ul>
              </div>
              <button class="tactical-btn-apply" @click="applyFullAuditResult">
                [COMMIT ALL AUDIT RECOMMENDATIONS TO TASK]
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Bottom Section: Telemetry Log Terminal -->
    <AiTelemetryLog />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useAiStore } from '../../store/useAiStore.js';
import { useTaskStore } from '../../store/useTaskStore.js';
import AiPromptEditor from './AiPromptEditor.vue';
import AiTelemetryLog from './AiTelemetryLog.vue';

const emit = defineEmits(['notify']);

const {
  state: aiState,
  decomposeTask,
  estimateDeadline,
  analyzePriority,
  generateSummary,
  runFullAnalysis
} = useAiStore();

const {
  state: taskState,
  addSubTasks,
  updateTaskInState
} = useTaskStore();

const selectedTaskId = ref(taskState.tasks[0]?.id || '');

const selectedTask = computed(() => {
  return taskState.tasks.find((t) => t.id === selectedTaskId.value) || taskState.tasks[0];
});

async function handleDecompose() {
  if (!selectedTask.value) return;
  try {
    await decomposeTask(selectedTask.value.id, selectedTask.value.title);
    emit('notify', 'Checklist items generated.', 'success');
  } catch (err) {
    emit('notify', err.message, 'error');
  }
}

async function handleEstimate() {
  if (!selectedTask.value) return;
  try {
    await estimateDeadline(selectedTask.value.id);
    emit('notify', 'Deadline calculated.', 'success');
  } catch (err) {
    emit('notify', err.message, 'error');
  }
}

async function handlePriority() {
  if (!selectedTask.value) return;
  try {
    await analyzePriority(selectedTask.value.id);
    emit('notify', 'Priority evaluation completed.', 'success');
  } catch (err) {
    emit('notify', err.message, 'error');
  }
}

async function handleSummary() {
  if (!selectedTask.value) return;
  try {
    await generateSummary(selectedTask.value.id);
    emit('notify', 'Summary synthesized.', 'success');
  } catch (err) {
    emit('notify', err.message, 'error');
  }
}

async function handleRunFullAudit() {
  if (!selectedTask.value) return;
  try {
    await runFullAnalysis(selectedTask.value.id, selectedTask.value.title);
    emit('notify', 'Full AI audit pipeline executed.', 'success');
  } catch (err) {
    emit('notify', err.message, 'error');
  }
}

function applyDecompositionResult() {
  const res = aiState.activeAiResult;
  if (res?.data?.subTasks && selectedTask.value) {
    addSubTasks(selectedTask.value.id, res.data.subTasks);
    emit('notify', 'Subtasks applied to task checklist.', 'success');
  }
}

function applyDeadlineResult() {
  const res = aiState.activeAiResult;
  if (res?.data?.recommendedDeadline && selectedTask.value) {
    updateTaskInState(selectedTask.value.id, { dueDate: res.data.recommendedDeadline });
    emit('notify', `Task deadline updated to ${res.data.recommendedDeadline}.`, 'success');
  }
}

function applyPriorityResult() {
  const res = aiState.activeAiResult;
  if (res?.data?.recommendedPriority && selectedTask.value) {
    updateTaskInState(selectedTask.value.id, { priority: res.data.recommendedPriority });
    emit('notify', `Task priority shifted to [${res.data.recommendedPriority}].`, 'success');
  }
}

function applySummaryResult() {
  const res = aiState.activeAiResult;
  if (res?.data?.summary && selectedTask.value) {
    updateTaskInState(selectedTask.value.id, { aiSummary: res.data.summary });
    emit('notify', 'AI summary attached to task.', 'success');
  }
}

function applyFullAuditResult() {
  const res = aiState.activeAiResult;
  if (!res?.data || !selectedTask.value) return;

  const updates = {};
  if (res.data.deadlineEstimation?.recommendedDeadline) {
    updates.dueDate = res.data.deadlineEstimation.recommendedDeadline;
  }
  if (res.data.priorityRecommendation?.recommendedPriority) {
    updates.priority = res.data.priorityRecommendation.recommendedPriority;
  }
  if (res.data.summary?.summary) {
    updates.aiSummary = res.data.summary.summary;
  }
  updateTaskInState(selectedTask.value.id, updates);

  if (res.data.decomposition?.subTasks) {
    addSubTasks(selectedTask.value.id, res.data.decomposition.subTasks);
  }
  emit('notify', 'All audit checkpoints applied to mission package.', 'success');
}
</script>

<style scoped>
.ai-copilot-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
  font-family: var(--font-mono-code);
}

.ai-workbench {
  background: var(--color-surface);
  border: 1px solid var(--color-border-heavy);
  display: flex;
  flex-direction: column;
}

.workbench-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 18px;
  background: var(--color-surface-sunken);
  border-bottom: 1px solid var(--color-border-heavy);
}

.header-tag {
  display: flex;
  align-items: center;
  gap: 10px;
}

.workbench-title {
  font-family: var(--font-mono-header);
  font-size: 0.88rem;
  font-weight: 800;
  color: var(--color-text-main);
  margin: 0;
}

.workbench-status {
  font-size: 0.72rem;
  color: var(--color-neon-amber);
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 700;
}

.workbench-body {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.target-task-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  padding: 10px 14px;
}

.target-lbl {
  font-size: 0.72rem;
  color: var(--color-text-muted);
  font-weight: 800;
}

.target-task-select {
  flex: 1;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  padding: 6px 10px;
  font-family: inherit;
  font-size: 0.75rem;
}

.tactical-btn-run-all {
  background: var(--color-neon-amber);
  color: #000;
  border: none;
  padding: 8px 16px;
  font-family: inherit;
  font-size: 0.75rem;
  font-weight: 900;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tactical-btn-run-all:hover:not(:disabled) {
  background: #ffc400;
}

.tactical-btn-run-all:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.quick-ops-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.op-card-btn {
  background: var(--color-surface-sunken);
  border: 1px solid var(--color-border);
  padding: 14px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition: all 0.15s ease;
}

.op-card-btn:hover:not(:disabled) {
  background: var(--color-surface-hover);
  border-color: var(--color-neon-amber);
}

.op-card-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.op-num {
  font-size: 0.68rem;
  color: var(--color-text-muted);
}

.op-name {
  font-family: var(--font-mono-header);
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--color-neon-amber);
}

.op-desc {
  font-size: 0.7rem;
  color: var(--color-text-dim);
  line-height: 1.3;
}

.ai-result-panel {
  background: var(--color-bg);
  border: 1px solid var(--color-neon-green);
  display: flex;
  flex-direction: column;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: rgba(74, 246, 38, 0.05);
  border-bottom: 1px solid var(--color-border);
}

.result-tag {
  display: flex;
  align-items: center;
  gap: 10px;
}

.badge-green {
  border-color: var(--color-neon-green);
  color: var(--color-neon-green);
}

.result-type {
  font-size: 0.75rem;
  font-weight: 800;
  color: var(--color-text-main);
}

.result-timestamp {
  font-size: 0.7rem;
  color: var(--color-text-muted);
}

.result-content {
  padding: 16px;
}

.payload-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.payload-sub-title {
  font-size: 0.72rem;
  color: var(--color-text-muted);
  font-weight: 800;
}

.payload-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 0.78rem;
  color: var(--color-text-main);
}

.payload-metric-line {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.82rem;
}

.payload-metric-line .metric-lbl {
  color: var(--color-text-muted);
}

.payload-metric-line .metric-val {
  font-weight: 800;
}

.payload-reasoning,
.summary-paragraph {
  font-size: 0.78rem;
  color: var(--color-text-dim);
  line-height: 1.4;
  margin: 0;
}

.full-analysis-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 0.78rem;
  line-height: 1.4;
}

.tactical-btn-apply {
  align-self: flex-start;
  background: var(--color-surface);
  border: 1px solid var(--color-neon-green);
  color: var(--color-neon-green);
  padding: 6px 14px;
  font-family: inherit;
  font-size: 0.72rem;
  font-weight: 800;
  cursor: pointer;
  margin-top: 6px;
}

.tactical-btn-apply:hover {
  background: var(--color-neon-green);
  color: #000;
}
</style>
