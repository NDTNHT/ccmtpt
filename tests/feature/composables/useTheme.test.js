import { afterEach, describe, expect, it, vi } from 'vitest';
import { initializeTheme, useTheme } from '@/composables/useTheme.js';

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  delete document.documentElement.dataset.theme;
});

describe('theme preference', () => {
  it('uses the system preference when no choice has been saved', () => {
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: true })));
    initializeTheme();
    expect(document.documentElement.dataset.theme).toBe('light');
  });

  it('toggles the page theme and restores the saved choice on initialization', () => {
    localStorage.setItem('task-ops-theme', 'dark');
    initializeTheme();
    const { theme, toggleTheme } = useTheme();
    toggleTheme();
    expect(theme.value).toBe('light');
    expect(document.documentElement.dataset.theme).toBe('light');
    expect(localStorage.getItem('task-ops-theme')).toBe('light');
    document.documentElement.dataset.theme = 'dark';
    initializeTheme();
    expect(document.documentElement.dataset.theme).toBe('light');
    toggleTheme();
    expect(document.documentElement.dataset.theme).toBe('dark');
  });

  it('remains usable when storage is blocked', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('blocked'); });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('blocked'); });
    expect(() => initializeTheme()).not.toThrow();
    const { theme, toggleTheme } = useTheme();
    const previousTheme = theme.value;
    expect(() => toggleTheme()).not.toThrow();
    expect(theme.value).not.toBe(previousTheme);
    expect(document.documentElement.dataset.theme).toBe(theme.value);
  });
});
