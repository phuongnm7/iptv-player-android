import legacyHandler from './nm7-private.m3u.js';
import { overlayCurrentMatches } from './nm7-schedule-overlay.mjs';

export default async function handler(req, res) {
  let status = 200;
  const headers = {};
  let body = '';
  const capture = {
    setHeader(name, value) { headers[name] = value; },
    status(code) { status = code; return this; },
    send(value) { body = String(value ?? ''); return this; },
    end(value) { if (value !== undefined) body = String(value); return this; },
    json(value) { body = JSON.stringify(value); headers['Content-Type'] = 'application/json; charset=utf-8'; return this; }
  };

  await legacyHandler(req, capture);
  if (status !== 200 || !body.startsWith('#EXTM3U')) {
    for (const [k, v] of Object.entries(headers)) res.setHeader(k, v);
    return res.status(status).send(body);
  }

  try {
    body = await overlayCurrentMatches(body);
  } catch (error) {
    console.warn('NM7 multi-source overlay unavailable; serving legacy playlist:', error?.message || error);
  }

  for (const [k, v] of Object.entries(headers)) res.setHeader(k, v);
  res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0');
  res.setHeader('CDN-Cache-Control', 'no-store, max-age=0');
  res.setHeader('Vercel-CDN-Cache-Control', 'no-store, max-age=0');
  res.setHeader('X-NM7-Multi-Source-Overlay', 'enabled');
  return res.status(200).send(body);
}
