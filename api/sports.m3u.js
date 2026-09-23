const DEFAULT_UPSTREAM = 'https://raw.githubusercontent.com/phuongnm7/iptv-player-android/main/playlist/sports.m3u';

const SPORTS_GROUPS = new Set([
  'Vua Sân Cỏ TV',
  'Khán Đài TV',
  'Cola TV',
  'Gà Vàng TV',
  'Xôi Lạc Z TV',
  'Gà Vàng 33 TV',
  'S8 TV',
  'Sao Kê TV',
  'Bia Ôm TV',
  'Chuối Chiên TV',
  'Giờ Vàng TV',
  'Socolive TV'
]);

const KEEP_AFTER_START_MINUTES = 180;

function getVietnamNow() {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Asia/Ho_Chi_Minh',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23'
  }).formatToParts(new Date());

  const get = (type) => Number(parts.find((p) => p.type === type)?.value || 0);
  return {
    year: get('year'),
    month: get('month'),
    day: get('day'),
    hour: get('hour'),
    minute: get('minute')
  };
}

function eventStart(extinf, now) {
  const m = String(extinf).match(/\b(\d{1,2}):(\d{2})\s+(\d{1,2})\/(\d{1,2})\b/);
  if (!m) return null;

  const hour = Number(m[1]);
  const minute = Number(m[2]);
  const day = Number(m[3]);
  const month = Number(m[4]);

  if (hour > 23 || minute > 59 || day < 1 || day > 31 || month < 1 || month > 12) {
    return null;
  }

  const nowMs = Date.UTC(now.year, now.month - 1, now.day, now.hour - 7, now.minute);
  const candidates = [-1, 0, 1].map((delta) => {
    const year = now.year + delta;
    return {
      year,
      ms: Date.UTC(year, month - 1, day, hour - 7, minute)
    };
  });

  return candidates.reduce((best, item) =>
    Math.abs(item.ms - nowMs) < Math.abs(best.ms - nowMs) ? item : best
  ).ms;
}

function parseBlocks(text) {
  return String(text || '')
    .replace(/^\uFEFF/, '')
    .replace(/\r/g, '')
    .split(/(?=#EXTINF\s*:)/i)
    .filter((x) => /^\s*#EXTINF\s*:/i.test(x));
}

function groupOf(block) {
  return block.match(/group-title="([^"]+)"/i)?.[1]?.trim() || '';
}

function playableUrl(block) {
  const lines = block.split('\n').map((x) => x.trim()).filter(Boolean);
  return lines.find((x) => /^https?:\/\//i.test(x)) || '';
}

function normalizeBlock(block) {
  const lines = block.split('\n').map((x) => x.trim()).filter(Boolean);
  const url = playableUrl(block);
  if (!url) return null;

  const urlIndex = lines.findIndex((x) => x === url);
  const useful = urlIndex >= 0 ? lines.slice(0, urlIndex + 1) : lines;
  return useful.join('\n');
}

function buildPlaylist(source) {
  const now = getVietnamNow();
  const seen = new Set();
  const out = ['#EXTM3U'];
  let skippedGroup = 0;
  let skippedNoUrl = 0;
  let skippedExpired = 0;

  for (const raw of parseBlocks(source)) {
    const group = groupOf(raw);
    if (!SPORTS_GROUPS.has(group)) {
      skippedGroup++;
      continue;
    }

    const block = normalizeBlock(raw);
    if (!block) {
      skippedNoUrl++;
      continue;
    }

    const lines = block.split('\n').filter(Boolean);
    const extinf = lines[0];
    if (/\bNone\b/i.test(block)) {
      skippedNoUrl++;
      continue;
    }

    const start = eventStart(extinf, now);
    if (start !== null) {
      const nowMs = Date.UTC(now.year, now.month - 1, now.day, now.hour - 7, now.minute);
      if (nowMs - start > KEEP_AFTER_START_MINUTES * 60 * 1000) {
        skippedExpired++;
        continue;
      }
    }

    const key = block;
    if (seen.has(key)) continue;
    seen.add(key);
    out.push(block);
  }

  if (out.length === 1) {
    throw new Error('No playable sports entries were found in the upstream playlist');
  }

  return {
    text: out.join('\n') + '\n',
    entries: out.length - 1,
    skippedGroup,
    skippedNoUrl,
    skippedExpired
  };
}

export default async function handler(req, res) {
  if (req.method === 'OPTIONS') {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
    return res.status(204).end();
  }

  // The app must consume the materialized, already-merged playlist. This prevents
  // the app from bypassing the GitHub filter and reading an old upstream directly.
  const upstream = process.env.SPORTS_UPSTREAM_URL || DEFAULT_UPSTREAM;

  try {
    const response = await fetch(upstream + (upstream.includes('?') ? '&' : '?') + 'nm7_cb=' + Date.now(), {
      redirect: 'follow',
      cache: 'no-store',
      headers: {
        'User-Agent': 'Mozilla/5.0 NM7-IPTV-Sports/1.0',
        'Accept': 'audio/x-mpegurl, application/vnd.apple.mpegurl, text/plain, */*',
        'Cache-Control': 'no-cache'
      },
      signal: AbortSignal.timeout(15000)
    });

    if (!response.ok) {
      throw new Error(`Upstream HTTP ${response.status}`);
    }

    const source = await response.text();
    if (!/#EXTM3U/i.test(source) || !/#EXTINF/i.test(source)) {
      throw new Error('Upstream did not return a valid M3U playlist');
    }

    const result = buildPlaylist(source);

    res.setHeader('Content-Type', 'audio/x-mpegurl; charset=utf-8');
    res.setHeader('Content-Disposition', 'inline; filename="nm7-sports.m3u"');
    res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0');
    res.setHeader('CDN-Cache-Control', 'no-store, max-age=0');
    res.setHeader('Vercel-CDN-Cache-Control', 'no-store, max-age=0');
    res.setHeader('Pragma', 'no-cache');
    res.setHeader('Expires', '0');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('X-NM7-Sports-Entries', String(result.entries));
    res.setHeader('X-NM7-Sports-Upstream', upstream);
    return res.status(200).send(result.text);
  } catch (error) {
    console.error('NM7 sports playlist error:', error);
    res.setHeader('Content-Type', 'text/plain; charset=utf-8');
    res.setHeader('Cache-Control', 'no-store, max-age=0');
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(502).send(`#EXTM3U\n# ERROR ${String(error?.message || error)}\n`);
  }
}
