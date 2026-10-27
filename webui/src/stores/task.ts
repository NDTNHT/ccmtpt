import { defineStore } from 'pinia';
import { ref } from 'vue';
import { taskService } from '../services/taskService';
import type { TaskOutputDTO, TaskInputDTO, ProjectOutputDTO } from '../types/task';

export const useTaskStore = defineStore('task', () => {
  const tasks = ref<TaskOutputDTO[]>([]);
  const projects = ref<ProjectOutputDTO[]>([]);
  const selectedTaskId = ref<string | null>(null);
  const isLoading = ref<boolean>(false);
  const error = ref<string | null>(null);

  // Fallback initial tasks for demonstration
  const fallbackTasks: TaskOutputDTO[] = [
    {
      taskId: '3fa85f64-5717-4562-b3fc-2c963f66afa6',
      title: 'Tích hợp mô hình AI Custom Provider cho Clean Architecture',
      description: 'Hiện thực bộ chuyển đổi OpenAI và Anthropic protocol độc lập với Spring Boot framework.',
      status: 'IN_PROGRESS',
      priority: 'HIGH',
      dueDate: new Date(Date.now() + 86400000 * 3).toISOString(),
      isOverdue: false,
      subTasks: [
        { id: 'st-1', title: 'Định nghĩa Domain Port AiModelPort', isCompleted: true },
        { id: 'st-2', title: 'Hiện thực CustomAiAdapter với HTTP Client', isCompleted: true },
        { id: 'st-3', title: 'Viết Unit Test kiểm thử 100% boundary', isCompleted: false }
      ]
    },
    {
      taskId: '7ba45f12-9812-4211-a1ab-5c862f11df99',
      title: 'Tối ưu hóa cơ chế Proxy Reactivity trong Vue 3 SPA',
      description: 'Chuyển đổi toàn bộ quản lý state sang Composition API và Pinia Setup Store.',
      status: 'TODO',
      priority: 'CRITICAL',
      dueDate: new Date(Date.now() + 86400000).toISOString(),
      isOverdue: false,
      subTasks: [
        { id: 'st-4', title: 'Thiết lập Pinia Store với computed getters', isCompleted: true },
        { id: 'st-5', title: 'Tích hợp Vue Router dynamic route params', isCompleted: false }
      ]
    },
    {
      taskId: '8bc91e44-1123-45ef-b124-77e8412356ab',
      title: 'Thiết lập Pipeline CI/CD với GitHub Actions',
      description: 'Tự động chạy 153 Unit Tests của Backend và build Vite SPA khi có commit mới.',
      status: 'DONE',
      priority: 'MEDIUM',
      dueDate: new Date(Date.now() - 86400000).toISOString(),
      isOverdue: false,
      subTasks: [
        { id: 'st-6', title: 'Cấu hình deploy.yml trên GitHub Actions', isCompleted: true },
        { id: 'st-7', title: 'Upload artifact bản build webui/dist', isCompleted: true }
      ]
    }
  ];

  async function fetchTasks() {
    isLoading.value = true;
    error.value = null;
    try {
      const data = await taskService.getTasks();
      if (data && data.length > 0) {
        tasks.value = data;
      } else {
        tasks.value = fallbackTasks;
      }
    } catch (err: any) {
      console.warn('Backend tasks offline or empty, using seed tasks:', err.message);
      tasks.value = fallbackTasks;
    } finally {
      isLoading.value = false;
    }
  }

  async function createTask(input: TaskInputDTO) {
    isLoading.value = true;
    error.value = null;
    try {
      const newTask = await taskService.createTask(input);
      tasks.value.unshift(newTask);
      return newTask;
    } catch (err: any) {
      const localTask: TaskOutputDTO = {
        taskId: crypto.randomUUID ? crypto.randomUUID() : `task-${Date.now()}`,
        title: input.title,
        description: input.description,
        status: 'TODO',
        priority: input.priority,
        dueDate: input.dueDate,
        isOverdue: false,
        subTasks: []
      };
      tasks.value.unshift(localTask);
      return localTask;
    } finally {
      isLoading.value = false;
    }
  }

  async function startTask(taskId: string) {
    try {
      const updated = await taskService.startTask(taskId);
      updateLocalTask(updated);
    } catch {
      const t = tasks.value.find(item => item.taskId === taskId);
      if (t) t.status = 'IN_PROGRESS';
    }
  }

  async function completeTask(taskId: string) {
    try {
      const updated = await taskService.completeTask(taskId);
      updateLocalTask(updated);
    } catch {
      const t = tasks.value.find(item => item.taskId === taskId);
      if (t) t.status = 'DONE';
    }
  }

  async function cancelTask(taskId: string) {
    try {
      const updated = await taskService.cancelTask(taskId);
      updateLocalTask(updated);
    } catch {
      const t = tasks.value.find(item => item.taskId === taskId);
      if (t) t.status = 'CANCELLED';
    }
  }

  function updateLocalTask(updated: TaskOutputDTO) {
    const idx = tasks.value.findIndex(item => item.taskId === updated.taskId);
    if (idx !== -1) {
      tasks.value[idx] = updated;
    }
  }

  function selectTask(id: string) {
    selectedTaskId.value = id;
  }

  return {
    tasks,
    projects,
    selectedTaskId,
    isLoading,
    error,
    fetchTasks,
    createTask,
    startTask,
    completeTask,
    cancelTask,
    selectTask
  };
});
