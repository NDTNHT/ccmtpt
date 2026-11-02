import { test, expect } from '@playwright/test';

test('Task creation validates titles, checklist duplicates and displays in-modal errors', async ({ page }) => {
  await page.goto('/');

  // 1. Quick Add Title Boundary Validation
  const bannerHeader = page.locator('.creation-banner .banner-header');
  await bannerHeader.click();

  const quickTitleInput = page.locator('.create-task-form .title-input');
  await quickTitleInput.waitFor({ state: 'visible' });
  const quickSubmitBtn = page.locator('.create-task-form button[type="submit"]');

  // Less than 3 characters should disable submit button
  await quickTitleInput.fill('AB');
  await expect(quickSubmitBtn).toBeDisabled();

  // Valid title enables submit button
  const quickTitle = `Task Quick ${Date.now()}`;
  await quickTitleInput.fill(quickTitle);
  await expect(quickSubmitBtn).toBeEnabled();
  await quickSubmitBtn.click();

  // Verify task card appears
  const quickCard = page.locator(`.tactical-task-card:has-text("${quickTitle}")`);
  await expect(quickCard).toBeVisible();

  // 2. Full Modal Task Creation with Subtasks and Duplicate Prevention
  const openModalBtn = page.locator('button.primary-dispatch-btn');
  await openModalBtn.click();
  const modal = page.locator('.tactical-modal-window');
  await expect(modal).toBeVisible();

  const titleInput = modal.locator('.title-input');
  const modalTaskTitle = `Modal Task ${Date.now()}`;
  await titleInput.fill(modalTaskTitle);

  // Subtask management
  const subtaskInput = modal.locator('.subtask-input');
  const addSubtaskBtn = modal.locator('button:has-text("[+ ADD SUBTASK]")');

  await subtaskInput.fill('Step 1: Planning');
  await addSubtaskBtn.click();
  await expect(modal.locator('.subtask-item-row')).toHaveCount(1);

  // Attempt duplicate subtask
  await subtaskInput.fill('Step 1: Planning');
  await addSubtaskBtn.click();
  const subtaskError = modal.locator('.subtask-validation-error');
  await expect(subtaskError).toBeVisible();
  await expect(subtaskError).toContainText('already exists');

  // AI suggest fallback
  const aiSuggestBtn = modal.locator('.ai-suggest-btn');
  await aiSuggestBtn.click();
  await expect(modal.locator('.subtask-item-row')).not.toHaveCount(1);

  // Dispatch task creation
  const dispatchBtn = modal.locator('button.tactical-btn-dispatch');
  await dispatchBtn.click();
  await expect(modal).toBeHidden();

  // Verify card exists and title casing is preserved
  const modalTaskCard = page.locator(`.tactical-task-card:has-text("${modalTaskTitle}")`);
  await expect(modalTaskCard).toBeVisible();
  const cardTitle = await modalTaskCard.locator('.task-title').textContent();
  expect(cardTitle.trim()).toBe(modalTaskTitle);

  // 3. Subtask Completion in Detail Modal
  await modalTaskCard.click();
  const openDetailsBtn = page.locator('button:has-text("OPEN FULL DETAILS")');
  await openDetailsBtn.click();
  const detailModal = page.locator('.tactical-modal-window[role="dialog"]');
  await expect(detailModal).toBeVisible();

  const firstSubtaskRow = detailModal.locator('.subtask-row').first();
  await expect(firstSubtaskRow).toContainText('[ ]');
  await firstSubtaskRow.click();
  await expect(firstSubtaskRow).toContainText('[X]');

  // Close detail modal
  await detailModal.locator('.modal-close-btn').click();
  await expect(detailModal).toBeHidden();

  // 4. Duplicate Task in Project Error Banner in Modal
  await openModalBtn.click();
  await expect(modal).toBeVisible();
  await titleInput.fill(modalTaskTitle); // Same title
  await dispatchBtn.click();

  const formError = modal.locator('.modal-form-error');
  await expect(formError).toBeVisible();
  await expect(formError).toContainText('already exists in this project');

  await modal.locator('.modal-close-btn').click();
  await expect(modal).toBeHidden();
});
