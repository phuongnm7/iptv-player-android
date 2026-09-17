import fs from 'node:fs/promises';
import path from 'node:path';
import { gunzipSync } from 'node:zlib';

const PARTS = [
  'nm7-private.part1.b64',
  'nm7-private.part2.b64',
  'nm7-private.part3.b64',
  'nm7-private.part4.b64'
];

async function loadPlaylist() {
  const chunks = await Promise.all(
    PARTS.map((name) => fs.readFile(path.join(process.cwd(), 'playlist', name), 'utf8'))
  );
  const compressed = Buffer.from(chunks.join('').trim(), 'base64');
  return gunzipSync(compressed).toString('utf8');
}

function vietnamNowParts() {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Asia/Ho_Chi_Minh',
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hourCycle: 'h23'
  }).formatToParts(new Date());
  const get = (type) => Number(parts.find((p) => p.type === type)?.value);
  return { year: get('year'), month: get('month'), day: get('day'), hour: get('hour'), minute: get('minute') };
}

function eventTimeVN(text, now) {
  const m = text.match(/(?:,|\s)(?:🟢|🟡)?\s*(\d{1,2}:\d{2})\s+(\d{1,2})\/(\d{1,2})(?:\s|⚽|🏀|🏐|⚾)/u);
  if (!m) return null;
  const [hour, minute] = m[1].split(':').map(Number);
  const day = Number(m[2]);
  const month = Number(m[3]);
  let year = now.year;
  if (month - now.month > 6) year--;
  if (now.month - month > 6) year++;
  // Vietnam is UTC+7; construct the event as an absolute timestamp.
  return Date.UTC(year, month - 1, day, hour - 7, minute, 0, 0);
}

function filterFinished(text) {
  const now = vietnamNowParts();
  const nowMs = Date.UTC(now.year, now.month - 1, now.day, now.hour - 7, now.minute);
  const keepAfterMinutes = 180; // 3 hours after scheduled start

  const blocks = text
    .replace(/\r/g, '')
    .split(/(?=#EXTINF:)/)
    .filter((b) => b.trim().startsWith('#EXTINF:'));

  const seen = new Set();
  const kept = [];

  for (const block of blocks) {
    const clean = block.trim();
    if (!clean || seen.has(clean)) continue;
    seen.add(clean);

    const started = eventTimeVN(clean, now);
    if (started !== null && started + keepAfterMinutes * 60 * 1000 < nowMs) {
      continue;
    }
    kept.push(clean);
  }

  return '#EXTM3U\n' + kept.join('\n') + '\n';
}

export default async function handler(req, res) {
  try {
    const source = await loadPlaylist();
    const playlist = filterFinished(source);

    res.setHeader('Content-Type', 'audio/x-mpegurl; charset=utf-8');
    res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0');
    res.setHeader('CDN-Cache-Control', 'no-store, max-age=0');
    res.setHeader('Vercel-CDN-Cache-Control', 'no-store, max-age=0');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
    return res.status(200).send(playlist);
  } catch (error) {
    res.setHeader('Content-Type', 'text/plain; charset=utf-8');
    res.setHeader('Cache-Control', 'no-store, no-cache, max-age=0');
    return res.status(500).send(`#EXTM3U\n# ERROR ${String(error?.message || error)}`);
  }
}
