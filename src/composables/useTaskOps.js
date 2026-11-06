import { ref, computed } from 'vue';
import { useTaskStore } from '../store/useTaskStore.js';
import { getFormattedDate } from '../domain/dateUtils.js';
import { TASK_STATUS_OPTIONS, TASK_PRIORITIES } from '../domain/taskRules.js';

export function useTaskOps({ emit }) {
  const {
    state,
    filteredTasks,
    taskStats,
    createTask,
    startTask,
    completeTask,
    cancelTask,
    selectTask,
    setHoveredTask,
    toggleSubTask,
    setFilters
  } = useTaskStore();

  const statusOptions = TASK_STATUS_OPTIONS;
  const priorityOptions = ['ALL', ...TASK_PRIORITIES];

  const showCreateForm = ref(false);
  const isModalOpen = ref(false);
  const isCreateModalOpen = ref(false);

  const newTask = ref({
    title: '',
    description: '',
    priority: 'HIGH',
    dueDate: getFormattedDate(3),
    projectId: null
  });

  const activePreviewTask = computed(() => {
    return state.hoveredTask || state.selectedTask || filteredTasks.value[0] || null;
  });

  function onSearchInput(e) {
    setFilters({ search: e.target.value });
  }

  function onSelectTask(task) {
    selectTask(task);
  }

  function onHoverTask(task) {
    setHoveredTask(task);
  }

  function openModalForTask(task) {
    selectTask(task);
    isModalOpen.value = true;
  }

  function handleTaskCreated(task) {
    selectTask(task);
  }

  async function handleCreateTask() {
    if (!newTask.value.title.trim()) return;
    try {
      const created = await createTask({ ...newTask.value });
      emit('notify', `Task "${newTask.value.title}" created successfully.`, 'success');
      selectTask(created);
      newTask.value = {
        title: '',
        description: '',
        priority: 'HIGH',
        dueDate: getFormattedDate(3),
        projectId: null
      };
      showCreateForm.value = false;
    } catch (err) {
      emit('notify', `Failed to create task: ${err.message}`, 'error');
    }
  }

  return {
    state,
    filteredTasks,
    taskStats,
    startTask,
    completeTask,
    cancelTask,
    toggleSubTask,
    setFilters,
    statusOptions,
    priorityOptions,
    showCreateForm,
    isModalOpen,
    isCreateModalOpen,
    newTask,
    activePreviewTask,
    onSearchInput,
    onSelectTask,
    onHoverTask,
    openModalForTask,
    handleTaskCreated,
    handleCreateTask
  };
}

