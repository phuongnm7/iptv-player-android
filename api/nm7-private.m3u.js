import fs from 'node:fs/promises';
import path from 'node:path';
import { gunzipSync } from 'node:zlib';

const PARTS = [
  'nm7-private.part1.b64',
  'nm7-private.part2.b64',
  'nm7-private.part3.b64',
  'nm7-private.part4.b64'
];

async function loadPlaylistFromBundle() {
  const chunks = await Promise.all(
    PARTS.map((name) => fs.readFile(path.join(process.cwd(), 'playlist', name), 'utf8'))
  );
  const compressed = Buffer.from(chunks.map((x) => x.trim()).join(''), 'base64');
  return gunzipSync(compressed).toString('utf8');
}

function normalizeM3U(text) {
  let source = String(text ?? '').replace(/^\uFEFF/, '').replace(/\r/g, '');
  const extinfIndex = source.search(/#EXTINF\s*:/i);
  if (extinfIndex >= 0 && source.slice(0, extinfIndex).indexOf('#EXTM3U') < 0) {
    source = `#EXTM3U\n${source.slice(extinfIndex)}`;
  }

  const start = source.search(/#EXTM3U/i);
  if (start < 0) throw new Error('Playlist does not contain #EXTM3U');

  const blocks = source.slice(start)
    .split(/(?=#EXTINF\s*:)/i)
    .filter((block) => /^\s*#EXTINF\s*:/i.test(block));

  const out = ['#EXTM3U'];
  const seen = new Set();
  let playable = 0;

  for (const block of blocks) {
    const lines = block.split('\n').map((line) => line.trim()).filter(Boolean);
    if (!/^#EXTINF\s*:/i.test(lines[0] || '')) continue;

    // Standard M3U has the stream URI on the next non-comment line.
    // Some NM7 sources place the URI directly after the EXTINF metadata.
    let streamLine = null;
    let streamIndex = -1;

    for (let i = 1; i < lines.length; i++) {
      const line = lines[i];
      if (/^[a-z][a-z0-9+.-]*:\/\//i.test(line)) {
        streamLine = line;
        streamIndex = i;
        break;
      }
      if (!line.startsWith('#') && !line.includes('=') && /\S/.test(line)) {
        streamLine = line;
        streamIndex = i;
        break;
      }
    }

    // Also support '#EXTINF:... ,Name https://stream...' style entries.
    if (!streamLine) {
      const inline = lines[0].match(/\s(https?:\/\/\S+)\s*$/i);
      if (inline) {
        streamLine = inline[1];
        streamIndex = 0;
      }
    }

    if (!streamLine) continue;

    let clean;
    if (streamIndex === 0) {
      clean = `${lines[0]}\n${streamLine}`;
    } else {
      clean = lines.slice(0, streamIndex + 1).join('\n');
    }

    if (seen.has(clean)) continue;
    seen.add(clean);
    out.push(clean);
    playable++;
  }

  if (!playable) {
    const sample = source.slice(0, 180).replace(/\s+/g, ' ').trim();
    throw new Error(`Playlist contains no playable EXTINF entries (sourceBytes=${Buffer.byteLength(source, 'utf8')}, extinfBlocks=${blocks.length}, sample=${sample})`);
  }

  return `${out.join('\n')}\n`;
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
  const match = text.match(/(?:,|\s)(?:🟢|🟡)?\s*(\d{1,2}:\d{2})\s+(\d{1,2})\/(\d{1,2})(?:\s|⚽|🏀|🏐|⚾)/u);
  if (!match) return null;
  const [hour, minute] = match[1].split(':').map(Number);
  const day = Number(match[2]);
  const month = Number(match[3]);
  let year = now.year;
  if (month - now.month > 6) year--;
  if (now.month - month > 6) year++;
  return Date.UTC(year, month - 1, day, hour - 7, minute, 0, 0);
}

function filterFinished(text) {
  const now = vietnamNowParts();
  const nowMs = Date.UTC(now.year, now.month - 1, now.day, now.hour - 7, now.minute);
  const blocks = text.replace(/\r/g, '')
    .split(/(?=#EXTINF\s*:)/i)
    .filter((block) => /^\s*#EXTINF\s*:/i.test(block));

  const kept = [];
  const seen = new Set();
  for (const block of blocks) {
    const clean = block.trim();
    if (!clean || seen.has(clean)) continue;
    seen.add(clean);
    const started = eventTimeVN(clean, now);
    if (started !== null && started + 180 * 60 * 1000 < nowMs) continue;
    kept.push(clean);
  }
  return `#EXTM3U\n${kept.join('\n')}\n`;
}

export default async function handler(req, res) {
  if (req.method === 'OPTIONS') {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
    return res.status(204).end();
  }

  try {
    const source = await loadPlaylistFromBundle();
    const normalized = normalizeM3U(source);
    const playlist = normalizeM3U(filterFinished(normalized));

    res.setHeader('Content-Type', 'audio/x-mpegurl; charset=utf-8');
    res.setHeader('Content-Disposition', 'inline; filename="nm7-private.m3u"');
    res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0');
    res.setHeader('CDN-Cache-Control', 'no-store, max-age=0');
    res.setHeader('Vercel-CDN-Cache-Control', 'no-store, max-age=0');
    res.setHeader('Pragma', 'no-cache');
    res.setHeader('Expires', '0');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
    res.setHeader('X-NM7-Playlist-Format', 'm3u-utf8');
    res.setHeader('X-NM7-Playlist-Bytes', String(Buffer.byteLength(playlist, 'utf8')));
    return res.status(200).send(playlist);
  } catch (error) {
    console.error('NM7 playlist error:', error);
    res.setHeader('Content-Type', 'text/plain; charset=utf-8');
    res.setHeader('Cache-Control', 'no-store, no-cache, max-age=0');
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).send(`#EXTM3U\n# ERROR ${String(error?.message || error)}\n`);
  }
}
