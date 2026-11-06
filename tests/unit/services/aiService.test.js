import { describe, it, expect, beforeEach, vi } from 'vitest';
import { aiService } from '@/services/aiService.js';

describe('services/aiService', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  describe('findProviderForModel', () => {
    it('accurately identifies provider based on model naming patterns', () => {
      expect(aiService.findProviderForModel('gemini-2.5-flash')).toBe('gemini');
      expect(aiService.findProviderForModel('gemini-1.5-pro')).toBe('gemini');
      expect(aiService.findProviderForModel('gpt-4o')).toBe('openai');
      expect(aiService.findProviderForModel('gpt-4o-mini')).toBe('openai');
      expect(aiService.findProviderForModel('o1-mini')).toBe('openai');
      expect(aiService.findProviderForModel('claude-3-5-sonnet-20241022')).toBe('anthropic');
      expect(aiService.findProviderForModel('claude-3-5-haiku-20241022')).toBe('anthropic');
      expect(aiService.findProviderForModel('openai-fast')).toBe('pollinations');
      expect(aiService.findProviderForModel('mistral')).toBe('pollinations');
      expect(aiService.findProviderForModel('unknown-model')).toBeNull();
    });
  });

  describe('Configuration & Storage', () => {
    it('returns default configuration when no config is saved', () => {
      const config = aiService.getConfig();
      expect(config.providerId).toBe('gemini');
      expect(config.provider).toBe('GEMINI');
      expect(config.model).toBe('gemini-2.5-flash');
      expect(config.apiKey).toBe('');
    });

    it('saves and retrieves API keys per provider independently', () => {
      aiService.saveKey('gemini', 'gemini-key-123');
      aiService.saveKey('openai', 'openai-key-456');

      expect(aiService.getKey('gemini')).toBe('gemini-key-123');
      expect(aiService.getKey('openai')).toBe('openai-key-456');
      expect(aiService.getKey('anthropic')).toBe('');
    });

    it('saves and merges partial configuration', () => {
      aiService.saveConfig({
        providerId: 'openai',
        model: 'gpt-4o',
        apiKey: 'sk-test-key'
      });

      const config = aiService.getConfig();
      expect(config.providerId).toBe('openai');
      expect(config.model).toBe('gpt-4o');
      expect(config.apiKey).toBe('sk-test-key');
    });

    it('saves and retrieves custom system prompt', () => {
      expect(aiService.getSystemPrompt()).toBeNull();
      aiService.saveSystemPrompt('Strict tactical instructions.');
      expect(aiService.getSystemPrompt()).toBe('Strict tactical instructions.');
    });
  });

  describe('Provider Verification', () => {
    it('verifies Pollinations free gateway without an API key', async () => {
      const result = await aiService.verifyProvider({
        baseUrl: 'https://text.pollinations.ai/openai',
        apiKey: '',
        protocol: 'OPENAI'
      });

      expect(result.valid).toBe(true);
      expect(result.discoveredModels).toEqual(['openai-fast', 'openai', 'mistral']);
    });

    it('verifies Anthropic client handshake directly', async () => {
      const result = await aiService.verifyProvider({
        baseUrl: 'https://api.anthropic.com/v1',
        apiKey: 'sk-ant-test',
        protocol: 'ANTHROPIC'
      });

      expect(result.valid).toBe(true);
      expect(result.discoveredModels.length).toBeGreaterThan(0);
    });

    it('throws error when API key is missing for non-free providers', async () => {
      await expect(
        aiService.verifyProvider({
          baseUrl: 'https://generativelanguage.googleapis.com/v1beta',
          apiKey: '',
          protocol: 'GEMINI'
        })
      ).rejects.toThrow(/API Key is required/);
    });
  });
});
