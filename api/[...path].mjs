// Vercel runs this proxy; the Java service and its SQLite volume run separately.
export default async function handler(req, res) {
  res.setHeader('Cache-Control', 'no-store');
  const origin = process.env.TASK_API_ORIGIN;
  if (!origin) {
    return res.status(503).json({ error: { message: 'Backend chưa được cấu hình. Đặt TASK_API_ORIGIN trên Vercel hoặc chọn chế độ local.' } });
  }
  try {
    const backend = new URL(origin);
    if (!['http:', 'https:'].includes(backend.protocol) || backend.username || backend.password || backend.search || backend.hash) {
      throw new Error('Invalid backend origin');
    }
    const incoming = new URL(req.url, 'https://frontend.invalid');
    if (!incoming.pathname.startsWith('/api/')) return res.status(404).json({ error: { message: 'Unknown endpoint' } });
    const target = new URL(`${backend.href.replace(/\/$/, '')}${incoming.pathname}${incoming.search}`);
    const headers = { Accept: 'application/json' };
    if (req.headers['content-type']) headers['Content-Type'] = req.headers['content-type'];
    if (req.headers['x-api-key']) headers['X-API-Key'] = req.headers['x-api-key'];
    const method = req.method || 'GET';
    let body;
    if (!['GET', 'HEAD'].includes(method)) {
      if (req.body !== undefined) body = typeof req.body === 'string' || Buffer.isBuffer(req.body) ? req.body : JSON.stringify(req.body);
      else {
        const chunks = [];
        let bytes = 0;
        for await (const chunk of req) {
          bytes += chunk.length;
          if (bytes > 1024 * 1024) return res.status(413).json({ error: { message: 'Request too large' } });
          chunks.push(chunk);
        }
        body = Buffer.concat(chunks);
      }
    }
    const upstream = await fetch(target, { method, headers, body, redirect: 'manual', signal: AbortSignal.timeout(55000) });
    const contentType = upstream.headers.get('content-type');
    if (contentType) res.setHeader('Content-Type', contentType);
    res.status(upstream.status);
    return res.send(Buffer.from(await upstream.arrayBuffer()));
  } catch {
    return res.status(502).json({ error: { message: 'Không kết nối được backend. Kiểm tra TASK_API_ORIGIN và trạng thái máy chủ.' } });
  }
}
