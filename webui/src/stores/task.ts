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

  // Fallback seed tasks for offline demo or when backend is freshly booted
  const fallbackTasks: TaskOutputDTO[] = [
    {
      taskId: '3fa85f64-5717-4562-b3fc-2c963f66afa6',
      title: 'Complete Ergonomic Split Keyboard Firmware Integration',
      description: 'Integrate QMK wireless layers and implement low-latency Bluetooth 5.2 battery sleep telemetry.',
      status: 'IN_PROGRESS',
      priority: 'HIGH',
      dueDate: new Date(Date.now() + 86400000 * 3).toISOString(),
      isOverdue: false,
      subTasks: [
        { id: 'st-1', title: 'Flash ARM Cortex-M4 bootloader', isCompleted: true },
        { id: 'st-2', title: 'Calibrate OLED display refresh rate', isCompleted: false },
        { id: 'st-3', title: 'Verify anti-chatter switch debouncing', isCompleted: false }
      ]
    },
    {
      taskId: '7ba45f12-9812-4211-a1ab-5c862f11df99',
      title: 'Audit Audio Coaxial Waveguide Acoustical Response',
      description: 'Perform laser vibrometry on pure beryllium dome to eliminate high-frequency distortion above 20kHz.',
      status: 'TODO',
      priority: 'CRITICAL',
      dueDate: new Date(Date.now() + 86400000).toISOString(),
      isOverdue: false,
      subTasks: [
        { id: 'st-4', title: 'Run pink noise swept sine wave in anechoic chamber', isCompleted: false },
        { id: 'st-5', title: 'Calculate FIR DSP coefficient tables', isCompleted: false }
      ]
    },
    {
      taskId: '8bc91e44-1123-45ef-b124-77e8412356ab',
      title: 'Haptic Dial Force Feedback Curve Profiling',
      description: 'Program magnetic virtual detents in firmware to replicate mechanical camera aperture ring clicks.',
      status: 'TODO',
      priority: 'MEDIUM',
      dueDate: new Date(Date.now() + 86400000 * 7).toISOString(),
      isOverdue: false,
      subTasks: []
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
      console.warn('Backend tasks offline, using seeded tasks:', err.message);
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
      // Local fallback creation
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

  function selectTask(id: string) {
    selectedTaskId.value = id;
  }

  function getSelectedTask(): TaskOutputDTO | undefined {
    return tasks.value.find(t => t.taskId === selectedTaskId.value) || tasks.value[0];
  }

  return {
    tasks,
    projects,
    selectedTaskId,
    isLoading,
    error,
    fetchTasks,
    createTask,
    selectTask,
    getSelectedTask
  };
});
