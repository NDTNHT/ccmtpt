export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED';

export interface SubTaskOutputDTO {
  id: string;
  title: string;
  isCompleted: boolean;
}

export interface TaskOutputDTO {
  taskId: string;
  title: string;
  description: string;
  status: TaskStatus;
  priority: Priority;
  dueDate: string;
  isOverdue: boolean;
  projectId?: string;
  subTasks: SubTaskOutputDTO[];
}

export interface TaskInputDTO {
  title: string;
  description: string;
  dueDate: string;
  priority: Priority;
  projectId?: string;
}

export interface ProjectOutputDTO {
  projectId: string;
  name: string;
  description?: string;
  createdAt: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}
