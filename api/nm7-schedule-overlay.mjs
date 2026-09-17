const SOURCES = [
  ['Vua Sân Cỏ TV', 'https://vscvn.com/'],
  ['Giờ Vàng TV', 'https://giovang.beer/'],
  ['Bia Ôm TV', 'https://biaomtv17.com/'],
  ['Xôi Lạc Z TV', 'https://xoilaczaq.tv/'],
  ['Sao Kê TV', 'https://www.saoketv-play.com/'],
  ['Gà Vàng TV', 'https://gavangtv.sbs/lich-thi-dau/'],
  ['Cola TV', 'https://colatv.live/'],
  ['Khán Đài TV', 'https://khandai3.link/'],
  ['Socolive TV', 'https://socolives.tv/'],
];

const clean = (s) => String(s || '')
  .replace(/&nbsp;/gi, ' ')
  .replace(/&amp;/gi, '&')
  .replace(/&#39;|&#x27;/gi, "'")
  .replace(/&quot;/gi, '"')
  .replace(/<[^>]+>/g, ' ')
  .replace(/\s+/g, ' ')
  .trim();

const norm = (s) => clean(s)
  .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
  .toLowerCase()
  .replace(/\b(clb|fc|cf|afc|cau lac bo|doi bong)\b/g, ' ')
  .replace(/[^a-z0-9]+/g, ' ')
  .trim();

const slug = (s) => norm(s).replace(/\s+/g, '-');

function parseMatches(html) {
  const source = String(html || '').replace(/\r/g, '');
  const text = clean(source);
  const matches = [];

  // Most of the target sites expose cards/tables containing a time, two team
  // names and often a BLV/status label. We deliberately accept only bounded
  // matches so unrelated article prose is not converted into playlist items.
  const re = /(\d{1,2}:\d{2})\s*(?:[-|/]\s*)?(\d{1,2}[./-]\d{1,2})?\s*([A-Za-zÀ-ỹ0-9][^<>]{1,100}?)\s+vs\s+([A-Za-zÀ-ỹ0-9][^<>]{1,100}?)(?=\s+(?:BLV|LIVE|Sắp diễn ra|Đang diễn ra|Chưa bắt đầu|Xem|\d{1,2}:\d{2})\b|$)/gi;
  for (const m of text.matchAll(re)) {
    const home = clean(m[3]).replace(/\s+/g, ' ');
    const away = clean(m[4]).replace(/\s+/g, ' ');
    if (home.length < 2 || away.length < 2 || home.length > 100 || away.length > 100) continue;
    matches.push({ time: m[1], date: m[2] || '', home, away, homeKey: norm(home), awayKey: norm(away) });
  }

  // Fallback for compact markup where team names are adjacent to VS.
  if (!matches.length) {
    const re2 = /(\d{1,2}:\d{2})[^\n]{0,100}([A-Za-zÀ-ỹ0-9][^\n]{1,90}?)\s+VS\s+([A-Za-zÀ-ỹ0-9][^\n]{1,90}?)(?=\s+(?:LIVE|Sắp diễn ra|Đang diễn ra|BLV)\b)/gi;
    for (const m of source.matchAll(re2)) {
      const home = clean(m[2]);
      const away = clean(m[3]);
      if (home.length >= 2 && away.length >= 2) matches.push({ time: m[1], date: '', home, away, homeKey: norm(home), awayKey: norm(away) });
    }
  }

  const unique = new Map();
  for (const m of matches) unique.set(`${m.time}|${m.date}|${m.homeKey}|${m.awayKey}`, m);
  return [...unique.values()].slice(0, 300);
}

function canonicalBlocks(playlist) {
  return String(playlist).replace(/\r/g, '')
    .split(/(?=#EXTINF\s*:)/i)
    .filter((b) => /^\s*#EXTINF\s*:/i.test(b));
}

function extractMeta(block) {
  const lines = block.split('\n').map((x) => x.trim()).filter(Boolean);
  const header = lines[0] || '';
  const group = header.match(/group-title="([^"]+)"/i)?.[1] || '';
  const logo = header.match(/(?:tvg-logo|tv-logo)="([^"]+)"/i)?.[1] || '';
  const url = lines.find((x) => /^https?:\/\//i.test(x)) || '';
  return { lines, header, group, logo, url };
}

function teamMatchScore(meta, match) {
  const hay = `${meta.logo} ${meta.url} ${meta.header}`.toLowerCase();
  const home = norm(match.home);
  const away = norm(match.away);
  let score = 0;
  if (home && hay.includes(home.replace(/ /g, '-'))) score += 8;
  if (away && hay.includes(away.replace(/ /g, '-'))) score += 8;
  const hTokens = home.split(' ').filter((x) => x.length >= 4);
  const aTokens = away.split(' ').filter((x) => x.length >= 4);
  for (const t of [...hTokens, ...aTokens]) if (hay.includes(t)) score += 1;
  return score;
}

function replaceTitle(block, title) {
  const lines = block.split('\n');
  const i = lines.findIndex((x) => /^\s*#EXTINF\s*:/i.test(x));
  if (i < 0) return block;
  const header = lines[i];
  const prefix = header.slice(0, header.indexOf(','));
  lines[i] = `${prefix}, ${title}`;
  return lines.join('\n');
}

function formatDateLabel(match) {
  if (!match.date) return '';
  const p = match.date.replace(/\./g, '/').replace(/-/g, '/').split('/');
  if (p.length !== 2) return '';
  return `${String(Number(p[0])).padStart(2, '0')}/${String(Number(p[1])).padStart(2, '0')}`;
}

async function fetchSource(url) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 5000);
  try {
    const r = await fetch(url, { headers: { 'User-Agent': 'Mozilla/5.0 NM7-IPTV/1.0', 'Cache-Control': 'no-cache' }, signal: controller.signal });
    if (!r.ok) throw new Error(`HTTP ${r.status}`);
    return await r.text();
  } finally {
    clearTimeout(timer);
  }
}

export async function overlayCurrentMatches(playlist) {
  let blocks = canonicalBlocks(playlist);
  const sourceResults = await Promise.allSettled(SOURCES.map(async ([group, url]) => ({ group, matches: parseMatches(await fetchSource(url)) })));
  const byGroup = new Map();
  for (const r of sourceResults) if (r.status === 'fulfilled' && r.value.matches.length) byGroup.set(r.value.group, r.value.matches);

  if (!byGroup.size) return playlist;

  const used = new Set();
  blocks = blocks.map((block) => {
    const meta = extractMeta(block);
    const matches = byGroup.get(meta.group);
    if (!matches?.length) return block;

    let best = null;
    let bestScore = 0;
    for (const match of matches) {
      const score = teamMatchScore(meta, match);
      if (score > bestScore) { bestScore = score; best = match; }
    }
    // Never rename an entry from weak textual coincidence. This keeps stream
    // URLs untouched and avoids assigning the wrong match to a channel.
    if (!best || bestScore < 2) return block;

    const key = `${meta.group}|${best.time}|${best.homeKey}|${best.awayKey}`;
    if (used.has(key)) return block;
    used.add(key);
    const date = formatDateLabel(best);
    const title = `${best.time}${date ? ` ${date}` : ''} ⚽ ${best.home} vs ${best.away}`;
    return replaceTitle(block, title);
  });

  return `#EXTM3U\n${blocks.join('\n').trim()}\n`;
}
