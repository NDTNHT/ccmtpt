import { describe, it, expect } from 'vitest';
import {
  TASK_PRIORITIES,
  TASK_STATUS_OPTIONS,
  TASK_PRIORITY_OPTIONS,
  validateTaskTitle,
  isTaskTitleValid,
  getStatusColorClass,
  calculatePriorityFromDueDate
} from '@/domain/taskRules.js';

describe('domain/taskRules', () => {
  describe('Constants', () => {
    it('defines expected task priorities', () => {
      expect(TASK_PRIORITIES).toEqual(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']);
      expect(TASK_PRIORITY_OPTIONS).toHaveLength(4);
    });

    it('defines expected task status options', () => {
      expect(TASK_STATUS_OPTIONS).toEqual(['ALL', 'TODO', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']);
    });
  });

  describe('validateTaskTitle', () => {
    it('returns null for empty or whitespace-only title (unfilled state)', () => {
      expect(validateTaskTitle('')).toBeNull();
      expect(validateTaskTitle('   ')).toBeNull();
      expect(validateTaskTitle(null)).toBeNull();
    });

    it('returns error when title has fewer than 3 characters', () => {
      expect(validateTaskTitle('ab')).toBe('Title must contain at least 3 characters.');
      expect(validateTaskTitle(' a ')).toBe('Title must contain at least 3 characters.');
    });

    it('returns error when title exceeds 100 characters', () => {
      const longTitle = 'a'.repeat(101);
      expect(validateTaskTitle(longTitle)).toBe('Title cannot exceed 100 characters.');
    });

    it('returns null for valid titles between 3 and 100 characters', () => {
      expect(validateTaskTitle('Fix auth issue')).toBeNull();
      expect(validateTaskTitle('a'.repeat(100))).toBeNull();
    });
  });

  describe('isTaskTitleValid', () => {
    it('validates title length correctly', () => {
      expect(isTaskTitleValid('')).toBe(false);
      expect(isTaskTitleValid('ab')).toBe(false);
      expect(isTaskTitleValid('abc')).toBe(true);
      expect(isTaskTitleValid('Valid Task Title')).toBe(true);
      expect(isTaskTitleValid('a'.repeat(101))).toBe(false);
    });
  });

  describe('getStatusColorClass', () => {
    it('maps statuses to the appropriate brutalist color utility classes', () => {
      expect(getStatusColorClass('IN_PROGRESS')).toBe('text-amber');
      expect(getStatusColorClass('COMPLETED')).toBe('text-green');
      expect(getStatusColorClass('CANCELLED')).toBe('text-dim');
      expect(getStatusColorClass('TODO')).toBe('text-main');
      expect(getStatusColorClass('UNKNOWN')).toBe('text-main');
    });
  });

  describe('calculatePriorityFromDueDate', () => {
    it('returns MEDIUM when dueDate is missing', () => {
      expect(calculatePriorityFromDueDate(null)).toBe('MEDIUM');
      expect(calculatePriorityFromDueDate('')).toBe('MEDIUM');
    });

    it('assigns CRITICAL when due in less than 1 day or overdue', () => {
      const pastDate = new Date(Date.now() - 3600000).toISOString();
      const imminentDate = new Date(Date.now() + 12 * 3600000).toISOString();
      expect(calculatePriorityFromDueDate(pastDate)).toBe('CRITICAL');
      expect(calculatePriorityFromDueDate(imminentDate)).toBe('CRITICAL');
    });

    it('assigns HIGH when due in 1 to 2.9 days', () => {
      const twoDays = new Date(Date.now() + 2 * 86400000).toISOString();
      expect(calculatePriorityFromDueDate(twoDays)).toBe('HIGH');
    });

    it('assigns MEDIUM when due in 3 to 6.9 days', () => {
      const fiveDays = new Date(Date.now() + 5 * 86400000).toISOString();
      expect(calculatePriorityFromDueDate(fiveDays)).toBe('MEDIUM');
    });

    it('assigns LOW when due in 7 or more days', () => {
      const tenDays = new Date(Date.now() + 10 * 86400000).toISOString();
      expect(calculatePriorityFromDueDate(tenDays)).toBe('LOW');
    });
  });
});
