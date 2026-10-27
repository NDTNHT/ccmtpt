<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { useTaskStore } from '../stores/task';
import { useAiStore } from '../stores/ai';
import type { Priority, TaskInputDTO } from '../types/task';

const taskStore = useTaskStore();
const aiStore = useAiStore();

const { tasks, isLoading, error } = storeToRefs(taskStore);

// New Task Form Modal State
const isCreateModalOpen = ref<boolean>(false);
const newTitle = ref<string>('');
const newDescription = ref<string>('');
const newPriority = ref<Priority>('MEDIUM');
const newDueDate = ref<string>(new Date(Date.now() + 86400000 * 3).toISOString().slice(0, 10));

onMounted(async () => {
  await taskStore.fetchTasks();
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
  <div class="pt-28 pb-24 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
    
    <!-- Header Section -->
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-6 pb-8 border-b border-white/10">
      <div>
        <div class="inline-flex items-center gap-2 rounded-full px-3 py-1 text-[10px] uppercase tracking-[0.2em] font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 mb-3 font-mono">
          <span>Clean Architecture Delivery Mechanism</span>
        </div>
        <h1 class="text-3xl sm:text-4xl font-extrabold text-white tracking-tight font-display">
          Quản Lý Tác Vụ &amp; AI Breakdown
        </h1>
        <p class="text-xs sm:text-sm text-zinc-400 mt-2 max-w-2xl leading-relaxed">
          Giao diện Vue 3 đóng vai trò là lớp <strong>Delivery Mechanism (Humble View)</strong> bên ngoài cùng, kết nối tới Spring Boot REST API tuân thủ nghiêm ngặt 4 vòng tròn Clean Architecture của Thầy Phước.
        </p>
      </div>

      <!-- Action Buttons -->
      <div class="flex items-center gap-3">
        <button 
          @click="taskStore.fetchTasks()"
          class="px-4 py-2 rounded-full text-xs font-semibold bg-white/5 border border-white/10 text-zinc-300 hover:text-white hover:bg-white/10 transition-fluid flex items-center gap-2"
        >
          <svg class="w-3.5 h-3.5" :class="{ 'animate-spin': isLoading }" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
          </svg>
          <span>Làm mới</span>
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

    <!-- Clean Architecture Architectural Ribbon -->
    <div class="mt-6 mb-10 grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs font-mono">
      <div class="p-3 rounded-2xl bg-white/[0.02] border border-white/5">
        <div class="text-[10px] text-zinc-500">1. ENTERPRISE RULES</div>
        <div class="text-white font-semibold mt-1">Task &amp; SubTask Entities</div>
        <div class="text-[10px] text-emerald-400 mt-0.5">Zero Framework Deps</div>
      </div>
      <div class="p-3 rounded-2xl bg-white/[0.02] border border-white/5">
        <div class="text-[10px] text-zinc-500">2. APPLICATION RULES</div>
        <div class="text-white font-semibold mt-1">Use Case Boundaries</div>
        <div class="text-[10px] text-emerald-400 mt-0.5">TaskControl &amp; AI Boundary</div>
      </div>
      <div class="p-3 rounded-2xl bg-white/[0.02] border border-white/5">
        <div class="text-[10px] text-zinc-500">3. INTERFACE ADAPTERS</div>
        <div class="text-white font-semibold mt-1">REST Controllers &amp; Ports</div>
        <div class="text-[10px] text-emerald-400 mt-0.5">OpenAI/Claude Adapters</div>
      </div>
      <div class="p-3 rounded-2xl bg-white/[0.02] border border-white/5">
        <div class="text-[10px] text-zinc-500">4. FRAMEWORKS &amp; DRIVERS</div>
        <div class="text-white font-semibold mt-1">Vue 3 SPA &amp; Spring Boot</div>
        <div class="text-[10px] text-emerald-400 mt-0.5">Vite + Pinia Reactive Proxy</div>
      </div>
    </div>

    <!-- Task List (Double-Bezel Grid) -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      
      <div 
        v-for="task in tasks" 
        :key="task.taskId"
        class="bezel-outer p-1.5 bg-white/[0.02] border border-white/10 hover:border-emerald-500/40 transition-fluid flex flex-col justify-between"
      >
        <div class="bezel-inner p-5 bg-[#0c0c0e] flex flex-col h-full justify-between">
          
          <div>
            <!-- Status & Priority Badges -->
            <div class="flex items-center justify-between gap-2 mb-3">
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

              <span class="text-[11px] font-mono text-zinc-400">
                Hạn: {{ new Date(task.dueDate).toLocaleDateString() }}
              </span>
            </div>

            <!-- Title & Description -->
            <h3 class="text-base font-bold text-white font-display leading-snug">
              {{ task.title }}
            </h3>
            <p class="text-xs text-zinc-400 mt-2 line-clamp-3 leading-relaxed">
              {{ task.description || 'Chưa có mô tả chi tiết.' }}
            </p>

            <!-- Subtasks Checklist -->
            <div v-if="task.subTasks && task.subTasks.length > 0" class="mt-4 pt-4 border-t border-white/5">
              <div class="text-[10px] font-mono uppercase text-zinc-500 mb-2">
                Subtasks ({{ task.subTasks.filter(s => s.isCompleted).length }}/{{ task.subTasks.length }})
              </div>
              <div class="space-y-1.5">
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

          <!-- Bottom Actions: Trigger AI Assistant Copilot -->
          <div class="mt-6 pt-4 border-t border-white/5 flex items-center justify-between gap-2">
            <span class="text-[10px] font-mono text-zinc-500 truncate max-w-[120px]">
              ID: {{ task.taskId.slice(0, 8) }}...
            </span>

            <button 
              @click="handleOpenAi(task.taskId)"
              class="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 hover:bg-emerald-500/20 transition-fluid"
            >
              <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
              </svg>
              <span>AI Copilot</span>
            </button>
          </div>

        </div>
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
