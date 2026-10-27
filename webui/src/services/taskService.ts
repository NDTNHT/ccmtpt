import { apiClient } from './apiClient';
import type { TaskOutputDTO, TaskInputDTO, ProjectOutputDTO, ApiResponse } from '../types/task';

export const taskService = {
  async getTasks(): Promise<TaskOutputDTO[]> {
    const res = await apiClient.get<ApiResponse<TaskOutputDTO[]>>('/tasks');
    return res.data.data;
  },

  async getTask(taskId: string): Promise<TaskOutputDTO> {
    const res = await apiClient.get<ApiResponse<TaskOutputDTO>>(`/tasks/${taskId}`);
    return res.data.data;
  },

  async createTask(input: TaskInputDTO): Promise<TaskOutputDTO> {
    const res = await apiClient.post<ApiResponse<TaskOutputDTO>>('/tasks', input);
    return res.data.data;
  },

  async startTask(taskId: string): Promise<TaskOutputDTO> {
    const res = await apiClient.post<ApiResponse<TaskOutputDTO>>(`/tasks/${taskId}/start`);
    return res.data.data;
  },

  async completeTask(taskId: string): Promise<TaskOutputDTO> {
    const res = await apiClient.post<ApiResponse<TaskOutputDTO>>(`/tasks/${taskId}/complete`);
    return res.data.data;
  },

  async cancelTask(taskId: string): Promise<TaskOutputDTO> {
    const res = await apiClient.post<ApiResponse<TaskOutputDTO>>(`/tasks/${taskId}/cancel`);
    return res.data.data;
  },

  async rescheduleTask(taskId: string, newDueDate: string): Promise<TaskOutputDTO> {
    const res = await apiClient.patch<ApiResponse<TaskOutputDTO>>(`/tasks/${taskId}/deadline`, {
      newDueDate
    });
    return res.data.data;
  },

  async getProjects(): Promise<ProjectOutputDTO[]> {
    const res = await apiClient.get<ApiResponse<ProjectOutputDTO[]>>('/projects');
    return res.data.data;
  }
};
