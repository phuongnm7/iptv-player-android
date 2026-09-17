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
  // Vercel bundles playlist/*.b64 with this function. Read the local snapshot
  // first so each request is independent of GitHub/private-repository access.
  const chunks = await Promise.all(
    PARTS.map((name) => fs.readFile(path.join(process.cwd(), 'playlist', name), 'utf8'))
  );
  const encoded = chunks.map((x) => x.trim()).join('');
  const compressed = Buffer.from(encoded, 'base64');
  return gunzipSync(compressed).toString('utf8');
}

function normalizeM3U(text) {
  const source = String(text ?? '')
    .replace(/^\uFEFF/, '')
    .replace(/\r/g, '');

  const start = source.indexOf('#EXTM3U');
  if (start < 0) throw new Error('Playlist does not contain #EXTM3U');

  const blocks = source
    .slice(start)
    .split(/(?=#EXTINF:)/)
    .filter((block) => block.trim().startsWith('#EXTINF:'));

  const out = ['#EXTM3U'];
  const seen = new Set();
  let playable = 0;

  for (const block of blocks) {
    const lines = block
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean);

    if (!lines[0]?.startsWith('#EXTINF:')) continue;

    const streamIndex = lines.findIndex((line, index) =>
      index > 0 && /^(https?|rtsp|rtsps|rtmp|rtmps|udp|rtp|srt):\/\//i.test(line)
    );
    if (streamIndex < 0) continue;

    // Keep EXTINF + metadata directives such as EXTVLCOPT, then the stream URL.
    const clean = lines.slice(0, streamIndex + 1).join('\n');
    if (seen.has(clean)) continue;
    seen.add(clean);
    out.push(clean);
    playable++;
  }

  if (!playable) throw new Error('Playlist contains no playable EXTINF entries');
  return `${out.join('\n')}\n`;
}

function vietnamNowParts() {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Asia/Ho_Chi_Minh',
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hourCycle: 'h23'
  }).formatToParts(new Date());
  const get = (type) => Number(parts.find((p) => p.type === type)?.value);
  return {
    year: get('year'), month: get('month'), day: get('day'),
    hour: get('hour'), minute: get('minute')
  };
}

function eventTimeVN(text, now) {
  const match = text.match(
    /(?:,|\s)(?:🟢|🟡)?\s*(\d{1,2}:\d{2})\s+(\d{1,2})\/(\d{1,2})(?:\s|⚽|🏀|🏐|⚾)/u
  );
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
  const keepAfterMinutes = 180;

  const blocks = text
    .replace(/\r/g, '')
    .split(/(?=#EXTINF:)/)
    .filter((block) => block.trim().startsWith('#EXTINF:'));

  const seen = new Set();
  const kept = [];

  for (const block of blocks) {
    const clean = block.trim();
    if (!clean || seen.has(clean)) continue;
    seen.add(clean);

    const started = eventTimeVN(clean, now);
    if (started !== null && started + keepAfterMinutes * 60 * 1000 < nowMs) continue;
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
    const playlist = normalizeM3U(filterFinished(source));

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
