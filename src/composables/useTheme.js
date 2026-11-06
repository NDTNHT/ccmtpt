import { ref } from 'vue';

const theme = ref('dark');
const storageKey = 'task-ops-theme';

export function initializeTheme() {
  let savedTheme;
  try {
    savedTheme = localStorage.getItem(storageKey);
  } catch {
    // The toggle still works when browser storage is unavailable.
  }
  theme.value = savedTheme === 'light' || savedTheme === 'dark'
    ? savedTheme
    : window.matchMedia?.('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
  document.documentElement.dataset.theme = theme.value;
}

export function useTheme() {
  function toggleTheme() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark';
    document.documentElement.dataset.theme = theme.value;
    try {
      localStorage.setItem(storageKey, theme.value);
    } catch {
      // Keep the selected theme for this session even if persistence is blocked.
    }
  }

  return { theme, toggleTheme };
}
