import fs from 'node:fs/promises';
import path from 'node:path';
import { gunzipSync } from 'node:zlib';

const PARTS = ['nm7-private.part1.b64','nm7-private.part2.b64','nm7-private.part3.b64','nm7-private.part4.b64'];

async function load() {
  const chunks = await Promise.all(PARTS.map((name) => fs.readFile(path.join(process.cwd(), 'playlist', name), 'utf8')));
  return gunzipSync(Buffer.from(chunks.map((x) => x.trim()).join(''), 'base64')).toString('utf8');
}

export default async function handler(req, res) {
  try {
    const text = await load();
    const blocks = text.replace(/\r/g, '').split(/(?=#EXTINF\s*:)/i).filter((b) => /^\s*#EXTINF\s*:/i.test(b));
    const entries = [];
    for (const block of blocks) {
      const lines = block.split('\n').map((x) => x.trim()).filter(Boolean);
      const info = lines[0] || '';
      const group = info.match(/group-title="([^"]+)"/i)?.[1] || '';
      const logo = info.match(/tv-logo="([^"]+)"/i)?.[1] || '';
      const title = info.split(',').slice(1).join(',').trim();
      const url = lines.find((x) => /^[a-z][a-z0-9+.-]*:\/\//i.test(x)) || '';
      if (group && url) entries.push({ group, title, logo, url });
    }
    res.setHeader('Content-Type','application/json; charset=utf-8');
    res.setHeader('Cache-Control','no-store');
    return res.status(200).json({ count: entries.length, entries });
  } catch (e) {
    return res.status(500).json({ error: String(e?.message || e) });
  }
}
