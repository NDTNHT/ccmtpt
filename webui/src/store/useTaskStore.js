import { reactive, computed, watch, onScopeDispose } from 'vue';
import { defineStore, storeToRefs } from 'pinia';
import { apiService } from '../services/apiService.js';

export const STORAGE_KEY = 'task-management:v1';
export function normalizeTask(task) {
  return { ...task, id: String(task.taskId || task.id),
    status: task.status === 'COMPLETED' ? 'DONE' : task.status,
    subTasks: (task.subTasks || []).map(s => ({ ...s, id: String(s.subTaskId || s.id), completed: s.completed !== undefined ? Boolean(s.completed) : Boolean(s.isCompleted) })) };
}
const useInternalTaskStore = defineStore('tasks', () => {
  const state = reactive({ tasks: [], selectedTask: null, hoveredTask: null,
    statusFilter: 'ALL', priorityFilter: 'ALL', deadlineFilter: 'ALL', searchQuery: '',
    loading: false, error: null, storageError: null, localMode: false });
  try {
    const saved = JSON.parse(globalThis.localStorage?.getItem(STORAGE_KEY) || 'null');
    if (saved?.version === 1 && Array.isArray(saved.tasks)) {
      state.tasks = saved.tasks.filter(t => t && (t.id || t.taskId) && typeof t.title === 'string').map(normalizeTask);
      state.localMode = saved.localMode === true;
    }
  } catch { state.storageError = 'Không đọc được bộ nhớ trình duyệt; đang dùng bộ nhớ tạm.'; }
  watch(() => [state.tasks, state.localMode], () => {
    try {
      globalThis.localStorage?.setItem(STORAGE_KEY, JSON.stringify({ version: 1, tasks: state.tasks, localMode: state.localMode }));
      state.storageError = null;
    } catch { state.storageError = 'Không thể lưu localStorage. Hãy xuất dữ liệu hoặc giải phóng dung lượng.'; }
  }, { deep: true, flush: 'sync' });
  // Refresh time-sensitive computed values even when the board stays open overnight.
  const clock = reactive({ now: Date.now() });
  const timer = setInterval(() => { clock.now = Date.now(); }, 60000);
  timer.unref?.();
  onScopeDispose(() => clearInterval(timer));
  const filteredTasks = computed(() => state.tasks.filter(t => {
    const q = state.searchQuery.trim().toLowerCase();
    const due = t.dueDate ? new Date(t.dueDate).getTime() : null;
    const day = new Date(clock.now); day.setHours(0, 0, 0, 0);
    const end = new Date(day); end.setDate(end.getDate() + 1);
    const active = !['DONE', 'CANCELLED'].includes(t.status);
    const deadlineMatch = state.deadlineFilter === 'ALL'
      || (state.deadlineFilter === 'NONE' && due === null)
      || (state.deadlineFilter === 'OVERDUE' && active && due !== null && due < clock.now)
      || (state.deadlineFilter === 'TODAY' && due !== null && due >= +day && due < +end)
      || (state.deadlineFilter === 'WEEK' && active && due !== null && due >= clock.now && due <= clock.now + 7 * 86400000);
    return (state.statusFilter === 'ALL' || t.status === state.statusFilter)
      && (state.priorityFilter === 'ALL' || t.priority === state.priorityFilter)
      && deadlineMatch && (!q || [t.title, t.description, t.id].some(v => v?.toLowerCase().includes(q)));
  }));
  const kanbanColumns = computed(() => ['TODO', 'IN_PROGRESS', 'DONE'].map(status => ({
    status, title: { TODO: 'To Do', IN_PROGRESS: 'In Progress', DONE: 'Done' }[status],
    tasks: filteredTasks.value.filter(t => t.status === status)
  })));
  const taskStats = computed(() => {
    const total = state.tasks.length;
    const count = status => state.tasks.filter(t => t.status === status).length;
    const completed = count('DONE');
    return { total, todo: count('TODO'), inProgress: count('IN_PROGRESS'), completed,
      cancelled: count('CANCELLED'), completionRate: total ? Math.round(completed / total * 100) : 0 };
  });
  async function fetchTasks() {
    if (state.localMode) return;
    state.loading = true; state.error = null;
    try {
      const data = await apiService.getTasks();
      if (!Array.isArray(data)) throw new Error('Invalid task response');
      // An empty server result is authoritative too.
      const summaries = new Map(state.tasks.map(t => [t.id, t.aiSummary]));
      state.tasks = data.map(t => ({ ...normalizeTask(t), aiSummary: summaries.get(String(t.taskId || t.id)) }));
      refreshSelection();
    } catch (err) { state.error = `Không kết nối được máy chủ: ${err.message}. Dữ liệu cache chỉ để xem; chọn chế độ local để làm việc độc lập.`; }
    finally { state.loading = false; }
  }
  function refreshSelection() {
    for (const key of ['selectedTask', 'hoveredTask']) {
      if (state[key]) state[key] = state.tasks.find(t => t.id === state[key].id) || null;
    }
  }
  function getTask(id) {
    const task = state.tasks.find(t => t.id === id);
    if (!task) throw new Error('Không tìm thấy công việc.');
    return task;
  }
  function assertMutable(task) {
    if (['DONE', 'CANCELLED'].includes(task.status)) throw new Error('Công việc đã kết thúc, không thể chỉnh sửa.');
  }
  function updateTaskInState(id, partial) {
    const i = state.tasks.findIndex(t => t.id === id);
    if (i < 0) return;
    state.tasks[i] = normalizeTask({ ...state.tasks[i], ...partial }); refreshSelection();
  }
  async function createTask(payload) {
    const title = payload.title?.trim();
    if (!title || title.length < 3 || title.length > 100) throw new Error('Tiêu đề phải có từ 3 đến 100 ký tự.');
    if ((payload.description || '').trim().length > 500) throw new Error('Mô tả tối đa 500 ký tự.');
    const titles = (payload.subTasks || []).map(s => (typeof s === 'string' ? s : s.title).trim());
    if (titles.length > 100 || titles.some(t => !t || t.length > 200) || new Set(titles.map(t => t.toLowerCase())).size !== titles.length)
      throw new Error('Checklist phải có nội dung, tối đa 200 ký tự và không trùng nhau.');
    let dueDate = null;
    if (payload.dueDate) {
      const d = new Date(payload.dueDate);
      if (!isNaN(d.getTime())) dueDate = d.toISOString();
    }
    const input = { title, description: payload.description?.trim() || '', priority: payload.priority || 'MEDIUM',
      dueDate, projectId: payload.projectId || null,
      subTasks: titles };
    state.loading = true;
    try {
      const created = state.localMode ? { ...input, id: crypto.randomUUID(), status: 'TODO',
        subTasks: titles.map(title => ({ id: crypto.randomUUID(), title, completed: false })) }
        : await apiService.createTask(input);
      const task = normalizeTask(created); state.tasks.unshift(task); state.selectedTask = task; return task;
    } finally { state.loading = false; }
  }
  async function transition(id, target, fn) {
    const task = getTask(id);
    const allowed = target === 'IN_PROGRESS' ? task.status === 'TODO'
      : target === 'DONE' ? task.status === 'IN_PROGRESS' : ['TODO', 'IN_PROGRESS'].includes(task.status);
    if (!allowed) throw new Error('Chuyển trạng thái không hợp lệ.');
    updateTaskInState(id, state.localMode ? { status: target } : await fn(id));
  }
  const startTask = id => transition(id, 'IN_PROGRESS', apiService.startTask);
  const completeTask = id => transition(id, 'DONE', apiService.completeTask);
  const cancelTask = id => transition(id, 'CANCELLED', apiService.cancelTask);
  async function refreshPriority(id) {
    if (!state.localMode) updateTaskInState(id, await apiService.refreshPriority(id));
  }
  async function toggleSubTask(id, subId) {
    const task = getTask(id); assertMutable(task);
    const sub = task.subTasks.find(s => s.id === subId);
    if (!sub) throw new Error('Không tìm thấy checklist.');
    if (state.localMode) { sub.completed = !sub.completed; refreshSelection(); }
    else updateTaskInState(id, await apiService.setSubTaskCompleted(id, subId, !sub.completed));
  }
  async function addSubTasks(id, titles) {
    const task = getTask(id); assertMutable(task);
    const seen = new Set(task.subTasks.map(s => s.title.toLowerCase()));
    const unique = titles.map(t => t.trim()).filter(t => t && !seen.has(t.toLowerCase()) && seen.add(t.toLowerCase()));
    if (!unique.length) return;
    if (state.localMode) {
      task.subTasks.push(...unique.map(title => ({ id: crypto.randomUUID(), title, completed: false }))); refreshSelection();
    } else {
      await apiService.applyDecomposition(id, unique);
      updateTaskInState(id, await apiService.getTaskById(id));
    }
  }
  async function applyRecommendations(id, updates) {
    assertMutable(getTask(id));
    if (!state.localMode) {
      if (updates.dueDate) await apiService.applyDeadline(id, updates.dueDate);
      if (updates.priority) await apiService.applyPriority(id, updates.priority);
      updateTaskInState(id, await apiService.getTaskById(id));
    }
    updateTaskInState(id, updates);
  }
  function selectTask(task) { state.selectedTask = task ? getTask(task.id) : null; }
  function setHoveredTask(task) { state.hoveredTask = task; }
  function setFilters({ status, priority, search, deadline }) {
    if (status !== undefined) state.statusFilter = status;
    if (priority !== undefined) state.priorityFilter = priority;
    if (search !== undefined) state.searchQuery = search;
    if (deadline !== undefined) state.deadlineFilter = deadline;
  }
  async function setLocalMode(enabled) {
    // Separate local workspace and server cache, so a server refresh cannot erase local work.
    try {
      localStorage.setItem(`task-management:workspace:${state.localMode ? 'local' : 'server'}`, JSON.stringify(state.tasks));
      const next = JSON.parse(localStorage.getItem(`task-management:workspace:${enabled ? 'local' : 'server'}`) || '[]');
      state.tasks = Array.isArray(next) ? next.map(normalizeTask) : [];
    } catch { state.storageError = 'Không thể đổi workspace vì bộ nhớ trình duyệt không khả dụng.'; return; }
    state.localMode = enabled; state.selectedTask = null; state.hoveredTask = null; state.error = null;
    if (!enabled) await fetchTasks();
  }
  return { state, filteredTasks, kanbanColumns, taskStats, fetchTasks, createTask, startTask, completeTask,
    cancelTask, refreshPriority, toggleSubTask, addSubTasks, applyRecommendations, updateTaskInState,
    selectTask, setHoveredTask, setFilters, setLocalMode };
});
// Preserve the component API while exposing computed values as refs when destructured.
export function useTaskStore() {
  const store = useInternalTaskStore();
  return { ...store, ...storeToRefs(store), state: store.state };
}
