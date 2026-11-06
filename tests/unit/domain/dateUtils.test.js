import { describe, it, expect } from 'vitest';
import { getFormattedDate, getCurrentUtcTime } from '@/domain/dateUtils.js';

describe('domain/dateUtils', () => {
  describe('getFormattedDate', () => {
    it('returns current date in YYYY-MM-DD format by default', () => {
      const result = getFormattedDate();
      expect(result).toMatch(/^\d{4}-\d{2}-\d{2}$/);
      const expected = new Date().toISOString().slice(0, 10);
      expect(result).toBe(expected);
    });

    it('calculates positive offset correctly', () => {
      const result = getFormattedDate(3);
      const expected = new Date(Date.now() + 3 * 86400000).toISOString().slice(0, 10);
      expect(result).toBe(expected);
    });

    it('calculates negative offset correctly', () => {
      const result = getFormattedDate(-1);
      const expected = new Date(Date.now() - 86400000).toISOString().slice(0, 10);
      expect(result).toBe(expected);
    });
  });

  describe('getCurrentUtcTime', () => {
    it('returns a formatted time string ending with UTC', () => {
      const result = getCurrentUtcTime();
      expect(result).toMatch(/^\d{2}:\d{2}:\d{2} UTC$/);
    });
  });
});
