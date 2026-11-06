import { describe, it, expect, beforeEach } from 'vitest';
import { useAiStore } from '@/store/useAiStore.js';

describe('store/useAiStore (Feature)', () => {
  beforeEach(() => {
    localStorage.clear();
    const { fetchConfig, fetchProviders } = useAiStore();
    fetchConfig();
    fetchProviders();
  });

  describe('Provider Activation & Auto-Routing', () => {
    it('activates a provider and configures base endpoint', () => {
      const { state, activeProvider, activateProvider } = useAiStore();

      activateProvider('pollinations');
      expect(state.activeProviderId).toBe('pollinations');
      expect(activeProvider.value.id).toBe('pollinations');
      expect(state.config.baseUrl).toContain('pollinations.ai');
      expect(state.verification.valid).toBe(true);
    });

    it('auto-switches provider when a model from another provider is selected', () => {
      const { state, activateProvider, selectModel } = useAiStore();

      activateProvider('gemini');
      expect(state.activeProviderId).toBe('gemini');

      selectModel('claude-3-5-sonnet-20241022');
      expect(state.activeProviderId).toBe('anthropic');
      expect(state.config.model).toBe('claude-3-5-sonnet-20241022');
    });
  });

  describe('Telemetry Event Logging', () => {
    it('adds telemetry logs and respects max limit of 50 events', () => {
      const { state, addLog } = useAiStore();

      for (let i = 0; i < 60; i++) {
        addLog('EVENT', `Message ${i}`, 'INFO');
      }

      expect(state.telemetryLogs.length).toBeLessThanOrEqual(50);
      expect(state.telemetryLogs[0].message).toBe('Message 59');
    });
  });

  describe('Custom System Prompt Management', () => {
    it('updates and resets system prompt directive', () => {
      const { state, setCustomPrompt, resetCustomPrompt } = useAiStore();

      setCustomPrompt('Custom directive buffer for unit testing.');
      expect(state.customSystemPrompt).toBe('Custom directive buffer for unit testing.');

      resetCustomPrompt();
      expect(state.customSystemPrompt).toContain('Tactical AI Task Management Assistant');
    });
  });
});
