import { beforeEach, afterEach, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia, disposePinia } from 'pinia';
import { useAiStore } from './useAiStore.js';
import { apiService } from '../services/apiService.js';
vi.mock('../services/apiService.js', () => ({ apiService: {
  verifyProvider: vi.fn(), registerProvider: vi.fn(), activateProvider: vi.fn(), updateAiConfig: vi.fn()
} }));
let pinia;
beforeEach(() => {
  vi.stubGlobal('localStorage', { getItem: () => null, setItem: vi.fn() });
  pinia = createPinia(); setActivePinia(pinia); vi.resetAllMocks();
});
afterEach(() => { disposePinia(pinia); vi.unstubAllGlobals(); });
it('verification saves credentials and selected model into the provider used for AI dispatch', async () => {
  apiService.verifyProvider.mockResolvedValue({ valid: true, availableModels: ['model-1'], message: 'OK' });
  const s = useAiStore();
  await s.verifyProvider('https://example.test/v1', 'test-key', 'OPENAI');
  expect(apiService.registerProvider).toHaveBeenCalledWith(expect.objectContaining({
    id: 'pollinations', apiKey: 'test-key', defaultModel: 'model-1', baseUrl: 'https://example.test/v1'
  }));
  expect(apiService.activateProvider).toHaveBeenCalledWith('pollinations');
  expect(s.state.verification.valid).toBe(true);
});
it('does not show a provider switch as successful when the server rejects it', async () => {
  apiService.activateProvider.mockRejectedValue(new Error('offline'));
  const s = useAiStore();
  expect(await s.activateProvider('gemini')).toBe(false);
  expect(s.state.activeProviderId).toBe('pollinations');
});
it('selecting a model does not clear the configured backend API key', async () => {
  const s = useAiStore();
  expect(await s.selectModel('model-2')).toBe(true);
  expect(apiService.updateAiConfig).toHaveBeenCalledWith({ model: 'model-2' });
  expect(apiService.updateAiConfig.mock.calls[0][0]).not.toHaveProperty('apiKey');
});
