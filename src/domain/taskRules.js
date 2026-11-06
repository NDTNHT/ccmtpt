/**
 * Domain Rules: Task Validation, Enums, and State Rules
 */

export const TASK_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export const TASK_PRIORITY_OPTIONS = [
  { value: 'LOW', label: 'LOW' },
  { value: 'MEDIUM', label: 'MEDIUM' },
  { value: 'HIGH', label: 'HIGH' },
  { value: 'CRITICAL', label: 'CRITICAL' }
];

export const TASK_STATUS_OPTIONS = ['ALL', 'TODO', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

export function validateTaskTitle(title) {
  const len = (title || '').trim().length;
  if (len === 0) return null;
  if (len < 3) return 'Title must contain at least 3 characters.';
  if (len > 100) return 'Title cannot exceed 100 characters.';
  return null;
}

export function isTaskTitleValid(title) {
  const len = (title || '').trim().length;
  return len >= 3 && len <= 100;
}

export function getStatusColorClass(status) {
  switch (status) {
    case 'IN_PROGRESS':
      return 'text-amber';
    case 'COMPLETED':
      return 'text-green';
    case 'CANCELLED':
      return 'text-dim';
    default:
      return 'text-main';
  }
}

export function calculatePriorityFromDueDate(dueDate) {
  if (!dueDate) return 'MEDIUM';
  const diffDays = (new Date(dueDate) - new Date()) / (1000 * 60 * 60 * 24);
  if (diffDays < 1) return 'CRITICAL';
  if (diffDays < 3) return 'HIGH';
  if (diffDays < 7) return 'MEDIUM';
  return 'LOW';
}


