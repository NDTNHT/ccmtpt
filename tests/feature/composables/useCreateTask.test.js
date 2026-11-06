import { describe, it, expect, beforeEach, vi } from 'vitest';
import { createApp } from 'vue';
import { useCreateTask } from '@/composables/useCreateTask.js';

function withSetup(composable) {
  let result;
  const app = createApp({
    setup() {
      result = composable();
      return () => {};
    }
  });
  app.mount(document.createElement('div'));
  return { result, app };
}

describe('composables/useCreateTask (Feature)', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('separates INBOX and REGULAR projects properly', async () => {
    const props = { isOpen: true };
    const emit = vi.fn();

    const { result } = withSetup(() => useCreateTask({ props, emit }));
    await result.loadProjects();

    expect(result.inboxProject.value).toBeDefined();
    expect(result.inboxProject.value?.id).toBe('proj-inbox');

    expect(result.regularProjects.value.length).toBeGreaterThan(0);
    expect(result.regularProjects.value.every((p) => p.projectType !== 'INBOX')).toBe(true);
  });

  it('manages subtask list in the form reactive state', () => {
    const props = { isOpen: true };
    const emit = vi.fn();
    const { result } = withSetup(() => useCreateTask({ props, emit }));

    result.newSubTaskTitle.value = 'Step Alpha';
    result.handleAddSubTask();

    expect(result.form.subTasks).toHaveLength(1);
    expect(result.form.subTasks[0].title).toBe('Step Alpha');
    expect(result.newSubTaskTitle.value).toBe('');

    result.removeSubTask(0);
    expect(result.form.subTasks).toHaveLength(0);
  });

  it('applies date presets smoothly', () => {
    const props = { isOpen: true };
    const emit = vi.fn();
    const { result } = withSetup(() => useCreateTask({ props, emit }));

    result.applyDatePreset(7);
    const expected = new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10);
    expect(result.form.dueDate).toBe(expected);
  });

  it('submits valid task and triggers emits', async () => {
    const props = { isOpen: true };
    const emit = vi.fn();
    const { result } = withSetup(() => useCreateTask({ props, emit }));

    result.form.title = 'OPERATIONAL DEPLOYMENT';
    result.form.priority = 'HIGH';

    expect(result.isFormValid.value).toBe(true);

    await result.handleSubmit();

    expect(emit).toHaveBeenCalledWith('notify', expect.stringContaining('created successfully'), 'success');
    expect(emit).toHaveBeenCalledWith('created', expect.objectContaining({ title: 'OPERATIONAL DEPLOYMENT' }));
    expect(emit).toHaveBeenCalledWith('close');
  });

  it('creates a new regular sector project and auto-selects it', async () => {
    const props = { isOpen: true };
    const emit = vi.fn();
    const { result } = withSetup(() => useCreateTask({ props, emit }));

    result.newProjectName.value = 'Tactical Recon Sector';
    await result.handleCreateRegularProject();

    expect(result.form.projectId).toMatch(/^proj-\d+$/);
    expect(result.selectedProjectType.value).toBe('REGULAR');
    expect(emit).toHaveBeenCalledWith('notify', expect.stringContaining('TACTICAL RECON SECTOR'), 'success');
  });
});
