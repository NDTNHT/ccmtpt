import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia, disposePinia } from 'pinia';
import { useTaskStore, STORAGE_KEY } from './useTaskStore.js';
import { apiService } from '../services/apiService.js';
import { mockAnalysis } from '../services/mockAi.js';
vi.mock('../services/apiService.js', () => ({ apiService: {
  getTasks: vi.fn(), createTask: vi.fn(), startTask: vi.fn(), completeTask: vi.fn(),
  cancelTask: vi.fn(), setSubTaskCompleted: vi.fn(), applyDecomposition: vi.fn(), getTaskById: vi.fn(),
  applyDeadline: vi.fn(), applyPriority: vi.fn()
} }));
let pinia;
beforeEach(() => {
  const data = new Map();
  vi.stubGlobal('localStorage', { getItem: k => data.get(k) ?? null, setItem: (k, v) => data.set(k, v), removeItem: k => data.delete(k) });
  pinia = createPinia(); setActivePinia(pinia); vi.clearAllMocks();
});
afterEach(() => { disposePinia(pinia); vi.unstubAllGlobals(); });
async function localStore() { const store = useTaskStore(); await store.setLocalMode(true); return store; }
describe('Task workspace', () => {
  it('persists checklist, lifecycle and computed Kanban progress across reload', async () => {
    const s = await localStore();
    const task = await s.createTask({ title: 'Write report', subTasks: ['Draft', 'Review'] });
    await s.toggleSubTask(task.id, task.subTasks[0].id);
    await s.startTask(task.id); await s.completeTask(task.id);
    expect(s.kanbanColumns.value[2].tasks).toHaveLength(1);
    expect(s.taskStats.value.completionRate).toBe(100);
    disposePinia(pinia); pinia = createPinia(); setActivePinia(pinia);
    const restored = useTaskStore();
    expect(restored.state.tasks[0].subTasks[0].completed).toBe(true);
    expect(restored.state.tasks[0].status).toBe('DONE');
  });
  it('rejects illegal lifecycle transitions and edits on terminal tasks', async () => {
    const s = await localStore(); const t = await s.createTask({ title: 'Test task' });
    await expect(s.completeTask(t.id)).rejects.toThrow();
    await s.cancelTask(t.id);
    await expect(s.addSubTasks(t.id, ['More'])).rejects.toThrow();
  });
  it('filters by priority, keyword, overdue and no deadline', async () => {
    const s = await localStore();
    await s.createTask({ title: 'Report overdue', priority: 'HIGH', dueDate: new Date(Date.now() - 10000) });
    await s.createTask({ title: 'Other task' });
    s.setFilters({ search: 'report', priority: 'HIGH', deadline: 'OVERDUE' });
    expect(s.filteredTasks.value).toHaveLength(1);
    s.setFilters({ search: '', priority: 'ALL', deadline: 'NONE' });
    expect(s.filteredTasks.value[0].title).toBe('Other task');
  });
  it('treats an empty server list as authoritative and retains local workspace separately', async () => {
    const s = await localStore(); await s.createTask({ title: 'Local work' });
    apiService.getTasks.mockResolvedValue([]);
    await s.setLocalMode(false); expect(s.state.tasks).toEqual([]);
    await s.setLocalMode(true); expect(s.state.tasks[0].title).toBe('Local work');
  });
  it('never pretends a rejected server transition succeeded', async () => {
    const s = useTaskStore(); s.state.tasks = [{ id: 'server-id', title: 'Server task', status: 'TODO', subTasks: [] }];
    apiService.startTask.mockRejectedValue(new Error('409'));
    await expect(s.startTask('server-id')).rejects.toThrow('409');
    expect(s.state.tasks[0].status).toBe('TODO');
  });
  it('saves server checklist using the API result, not only local memory', async () => {
    const s = useTaskStore(); s.state.tasks = [{ id: 't', title: 'Server', status: 'TODO', subTasks: [{ id: 's', title: 'Step', completed: false }] }];
    apiService.setSubTaskCompleted.mockResolvedValue({ taskId: 't', title: 'Server', status: 'TODO', subTasks: [{ subTaskId: 's', title: 'Step', completed: true }] });
    await s.toggleSubTask('t', 's');
    expect(apiService.setSubTaskCompleted).toHaveBeenCalledWith('t', 's', true);
    expect(s.state.tasks[0].subTasks[0].completed).toBe(true);
  });
  it('handles corrupt storage and maps legacy status', () => {
    localStorage.setItem(STORAGE_KEY, '{bad json');
    expect(useTaskStore().state.storageError).toBeTruthy();
  });
  it('normalizes legacy COMPLETED and counts it as Done', () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ version: 1, tasks: [{ id: 'x', title: 'Legacy', status: 'COMPLETED' }] }));
    expect(useTaskStore().taskStats.value.completed).toBe(1);
  });
  it('mock AI reports actual checklist progress and returns a usable deadline', () => {
    const result = mockAnalysis('FULL_ANALYSIS', { id: 'x', title: 'Report', status: 'IN_PROGRESS', subTasks: [{ completed: true }, { completed: false }] });
    expect(result.summary.completionPercentage).toBe(50);
    expect(new Date(result.deadlineEstimation.recommendedDeadline).getTime()).toBeGreaterThan(Date.now());
    expect(result.summary.summary).toContain('MOCK LOCAL');
  });
});
