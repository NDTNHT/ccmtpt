import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import handler from '../../../api/[...path].mjs';
let res;
beforeEach(() => {
  res = { setHeader: vi.fn(), status: vi.fn().mockReturnThis(), json: vi.fn().mockReturnThis(), send: vi.fn().mockReturnThis() };
  vi.stubGlobal('fetch', vi.fn());
});
afterEach(() => { vi.unstubAllGlobals(); vi.unstubAllEnvs(); });
it('returns a clear configuration error when no backend is deployed', async () => {
  vi.stubEnv('TASK_API_ORIGIN', '');
  await handler({ url: '/api/tasks', headers: {}, method: 'GET' }, res);
  expect(res.status).toHaveBeenCalledWith(503);
  expect(fetch).not.toHaveBeenCalled();
});
it('preserves request, auth header and backend error status without forwarding browser cookies', async () => {
  vi.stubEnv('TASK_API_ORIGIN', 'https://backend.example.test');
  fetch.mockResolvedValue(new Response('{"error":{"message":"Invalid transition"}}', { status: 409, headers: { 'Content-Type': 'application/json' } }));
  await handler({ url: '/api/tasks/id/start?source=web', headers: { 'content-type': 'application/json', 'x-api-key': 'test-app-key', cookie: 'private=session' }, method: 'POST', body: { title: 'Test' } }, res);
  const [url, options] = fetch.mock.calls[0];
  expect(url.href).toBe('https://backend.example.test/api/tasks/id/start?source=web');
  expect(options.body).toBe('{"title":"Test"}');
  expect(options.headers['X-API-Key']).toBe('test-app-key');
  expect(options.headers).not.toHaveProperty('cookie');
  expect(options.redirect).toBe('manual');
  expect(res.status).toHaveBeenCalledWith(409);
});
it('rejects credential-bearing backend URLs without leaking their values', async () => {
  vi.stubEnv('TASK_API_ORIGIN', 'https://user:secret@backend.example.test');
  await handler({ url: '/api/tasks', headers: {}, method: 'GET' }, res);
  expect(res.status).toHaveBeenCalledWith(502);
  expect(fetch).not.toHaveBeenCalled();
  expect(JSON.stringify(res.json.mock.calls)).not.toContain('secret');
});
