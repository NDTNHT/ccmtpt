import { describe, it, expect, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import TacticalHeader from '@/components/layout/TacticalHeader.vue';
import { useTaskStore } from '@/store/useTaskStore.js';
import { useAiStore } from '@/store/useAiStore.js';

describe('components/TacticalHeader (Feature)', () => {
  beforeEach(async () => {
    localStorage.clear();
    const { fetchTasks } = useTaskStore();
    const { fetchConfig, fetchProviders } = useAiStore();
    await fetchTasks();
    fetchConfig();
    fetchProviders();
  });

  it('renders the tactical header branding and task counters', () => {
    const wrapper = mount(TacticalHeader);

    expect(wrapper.find('.system-title').text()).toBe('TASK MANAGEMENT SYSTEM');
    expect(wrapper.find('.status-badge-group').text()).toContain('TASKS:');
    expect(wrapper.find('.status-badge-group').text()).toContain('IN PROGRESS:');
    expect(wrapper.find('.status-badge-group').text()).toContain('DONE:');
  });

  it('displays the active AI model badge', () => {
    const wrapper = mount(TacticalHeader);

    const modelBadge = wrapper.find('.engine-indicator-badge');
    expect(modelBadge.exists()).toBe(true);
    expect(modelBadge.find('.indicator-prefix').text()).toBe('[MODEL]');
    expect(modelBadge.find('.indicator-val').text()).toBeTruthy();
  });
});
