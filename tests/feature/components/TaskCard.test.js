import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import TaskCard from '@/components/tasks/TaskCard.vue';

describe('components/TaskCard (Feature)', () => {
  const sampleTask = {
    id: 'task-test-01',
    title: 'MISSION PROTOCOL VERIFICATION',
    description: 'Ensure all subsystems are green.',
    status: 'IN_PROGRESS',
    priority: 'HIGH',
    dueDate: '2026-12-31',
    projectId: 'proj-omega-01',
    subTasks: [
      { id: 'st-1', title: 'Subtask 1', completed: true },
      { id: 'st-2', title: 'Subtask 2', completed: false }
    ]
  };

  it('renders task metadata, title, and badges correctly', () => {
    const wrapper = mount(TaskCard, {
      props: {
        task: sampleTask,
        isSelected: false
      }
    });

    expect(wrapper.find('.task-id').text()).toBe('task-test-01');
    expect(wrapper.find('.task-title').text()).toBe('MISSION PROTOCOL VERIFICATION');
    expect(wrapper.find('.badge-cluster').text()).toContain('[ACTIVE]');
    expect(wrapper.find('.badge-cluster').text()).toContain('HIGH');
  });

  it('displays subtasks progress count and percentage accurately', () => {
    const wrapper = mount(TaskCard, {
      props: {
        task: sampleTask
      }
    });

    expect(wrapper.find('.checklist-header').text()).toContain('SUBTASKS // 1/2');
    expect(wrapper.find('.checklist-header').text()).toContain('50%');
    expect(wrapper.find('.progress-fill').attributes('style')).toContain('width: 50%');
  });

  it('emits lifecycle transition events on button clicks', async () => {
    const wrapper = mount(TaskCard, {
      props: {
        task: sampleTask
      }
    });

    // Complete button (visible when status is IN_PROGRESS)
    const completeBtn = wrapper.find('button[title="Complete Task"]');
    expect(completeBtn.exists()).toBe(true);
    await completeBtn.trigger('click');
    expect(wrapper.emitted('complete')?.[0]).toEqual(['task-test-01']);

    // Cancel button
    const cancelBtn = wrapper.find('button[title="Cancel Task"]');
    expect(cancelBtn.exists()).toBe(true);
    await cancelBtn.trigger('click');
    expect(wrapper.emitted('cancel')?.[0]).toEqual(['task-test-01']);

    // AI Inspect button
    const aiInspectBtn = wrapper.find('.ai-inspect-btn');
    expect(aiInspectBtn.exists()).toBe(true);
    await aiInspectBtn.trigger('click');
    expect(wrapper.emitted('inspect')?.[0]).toEqual([sampleTask]);
  });

  it('emits select and hover events', async () => {
    const wrapper = mount(TaskCard, {
      props: {
        task: sampleTask
      }
    });

    await wrapper.trigger('click');
    expect(wrapper.emitted('select')?.[0]).toEqual([sampleTask]);

    await wrapper.trigger('mouseover');
    expect(wrapper.emitted('hover')?.[0]).toEqual([sampleTask]);
  });
});
