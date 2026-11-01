import { test, expect } from '@playwright/test';
test('local Kanban persists checklist and AI recommendations across reload', async ({ page }) => {
  await page.addInitScript(() => {
    if (!localStorage.getItem('task-management:v1')) localStorage.setItem('task-management:v1', JSON.stringify({ version: 1, localMode: true, tasks: [] }));
  });
  await page.goto('/');
  await page.getByText('QUICK ADD TASK', { exact: true }).click();
  await page.getByPlaceholder('Task title (e.g. Set up payment gateway)...').fill('Viết báo cáo Vue và Pinia');
  await page.getByRole('button', { name: '[CREATE TASK]', exact: true }).click();
  const todo = page.locator('.kanban-column').filter({ has: page.getByRole('heading', { name: 'To Do' }) });
  await expect(todo.locator('.tactical-task-card')).toHaveCount(1);
  await todo.locator('.tactical-task-card').dblclick();
  // Open via the persisted task's dynamic route.
  const id = await page.evaluate(() => JSON.parse(localStorage.getItem('task-management:v1')).tasks[0].id);
  await page.goto(`/#/tasks/${id}`);
  await expect(page.locator('.tactical-modal-window')).toBeVisible();
  await page.getByRole('button', { name: /BREAK DOWN SUBTASKS/ }).click();
  await page.getByRole('button', { name: /APPLY/ }).click();
  await expect(page.locator('.subtask-row')).toHaveCount(4);
  await page.getByRole('dialog').getByRole('checkbox').first().click();
  await page.getByRole('button', { name: /SUMMARIZE TASK/ }).click();
  await expect(page.locator('.ai-banner-text')).toContainText('25%');
  await page.getByRole('button', { name: /ESTIMATE DUE DATE/ }).click();
  await expect(page.locator('.ai-banner-text')).toContainText('12h');
  await page.getByRole('button', { name: /APPLY TO TASK/ }).click();
  await page.reload();
  await expect(page.locator('.subtask-row')).toHaveCount(4);
  await expect(page.locator('.row-completed')).toHaveCount(1);
  await page.getByRole('button', { name: /CLOSE/ }).click();
  await page.getByRole('button', { name: /START/ }).first().click();
  await expect(page.locator('.kanban-column').nth(1).locator('.tactical-task-card')).toHaveCount(1);
  await page.getByRole('button', { name: /COMPLETE/ }).first().click();
  await expect(page.locator('.kanban-column').nth(2).locator('.tactical-task-card')).toHaveCount(1);
  await page.reload();
  await expect(page.locator('.kanban-column').nth(2).locator('.tactical-task-card')).toHaveCount(1);
});

test('Kanban, AI panel and mobile layout provide report evidence', async ({ page }, testInfo) => {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.addInitScript(() => {
    const tasks = [
      { id: 'demo-1', title: 'Thiết kế bảng Kanban', description: 'Phác thảo giao diện; Xây dựng component; Kiểm thử responsive', status: 'TODO', priority: 'HIGH', dueDate: new Date(Date.now() + 86400000).toISOString(), subTasks: [] },
      { id: 'demo-2', title: 'Tích hợp Pinia và localStorage', description: 'Lưu state và khôi phục sau tải lại.', status: 'IN_PROGRESS', priority: 'MEDIUM', dueDate: new Date(Date.now() + 3 * 86400000).toISOString(), subTasks: [{ id: 'step1', title: 'Tạo store', completed: true }, { id: 'step2', title: 'Kiểm thử khôi phục', completed: false }] },
      { id: 'demo-3', title: 'Phân tích yêu cầu đề tài', description: 'Đã hoàn thành tài liệu yêu cầu.', status: 'DONE', priority: 'LOW', dueDate: null, subTasks: [] }
    ];
    localStorage.setItem('task-management:v1', JSON.stringify({ version: 1, localMode: true, tasks }));
  });
  await page.goto('/');
  await expect(page.locator('.tactical-task-card')).toHaveCount(3);
  const capture = name => process.env.CAPTURE_SCREENSHOTS
    ? `../docs/screenshots/${name}.png` : testInfo.outputPath(`${name}.png`);
  await page.screenshot({ path: capture('kanban-desktop'), fullPage: true });
  await page.getByRole('button', { name: /AI COPILOT & SETTINGS/ }).click();
  await page.locator('.workbench-body select').selectOption('demo-2');
  await page.getByRole('button', { name: /FULL/ }).first().click();
  await expect(page.locator('.full-analysis-summary')).toContainText('50%');
  await page.screenshot({ path: capture('ai-progress'), fullPage: true });
  if (await page.locator('.toast-close').count()) await page.locator('.toast-close').first().click();
  await page.goto('/#/tasks');
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.locator('.kanban-column').first()).toBeVisible();
  await page.screenshot({ path: capture('kanban-mobile'), fullPage: true });
  expect(errors).toEqual([]);
});
