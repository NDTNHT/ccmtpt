<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { useTaskStore } from '../stores/task';
import { useAiStore } from '../stores/ai';
import type { Priority, TaskStatus, TaskInputDTO } from '../types/task';

const taskStore = useTaskStore();
const aiStore = useAiStore();

const { tasks, isLoading, error } = storeToRefs(taskStore);

// Filters
const statusFilter = ref<string>('ALL');
const priorityFilter = ref<string>('ALL');
const searchQuery = ref<string>('');

// New Task Form Modal State
const isCreateModalOpen = ref<boolean>(false);
const newTitle = ref<string>('');
const newDescription = ref<string>('');
const newPriority = ref<Priority>('MEDIUM');
const newDueDate = ref<string>(new Date(Date.now() + 86400000 * 3).toISOString().slice(0, 10));

onMounted(async () => {
  await taskStore.fetchTasks();
});

// COMPUTED: Metrics
const totalTasks = computed(() => tasks.value.length);
const todoTasks = computed(() => tasks.value.filter(t => t.status === 'TODO').length);
const inProgressTasks = computed(() => tasks.value.filter(t => t.status === 'IN_PROGRESS').length);
const completedTasks = computed(() => tasks.value.filter(t => t.status === 'DONE').length);

// COMPUTED: Filtered Tasks
const filteredTasks = computed(() => {
  return tasks.value.filter(task => {
    const matchStatus = statusFilter.value === 'ALL' || task.status === statusFilter.value;
    const matchPriority = priorityFilter.value === 'ALL' || task.priority === priorityFilter.value;
    const matchSearch = !searchQuery.value.trim() || 
      task.title.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      (task.description && task.description.toLowerCase().includes(searchQuery.value.toLowerCase()));
    return matchStatus && matchPriority && matchSearch;
  });
});

async function handleCreateTask() {
  if (!newTitle.value.trim()) return;

  const input: TaskInputDTO = {
    title: newTitle.value.trim(),
    description: newDescription.value.trim(),
    priority: newPriority.value,
    dueDate: new Date(newDueDate.value).toISOString()
  };

  await taskStore.createTask(input);
  newTitle.value = '';
  newDescription.value = '';
  isCreateModalOpen.value = false;
}

function handleOpenAi(taskId: string) {
  aiStore.openAssistant(taskId);
}
</script>

<template>
  <div class="pt-28 pb-24 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto space-y-8">
    
    <!-- Hero Header: Task Management System -->
    <div class="flex flex-col lg:flex-row lg:items-end justify-between gap-6 pb-6 border-b border-white/10">
      <div>
        <div class="inline-flex items-center gap-2 rounded-full px-3.5 py-1 text-[11px] uppercase tracking-[0.2em] font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/25 mb-3 font-mono">
          <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
          <span>Hệ Thống Quản Lý Tác Vụ • Clean Architecture</span>
        </div>
        <h1 class="text-3xl sm:text-5xl font-extrabold text-white tracking-tight font-display">
          Task Management System
        </h1>
        <p class="text-xs sm:text-sm text-zinc-400 mt-2 max-w-2xl leading-relaxed">
          Ứng dụng quản trị tác vụ phát triển theo kiến trúc phần mềm sạch (Clean Architecture của Thầy Phước). Kết hợp tự động phân rã tác vụ và đánh giá độ ưu tiên bằng AI Copilot (OpenAI / Anthropic format).
        </p>
      </div>

      <!-- Action Buttons -->
      <div class="flex items-center gap-3">
        <button 
          @click="taskStore.fetchTasks()"
          class="px-4 py-2.5 rounded-full text-xs font-semibold bg-white/5 border border-white/10 text-zinc-300 hover:text-white hover:bg-white/10 transition-fluid flex items-center gap-2"
        >
          <svg class="w-3.5 h-3.5" :class="{ 'animate-spin': isLoading }" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
          </svg>
          <span>Đồng bộ API</span>
        </button>

        <button 
          @click="isCreateModalOpen = true"
          class="px-5 py-2.5 rounded-full text-xs font-bold bg-emerald-500 text-black hover:bg-emerald-400 shadow-lg shadow-emerald-500/20 active:scale-[0.98] transition-fluid flex items-center gap-2"
        >
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
          <span>Tạo Tác Vụ Mới</span>
        </button>
      </div>
    </div>

    <!-- Live Metrics Counter Cards -->
    <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
      <div class="p-4 rounded-2xl bg-white/[0.02] border border-white/10 flex items-center justify-between">
        <div>
          <span class="text-[11px] font-mono text-zinc-500 uppercase">Tổng Tác Vụ</span>
          <div class="text-2xl font-bold font-mono text-white mt-1">{{ totalTasks }}</div>
        </div>
        <div class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center text-zinc-400">
          📋
        </div>
      </div>

      <div class="p-4 rounded-2xl bg-blue-500/5 border border-blue-500/20 flex items-center justify-between">
        <div>
          <span class="text-[11px] font-mono text-blue-400 uppercase">Đang Làm (In Progress)</span>
          <div class="text-2xl font-bold font-mono text-blue-300 mt-1">{{ inProgressTasks }}</div>
        </div>
        <div class="w-8 h-8 rounded-full bg-blue-500/10 flex items-center justify-center text-blue-400">
          ⚡
        </div>
      </div>

      <div class="p-4 rounded-2xl bg-amber-500/5 border border-amber-500/20 flex items-center justify-between">
        <div>
          <span class="text-[11px] font-mono text-amber-400 uppercase">Cần Làm (To Do)</span>
          <div class="text-2xl font-bold font-mono text-amber-300 mt-1">{{ todoTasks }}</div>
        </div>
        <div class="w-8 h-8 rounded-full bg-amber-500/10 flex items-center justify-center text-amber-400">
          ⏳
        </div>
      </div>

      <div class="p-4 rounded-2xl bg-emerald-500/5 border border-emerald-500/20 flex items-center justify-between">
        <div>
          <span class="text-[11px] font-mono text-emerald-400 uppercase">Hoàn Thành (Done)</span>
          <div class="text-2xl font-bold font-mono text-emerald-300 mt-1">{{ completedTasks }}</div>
        </div>
        <div class="w-8 h-8 rounded-full bg-emerald-500/10 flex items-center justify-center text-emerald-400">
          ✓
        </div>
      </div>
    </div>

    <!-- Search & Filter Controls -->
    <div class="p-4 rounded-2xl bg-white/[0.02] border border-white/10 flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
      <!-- Status Tabs -->
      <div class="flex items-center gap-1.5 overflow-x-auto pb-2 md:pb-0 scrollbar-none text-xs font-mono">
        <button 
          v-for="st in ['ALL', 'TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED']"
          :key="st"
          @click="statusFilter = st"
          class="px-3.5 py-1.5 rounded-full transition-fluid"
          :class="statusFilter === st ? 'bg-emerald-500 text-black font-bold shadow-md' : 'text-zinc-400 hover:text-white bg-white/5'"
        >
          {{ st === 'ALL' ? 'TẤT CẢ' : st }}
        </button>
      </div>

      <!-- Priority Filter & Search -->
      <div class="flex items-center gap-3">
        <select 
          v-model="priorityFilter"
          class="bg-black/50 border border-white/10 rounded-full px-3 py-1.5 text-xs text-white focus:outline-none focus:border-emerald-500 font-mono"
        >
          <option value="ALL">Mọi Priority</option>
          <option value="CRITICAL">CRITICAL</option>
          <option value="HIGH">HIGH</option>
          <option value="MEDIUM">MEDIUM</option>
          <option value="LOW">LOW</option>
        </select>

        <div class="relative w-48 sm:w-60">
          <input 
            v-model="searchQuery"
            type="text"
            placeholder="Tìm kiếm tác vụ..."
            class="w-full bg-black/50 border border-white/10 rounded-full pl-8 pr-3 py-1.5 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-emerald-500 font-mono"
          />
          <svg class="w-3.5 h-3.5 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>
      </div>
    </div>

    <!-- Task Cards Grid (Double-Bezel Architecture) -->
    <div v-if="filteredTasks.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      
      <div 
        v-for="task in filteredTasks" 
        :key="task.taskId"
        class="bezel-outer p-1.5 bg-white/[0.02] border border-white/10 hover:border-emerald-500/40 transition-fluid flex flex-col justify-between"
      >
        <div class="bezel-inner p-5 bg-[#0c0c0e] flex flex-col h-full justify-between">
          
          <div>
            <!-- Header: Priority & Status Badges -->
            <div class="flex items-center justify-between gap-2 mb-3">
              <div class="flex items-center gap-2">
                <span 
                  class="px-2.5 py-0.5 rounded-full text-[10px] font-mono font-bold uppercase tracking-wider"
                  :class="{
                    'bg-rose-500/20 text-rose-300 border border-rose-500/40': task.priority === 'CRITICAL',
                    'bg-amber-500/20 text-amber-300 border border-amber-500/40': task.priority === 'HIGH',
                    'bg-blue-500/20 text-blue-300 border border-blue-500/40': task.priority === 'MEDIUM',
                    'bg-zinc-500/20 text-zinc-300 border border-zinc-500/40': task.priority === 'LOW'
                  }"
                >
                  {{ task.priority }}
                </span>

                <span 
                  class="px-2 py-0.5 rounded-full text-[9px] font-mono"
                  :class="{
                    'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20': task.status === 'DONE',
                    'bg-blue-500/10 text-blue-400 border border-blue-500/20': task.status === 'IN_PROGRESS',
                    'bg-zinc-500/10 text-zinc-400 border border-zinc-500/20': task.status === 'TODO',
                    'bg-rose-500/10 text-rose-400 border border-rose-500/20': task.status === 'CANCELLED'
                  }"
                >
                  {{ task.status }}
                </span>
              </div>

              <span class="text-[11px] font-mono text-zinc-500">
                Hạn: {{ new Date(task.dueDate).toLocaleDateString() }}
              </span>
            </div>

            <!-- Task Title & Description -->
            <h3 class="text-base font-bold text-white font-display leading-snug">
              {{ task.title }}
            </h3>
            <p class="text-xs text-zinc-400 mt-2 line-clamp-3 leading-relaxed">
              {{ task.description || 'Chưa có mô tả chi tiết cho tác vụ này.' }}
            </p>

            <!-- Subtasks Checklist -->
            <div v-if="task.subTasks && task.subTasks.length > 0" class="mt-4 pt-3 border-t border-white/5">
              <div class="text-[10px] font-mono uppercase text-zinc-500 mb-2 flex items-center justify-between">
                <span>Subtasks</span>
                <span>{{ task.subTasks.filter(s => s.isCompleted).length }}/{{ task.subTasks.length }}</span>
              </div>
              <div class="space-y-1.5 max-h-36 overflow-y-auto pr-1">
                <div 
                  v-for="sub in task.subTasks" 
                  :key="sub.id"
                  class="flex items-center gap-2 text-xs"
                >
                  <span 
                    class="w-3.5 h-3.5 rounded flex items-center justify-center text-[9px] border font-mono"
                    :class="sub.isCompleted ? 'bg-emerald-500 text-black border-emerald-400' : 'border-white/20 text-transparent'"
                  >
                    ✓
                  </span>
                  <span :class="sub.isCompleted ? 'line-through text-zinc-500' : 'text-zinc-300'">
                    {{ sub.title }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- Bottom Actions: State Transition & AI Copilot -->
          <div class="mt-6 pt-4 border-t border-white/5 flex flex-wrap items-center justify-between gap-2">
            <!-- State Transitions -->
            <div class="flex items-center gap-1.5">
              <button 
                v-if="task.status === 'TODO'"
                @click="taskStore.startTask(task.taskId)"
                class="px-2.5 py-1 rounded-full text-[11px] font-mono bg-blue-500/20 text-blue-300 hover:bg-blue-500/30 border border-blue-500/30 transition-fluid"
                title="Bắt đầu tác vụ"
              >
                Start ▶
              </button>
              
              <button 
                v-if="task.status === 'IN_PROGRESS'"
                @click="taskStore.completeTask(task.taskId)"
                class="px-2.5 py-1 rounded-full text-[11px] font-mono bg-emerald-500/20 text-emerald-300 hover:bg-emerald-500/30 border border-emerald-500/30 transition-fluid"
                title="Đánh dấu hoàn thành"
              >
                Complete ✓
              </button>

              <button 
                v-if="task.status !== 'DONE' && task.status !== 'CANCELLED'"
                @click="taskStore.cancelTask(task.taskId)"
                class="px-2.5 py-1 rounded-full text-[11px] font-mono text-zinc-500 hover:text-rose-400 hover:bg-rose-500/10 transition-fluid"
                title="Hủy tác vụ"
              >
                Cancel
              </button>
            </div>

            <!-- Trigger AI Assistant Copilot -->
            <button 
              @click="handleOpenAi(task.taskId)"
              class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 hover:bg-emerald-500/20 transition-fluid group"
            >
              <svg class="w-3.5 h-3.5 transition-transform group-hover:rotate-12" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
              </svg>
              <span>AI Copilot</span>
            </button>
          </div>

        </div>
      </div>

    </div>

    <!-- Empty State -->
    <div v-else class="text-center py-20 bezel-outer max-w-md mx-auto">
      <div class="bezel-inner p-8">
        <div class="w-12 h-12 rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-zinc-500 mx-auto mb-3">
          📋
        </div>
        <h3 class="text-sm font-bold text-white mb-1">Không có tác vụ nào phù hợp</h3>
        <p class="text-xs text-zinc-400 mb-4">Hãy thay đổi bộ lọc trạng thái hoặc bấm "Tạo Tác Vụ Mới".</p>
      </div>
    </div>

    <!-- Create Task Modal -->
    <Teleport to="body">
      <div 
        v-if="isCreateModalOpen"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md"
        @click.self="isCreateModalOpen = false"
      >
        <div class="bezel-outer max-w-lg w-full bg-white/[0.02]">
          <div class="bezel-inner p-6 bg-[#0c0c0e]">
            <h3 class="text-base font-bold text-white mb-4 font-display">Tạo Tác Vụ Mới</h3>

            <div class="space-y-4 text-xs font-mono">
              <div>
                <label class="block text-zinc-400 mb-1">Tiêu đề (Task Title):</label>
                <input 
                  v-model="newTitle"
                  type="text"
                  placeholder="Ví dụ: Thiết kế hệ thống bàn phím công thái học..."
                  class="w-full bg-black/50 border border-white/10 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div>
                <label class="block text-zinc-400 mb-1">Mô tả chi tiết:</label>
                <textarea 
                  v-model="newDescription"
                  rows="3"
                  placeholder="Chi tiết yêu cầu kỹ thuật..."
                  class="w-full bg-black/50 border border-white/10 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-emerald-500"
                ></textarea>
              </div>

              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label class="block text-zinc-400 mb-1">Mức ưu tiên (Priority):</label>
                  <select 
                    v-model="newPriority"
                    class="w-full bg-black/50 border border-white/10 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-emerald-500"
                  >
                    <option value="LOW">LOW</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="HIGH">HIGH</option>
                    <option value="CRITICAL">CRITICAL</option>
                  </select>
                </div>

                <div>
                  <label class="block text-zinc-400 mb-1">Hạn hoàn thành (Due Date):</label>
                  <input 
                    v-model="newDueDate"
                    type="date"
                    class="w-full bg-black/50 border border-white/10 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-emerald-500"
                  />
                </div>
              </div>
            </div>

            <div class="mt-6 pt-4 border-t border-white/10 flex items-center justify-end gap-3">
              <button 
                @click="isCreateModalOpen = false"
                class="px-4 py-2 rounded-full text-xs font-medium text-zinc-400 hover:text-white"
              >
                Hủy
              </button>
              <button 
                @click="handleCreateTask"
                class="px-5 py-2 rounded-full text-xs font-bold bg-emerald-500 text-black hover:bg-emerald-400"
              >
                Xác nhận tạo
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

  </div>
</template>
