import { describe, it, expect, beforeEach } from 'vitest';
import { taskService } from '@/services/taskService.js';

describe('services/taskService', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  describe('Task CRUD and Lifecycle', () => {
    it('returns default tasks on initial load', () => {
      const tasks = taskService.getTasks();
      expect(Array.isArray(tasks)).toBe(true);
      expect(tasks.length).toBeGreaterThan(0);
      expect(tasks[0]).toHaveProperty('id');
      expect(tasks[0]).toHaveProperty('title');
    });

    it('creates a new task with default status and formatted fields', () => {
      const created = taskService.createTask({
        title: 'test mission',
        description: 'execute protocol check',
        priority: 'HIGH',
        dueDate: '2026-12-31',
        projectId: 'proj-omega-01',
        subTasks: ['Step 1', { title: 'Step 2', completed: true }]
      });

      expect(created.id).toMatch(/^task-\d+$/);
      expect(created.title).toBe('TEST MISSION');
      expect(created.status).toBe('TODO');
      expect(created.priority).toBe('HIGH');
      expect(created.subTasks).toHaveLength(2);
      expect(created.subTasks[0].completed).toBe(false);
      expect(created.subTasks[1].completed).toBe(true);

      const found = taskService.getTaskById(created.id);
      expect(found.title).toBe('TEST MISSION');
    });

    it('throws error when retrieving non-existent task by ID', () => {
      expect(() => taskService.getTaskById('non-existent-id')).toThrow(/Task not found/);
    });

    it('updates task properties partially', () => {
      const created = taskService.createTask({ title: 'Task to Update' });
      const updated = taskService.updateTask(created.id, { description: 'Updated details' });

      expect(updated.description).toBe('Updated details');
      expect(taskService.getTaskById(created.id).description).toBe('Updated details');
    });

    it('transitions task status with startTask, completeTask, and cancelTask', () => {
      const task = taskService.createTask({ title: 'Lifecycle task' });
      expect(task.status).toBe('TODO');

      const started = taskService.startTask(task.id);
      expect(started.status).toBe('IN_PROGRESS');

      const completed = taskService.completeTask(task.id);
      expect(completed.status).toBe('COMPLETED');

      const cancelled = taskService.cancelTask(task.id);
      expect(cancelled.status).toBe('CANCELLED');
    });

    it('refreshes task priority dynamically from its dueDate', () => {
      const task = taskService.createTask({
        title: 'Priority check task',
        dueDate: new Date(Date.now() + 86400000 * 10).toISOString().slice(0, 10),
        priority: 'HIGH'
      });

      const refreshed = taskService.refreshPriority(task.id);
      expect(refreshed.priority).toBe('LOW');
    });
  });

  describe('Subtasks Operations', () => {
    it('toggles a subtask completion status', () => {
      const task = taskService.createTask({
        title: 'Task with subtasks',
        subTasks: ['Subtask Alpha']
      });
      const stId = task.subTasks[0].id;
      expect(task.subTasks[0].completed).toBe(false);

      const toggled = taskService.toggleSubTask(task.id, stId);
      expect(toggled.subTasks[0].completed).toBe(true);

      const toggledBack = taskService.toggleSubTask(task.id, stId);
      expect(toggledBack.subTasks[0].completed).toBe(false);
    });

    it('adds new subtasks to an existing task', () => {
      const task = taskService.createTask({ title: 'Task for append' });
      const updated = taskService.addSubTasks(task.id, ['New step 1', 'New step 2']);

      expect(updated.subTasks).toHaveLength(2);
      expect(updated.subTasks[0].title).toBe('New step 1');
      expect(updated.subTasks[1].title).toBe('New step 2');
    });
  });

  describe('Projects & Database Backup', () => {
    it('retrieves default projects and creates a new project', () => {
      const projects = taskService.getProjects();
      expect(projects.length).toBeGreaterThan(0);

      const newProj = taskService.createProject({
        name: 'Cyber Operations',
        description: 'New tactical sector'
      });
      expect(newProj.name).toBe('CYBER OPERATIONS');
      expect(newProj.projectType).toBe('ACTIVE');

      const updated = taskService.getProjects();
      expect(updated.some((p) => p.id === newProj.id)).toBe(true);
    });

    it('imports database backup JSON successfully and rejects invalid format', () => {
      const validBackup = JSON.stringify({
        tasks: [{ id: 'task-imp-1', title: 'IMPORTED TASK', status: 'TODO', priority: 'HIGH' }],
        projects: [{ id: 'proj-imp-1', name: 'IMPORTED PROJECT', projectType: 'ACTIVE' }]
      });

      expect(taskService.importDatabase(validBackup)).toBe(true);
      expect(taskService.getTasks().some((t) => t.id === 'task-imp-1')).toBe(true);

      expect(() => taskService.importDatabase('INVALID_JSON')).toThrow(/Invalid JSON/);
    });
  });
});
