<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { useAiStore } from '../stores/ai';
import { useTaskStore } from '../stores/task';

const aiStore = useAiStore();
const taskStore = useTaskStore();

const { 
  isAssistantOpen, 
  activeTaskId, 
  provider, 
  customFormat, 
  customBaseUrl, 
  customApiKey, 
  customModel, 
  systemPrompt, 
  additionalInstructions,
  isGenerating,
  activeTab,
  breakdownResult,
  priorityResult,
  summaryResult,
  executionMessage,
  error 
} = storeToRefs(aiStore);

const { tasks } = storeToRefs(taskStore);

onMounted(async () => {
  if (tasks.value.length === 0) {
    await taskStore.fetchTasks();
  }
  if (!activeTaskId.value && tasks.value.length > 0) {
    activeTaskId.value = tasks.value[0].taskId;
  }
});

function handleRunCurrentTab() {
  if (!activeTaskId.value) return;
  if (activeTab.value === 'breakdown') {
    aiStore.generateBreakdown(activeTaskId.value, false);
  } else if (activeTab.value === 'prioritize') {
    aiStore.generatePriority(activeTaskId.value, false);
  } else if (activeTab.value === 'summary') {
    aiStore.generateSummary(activeTaskId.value);
  }
}

function handleApplyBreakdown() {
  if (!activeTaskId.value) return;
  aiStore.generateBreakdown(activeTaskId.value, true);
}

function handleApplyPriority() {
  if (!activeTaskId.value) return;
  aiStore.generatePriority(activeTaskId.value, true);
}
</script>

<template>
  <Teleport to="body">
    <div 
      v-if="isAssistantOpen"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md transition-all duration-300"
      @click.self="aiStore.closeAssistant()"
    >
      <!-- Double-Bezel Modal Window -->
      <div 
        class="w-full max-w-3xl bezel-outer bg-white/[0.02] border border-white/10 shadow-2xl overflow-hidden"
        role="dialog"
        aria-modal="true"
      >
        <div class="bezel-inner bg-[#0b0b0e] flex flex-col max-h-[85vh] overflow-hidden">
          
          <!-- Top Bar -->
          <div class="px-6 py-4 border-b border-white/10 flex items-center justify-between bg-black/40">
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
                </svg>
              </div>
              <div>
                <h3 class="text-sm font-bold text-white flex items-center gap-2 font-display">
                  Aether AI Copilot & Multi-Provider Agent
                  <span class="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-mono">
                    OpenCode Clean Arch
                  </span>
                </h3>
                <p class="text-[11px] text-zinc-400">
                  Phân rã task, đề xuất độ ưu tiên, tổng kết tiến độ với OpenAI & Anthropic protocol.
                </p>
              </div>
            </div>

            <button 
              @click="aiStore.closeAssistant()"
              class="w-8 h-8 rounded-full bg-white/5 border border-white/10 hover:bg-white/10 flex items-center justify-center text-zinc-400 hover:text-white transition-fluid"
            >
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- Task Selection & Feature Tabs -->
          <div class="px-6 py-3 border-b border-white/5 bg-white/[0.01] flex flex-wrap items-center justify-between gap-3">
            <!-- Target Task Selector -->
            <div class="flex items-center gap-2 flex-1 min-w-[240px]">
              <span class="text-xs text-zinc-400 font-mono whitespace-nowrap">Target Task:</span>
              <select 
                v-model="activeTaskId"
                class="flex-1 bg-black/60 border border-white/10 rounded-xl px-3 py-1.5 text-xs text-white focus:outline-none focus:border-emerald-500/50"
              >
                <option v-for="t in tasks" :key="t.taskId" :value="t.taskId">
                  {{ t.title }} ({{ t.priority }})
                </option>
              </select>
            </div>

            <!-- Tabs -->
            <div class="flex items-center gap-1 bg-black/40 border border-white/5 p-1 rounded-xl text-xs">
              <button 
                @click="activeTab = 'breakdown'"
                class="px-3 py-1 rounded-lg transition-fluid"
                :class="activeTab === 'breakdown' ? 'bg-emerald-500/20 text-emerald-300 font-semibold' : 'text-zinc-400 hover:text-white'"
              >
                Breakdown
              </button>
              <button 
                @click="activeTab = 'prioritize'"
                class="px-3 py-1 rounded-lg transition-fluid"
                :class="activeTab === 'prioritize' ? 'bg-emerald-500/20 text-emerald-300 font-semibold' : 'text-zinc-400 hover:text-white'"
              >
                Prioritize
              </button>
              <button 
                @click="activeTab = 'summary'"
                class="px-3 py-1 rounded-lg transition-fluid"
                :class="activeTab === 'summary' ? 'bg-emerald-500/20 text-emerald-300 font-semibold' : 'text-zinc-400 hover:text-white'"
              >
                Summary
              </button>
              <button 
                @click="activeTab = 'config'"
                class="px-3 py-1 rounded-lg transition-fluid flex items-center gap-1"
                :class="activeTab === 'config' ? 'bg-purple-500/20 text-purple-300 font-semibold' : 'text-zinc-400 hover:text-white'"
              >
                <span>Provider Config</span>
                <span class="w-1.5 h-1.5 rounded-full bg-purple-400"></span>
              </button>
            </div>
          </div>

          <!-- Tab Content Area -->
          <div class="flex-1 overflow-y-auto p-6 space-y-4">
            
            <!-- Execution Banner / Message -->
            <div 
              v-if="executionMessage"
              class="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs flex items-center justify-between"
            >
              <span>{{ executionMessage }}</span>
              <span class="text-[10px] font-mono text-emerald-500">HTTP 200 OK</span>
            </div>

            <!-- TAB 1: Task Breakdown -->
            <div v-if="activeTab === 'breakdown'" class="space-y-4">
              <div class="flex items-center justify-between">
                <div>
                  <h4 class="text-xs font-bold text-white uppercase tracking-wider font-mono">
                    AI Task Decomposition
                  </h4>
                  <p class="text-xs text-zinc-400">
                    Phân tích nhiệm vụ thành các bước thực thi nhỏ hơn kèm deadline ước tính.
                  </p>
                </div>
                <div class="flex items-center gap-2">
                  <button 
                    @click="handleRunCurrentTab"
                    :disabled="isGenerating"
                    class="px-4 py-2 rounded-full text-xs font-bold bg-emerald-500 text-black hover:bg-emerald-400 disabled:opacity-50 transition-fluid flex items-center gap-2"
                  >
                    <span v-if="isGenerating" class="w-3 h-3 border-2 border-black border-t-transparent rounded-full animate-spin"></span>
                    <span>{{ isGenerating ? 'Đang phân tích...' : 'Preview Breakdown' }}</span>
                  </button>
                  <button 
                    v-if="breakdownResult"
                    @click="handleApplyBreakdown"
                    :disabled="isGenerating"
                    class="px-3 py-2 rounded-full text-xs font-semibold bg-white/10 hover:bg-white/20 text-white transition-fluid"
                  >
                    Áp dụng vào Task
                  </button>
                </div>
              </div>

              <!-- Breakdown Results -->
              <div v-if="breakdownResult" class="space-y-3 pt-2">
                <div class="p-3 rounded-xl bg-black/40 border border-white/5 text-xs text-zinc-300">
                  <strong class="text-emerald-400">Reasoning:</strong> {{ breakdownResult.reasoning }}
                </div>

                <div class="space-y-2">
                  <div 
                    v-for="(sub, i) in breakdownResult.subtasks" 
                    :key="i"
                    class="p-3 rounded-xl bg-white/[0.02] border border-white/5 flex items-start justify-between gap-3"
                  >
                    <div class="flex items-start gap-2.5">
                      <span class="w-5 h-5 rounded-full bg-emerald-500/15 text-emerald-400 text-[10px] font-mono flex items-center justify-center mt-0.5">
                        {{ i + 1 }}
                      </span>
                      <div>
                        <div class="text-xs font-semibold text-white">{{ sub.title }}</div>
                        <div v-if="sub.rationale" class="text-[11px] text-zinc-400 mt-0.5">{{ sub.rationale }}</div>
                      </div>
                    </div>
                    <span v-if="sub.suggestedDeadline" class="text-[10px] font-mono text-zinc-500">
                      {{ new Date(sub.suggestedDeadline).toLocaleDateString() }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- TAB 2: Priority Proposal -->
            <div v-if="activeTab === 'prioritize'" class="space-y-4">
              <div class="flex items-center justify-between">
                <div>
                  <h4 class="text-xs font-bold text-white uppercase tracking-wider font-mono">
                    AI Priority Evaluation
                  </h4>
                  <p class="text-xs text-zinc-400">
                    Đánh giá độ khẩn cấp và đề xuất mức ưu tiên thích hợp dựa trên ngữ cảnh.
                  </p>
                </div>
                <div class="flex items-center gap-2">
                  <button 
                    @click="handleRunCurrentTab"
                    :disabled="isGenerating"
                    class="px-4 py-2 rounded-full text-xs font-bold bg-emerald-500 text-black hover:bg-emerald-400 disabled:opacity-50 transition-fluid flex items-center gap-2"
                  >
                    <span v-if="isGenerating" class="w-3 h-3 border-2 border-black border-t-transparent rounded-full animate-spin"></span>
                    <span>{{ isGenerating ? 'Đang tính toán...' : 'Đánh giá Priority' }}</span>
                  </button>
                  <button 
                    v-if="priorityResult"
                    @click="handleApplyPriority"
                    :disabled="isGenerating"
                    class="px-3 py-2 rounded-full text-xs font-semibold bg-white/10 hover:bg-white/20 text-white transition-fluid"
                  >
                    Cập nhật vào Task
                  </button>
                </div>
              </div>

              <!-- Priority Result -->
              <div v-if="priorityResult" class="p-5 rounded-2xl bg-black/40 border border-white/10 space-y-3">
                <div class="flex items-center gap-3">
                  <span class="text-xs text-zinc-400">Mức đề xuất:</span>
                  <span 
                    class="px-3 py-1 rounded-full text-xs font-bold font-mono"
                    :class="{
                      'bg-rose-500/20 text-rose-300 border border-rose-500/40': priorityResult.suggestedPriority === 'CRITICAL',
                      'bg-amber-500/20 text-amber-300 border border-amber-500/40': priorityResult.suggestedPriority === 'HIGH',
                      'bg-blue-500/20 text-blue-300 border border-blue-500/40': priorityResult.suggestedPriority === 'MEDIUM',
                      'bg-zinc-500/20 text-zinc-300 border border-zinc-500/40': priorityResult.suggestedPriority === 'LOW'
                    }"
                  >
                    {{ priorityResult.suggestedPriority }}
                  </span>
                  <span class="text-[11px] font-mono text-zinc-400">
                    Độ tin cậy: {{ Math.round((priorityResult.confidence || 0.9) * 100) }}%
                  </span>
                </div>
                <p class="text-xs text-zinc-300 leading-relaxed">
                  <strong class="text-white">Lý do:</strong> {{ priorityResult.reasoning }}
                </p>
              </div>
            </div>

            <!-- TAB 3: Summary -->
            <div v-if="activeTab === 'summary'" class="space-y-4">
              <div class="flex items-center justify-between">
                <div>
                  <h4 class="text-xs font-bold text-white uppercase tracking-wider font-mono">
                    AI Task Executive Summary
                  </h4>
                  <p class="text-xs text-zinc-400">
                    Tổng kết tiến độ, điểm nổi bật và bước hành động tiếp theo.
                  </p>
                </div>
                <button 
                  @click="handleRunCurrentTab"
                  :disabled="isGenerating"
                  class="px-4 py-2 rounded-full text-xs font-bold bg-emerald-500 text-black hover:bg-emerald-400 disabled:opacity-50 transition-fluid flex items-center gap-2"
                >
                  <span v-if="isGenerating" class="w-3 h-3 border-2 border-black border-t-transparent rounded-full animate-spin"></span>
                  <span>{{ isGenerating ? 'Đang tổng kết...' : 'Tạo Báo Cáo' }}</span>
                </button>
              </div>

              <!-- Summary Result -->
              <div v-if="summaryResult" class="p-5 rounded-2xl bg-black/40 border border-white/10 space-y-4">
                <div>
                  <h5 class="text-sm font-bold text-white font-display">{{ summaryResult.title }}</h5>
                  <p class="text-xs text-zinc-300 mt-1 leading-relaxed">{{ summaryResult.summary }}</p>
                </div>

                <div v-if="summaryResult.keyHighlights?.length > 0">
                  <span class="text-[11px] font-mono uppercase tracking-wider text-emerald-400 font-semibold">Key Highlights:</span>
                  <ul class="mt-2 space-y-1.5 text-xs text-zinc-300">
                    <li v-for="(h, idx) in summaryResult.keyHighlights" :key="idx" class="flex items-start gap-2">
                      <span class="text-emerald-500">•</span>
                      <span>{{ h }}</span>
                    </li>
                  </ul>
                </div>

                <div v-if="summaryResult.suggestedNextStep" class="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-xs">
                  <strong class="text-emerald-400">Bước tiếp theo đề xuất:</strong>
                  <span class="text-zinc-200 ml-1">{{ summaryResult.suggestedNextStep }}</span>
                </div>
              </div>
            </div>

            <!-- TAB 4: Provider & Custom LLM Protocol Config -->
            <div v-if="activeTab === 'config'" class="space-y-4">
              <div>
                <h4 class="text-xs font-bold text-white uppercase tracking-wider font-mono">
                  Custom AI Provider Integration (Opencode Wire Format)
                </h4>
                <p class="text-xs text-zinc-400">
                  Người dùng có thể tự tích hợp provider AI cá nhân theo chuẩn OpenAI hoặc Anthropic Claude.
                </p>
              </div>

              <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                <!-- Provider Protocol Selector -->
                <div>
                  <label class="block text-zinc-400 font-mono text-[11px] mb-1">Wire Format Protocol:</label>
                  <select 
                    v-model="customFormat"
                    class="w-full bg-black/50 border border-white/10 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-purple-500"
                  >
                    <option value="openai">OpenAI Compatible (v1/chat/completions)</option>
                    <option value="anthropic">Anthropic Claude (v1/messages)</option>
                  </select>
                </div>

                <!-- Custom Model Name -->
                <div>
                  <label class="block text-zinc-400 font-mono text-[11px] mb-1">Model Name:</label>
                  <input 
                    v-model="customModel"
                    type="text"
                    placeholder="gpt-4o, claude-3-5-sonnet-20241022, deepseek-chat..."
                    class="w-full bg-black/50 border border-white/10 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-purple-500"
                  />
                </div>

                <!-- Custom Base URL -->
                <div class="sm:col-span-2">
                  <label class="block text-zinc-400 font-mono text-[11px] mb-1">Base URL Endpoint:</label>
                  <input 
                    v-model="customBaseUrl"
                    type="text"
                    placeholder="https://api.openai.com/v1 hoặc https://api.anthropic.com/v1"
                    class="w-full bg-black/50 border border-white/10 rounded-xl px-3 py-2 text-white font-mono focus:outline-none focus:border-purple-500"
                  />
                </div>

                <!-- Custom API Key -->
                <div class="sm:col-span-2">
                  <label class="block text-zinc-400 font-mono text-[11px] mb-1">Custom API Key (Optional Override):</label>
                  <input 
                    v-model="customApiKey"
                    type="password"
                    placeholder="sk-... (để trống nếu dùng key mặc định từ backend)"
                    class="w-full bg-black/50 border border-white/10 rounded-xl px-3 py-2 text-white font-mono focus:outline-none focus:border-purple-500"
                  />
                </div>

                <!-- System Prompt Override -->
                <div class="sm:col-span-2">
                  <label class="block text-zinc-400 font-mono text-[11px] mb-1">Custom System Prompt Override:</label>
                  <textarea 
                    v-model="systemPrompt"
                    rows="2"
                    class="w-full bg-black/50 border border-white/10 rounded-xl px-3 py-2 text-white text-xs focus:outline-none focus:border-purple-500"
                  ></textarea>
                </div>
              </div>
            </div>

          </div>

        </div>
      </div>
    </div>
  </Teleport>
</template>
