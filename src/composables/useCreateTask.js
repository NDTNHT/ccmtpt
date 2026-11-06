import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue';
import { useTaskStore } from '../store/useTaskStore.js';
import { taskService } from '../services/taskService.js';
import { aiService } from '../services/aiService.js';
import { getFormattedDate } from '../domain/dateUtils.js';
import {
  TASK_PRIORITY_OPTIONS,
  validateTaskTitle,
  isTaskTitleValid
} from '../domain/taskRules.js';

export function useCreateTask({ props, emit }) {
  const { createTask } = useTaskStore();

  const titleInputRef = ref(null);
  const isSubmitting = ref(false);
  const newSubTaskTitle = ref('');
  const projectList = ref([]);
  const showNewProjectForm = ref(false);
  const newProjectName = ref('');
  const newProjectLoading = ref(false);
  const aiDraftLoading = ref(false);

  const form = reactive({
    title: '',
    description: '',
    priority: 'HIGH',
    dueDate: getFormattedDate(3),
    projectId: null,
    subTasks: []
  });

  const inboxProject = computed(() => {
    return projectList.value.find((p) => p.type === 'INBOX') || null;
  });

  const regularProjects = computed(() => {
    return projectList.value.filter((p) => p.type === 'REGULAR');
  });

  const selectedProject = computed(() => {
    return projectList.value.find((p) => p.id === form.projectId) || null;
  });

  const selectedProjectType = computed(() => {
    return selectedProject.value ? selectedProject.value.type : 'INBOX';
  });

  const priorityOptions = TASK_PRIORITY_OPTIONS;

  const isFormValid = computed(() => {
    return isTaskTitleValid(form.title);
  });

  const titleError = computed(() => {
    return validateTaskTitle(form.title);
  });

  function applyDatePreset(days) {
    form.dueDate = getFormattedDate(days);
  }

  async function loadProjects() {
    try {
      const data = taskService.getProjects();
      if (Array.isArray(data)) {
        projectList.value = data;
        // Default to the first REGULAR project if available, or first available project
        if (!form.projectId || !data.some((p) => p.id === form.projectId)) {
          const firstRegular = data.find((p) => p.type === 'REGULAR');
          form.projectId = firstRegular ? firstRegular.id : data[0] ? data[0].id : null;
        }
      }
    } catch (err) {
      console.warn('Projects endpoint unreachable, using default inbox:', err.message);
    }
  }

  async function handleCreateRegularProject() {
    const name = newProjectName.value.trim();
    if (!name) return;
    newProjectLoading.value = true;
    try {
      const created = taskService.createProject({
        name: name.toUpperCase(),
        description: 'Tactical regular mission sector'
      });
      await loadProjects();
      form.projectId = created.id;
      showNewProjectForm.value = false;
      newProjectName.value = '';
      emit('notify', `Regular Sector "${created.name}" registered and assigned.`, 'success');
    } catch (err) {
      emit('notify', `Failed to create regular project: ${err.message}`, 'error');
    } finally {
      newProjectLoading.value = false;
    }
  }

  function handleAddSubTask() {
    const title = newSubTaskTitle.value.trim();
    if (!title) return;
    form.subTasks.push({
      title,
      completed: false
    });
    newSubTaskTitle.value = '';
  }

  function removeSubTask(index) {
    form.subTasks.splice(index, 1);
  }

  async function generateSmartCheckpoints() {
    const title = form.title.trim();
    if (!title) return;

    aiDraftLoading.value = true;
    try {
      const res = await aiService.decomposeTask({
        id: 'draft',
        title,
        description: form.description.trim(),
        status: 'TODO',
        priority: form.priority || 'MEDIUM'
      });

      const suggestions = res && res.subTasks ? res.subTasks : [];
      if (suggestions.length === 0) {
        emit('notify', 'AI returned no subtask suggestions for this title.', 'info');
        return;
      }

      suggestions.forEach((st) => {
        if (!form.subTasks.some((existing) => existing.title === st)) {
          form.subTasks.push({ title: st, completed: false });
        }
      });

      emit('notify', `AI added ${suggestions.length} subtasks!`, 'success');
    } catch (err) {
      console.error('AI draft subtasks failed:', err);
      emit('notify', `AI Error: ${err.message}`, 'error');
    } finally {
      aiDraftLoading.value = false;
    }
  }

  function resetForm() {
    form.title = '';
    form.description = '';
    form.priority = 'HIGH';
    form.dueDate = getFormattedDate(3);
    form.subTasks = [];
    newSubTaskTitle.value = '';
  }

  function handleClose() {
    if (isSubmitting.value) return;
    emit('close');
  }

  async function handleSubmit() {
    if (!isFormValid.value || isSubmitting.value) return;

    isSubmitting.value = true;
    try {
      const payload = {
        title: form.title.trim().toUpperCase(),
        description: form.description.trim(),
        priority: form.priority,
        dueDate: form.dueDate,
        projectId: form.projectId,
        subTasks: form.subTasks
      };

      const created = await createTask(payload);
      emit('notify', `Task "${created.title}" created successfully.`, 'success');
      emit('created', created);
      resetForm();
      emit('close');
    } catch (err) {
      console.error('Failed to create task:', err);
      emit('notify', `Failed to create task: ${err.message || 'Unknown network error'}`, 'error');
    } finally {
      isSubmitting.value = false;
    }
  }

  onMounted(() => {
    loadProjects();
  });

  watch(
    () => props.isOpen,
    (open) => {
      if (open) {
        loadProjects();
        nextTick(() => {
          if (titleInputRef.value) {
            titleInputRef.value.focus();
          }
        });
      }
    }
  );

  return {
    form,
    titleInputRef,
    isSubmitting,
    newSubTaskTitle,
    projectList,
    showNewProjectForm,
    newProjectName,
    newProjectLoading,
    aiDraftLoading,
    inboxProject,
    regularProjects,
    selectedProject,
    selectedProjectType,
    priorityOptions,
    isFormValid,
    titleError,
    applyDatePreset,
    loadProjects,
    handleCreateRegularProject,
    handleAddSubTask,
    removeSubTask,
    generateSmartCheckpoints,
    resetForm,
    handleClose,
    handleSubmit
  };
}

