export default async function handler(req, res) {
  const sources = [
    'https://thanhtungitblog.blogspot.com/2026/06/httpsthanhtungitblog.blogspot.comtonghopnguon.html',
    'https://byvn.net/6ZQ5'
  ];

  // Giữ trận đấu trong danh sách tối đa 3 giờ sau giờ bắt đầu.
  // Mục đích là không xoá trận đang đá (đặc biệt trận có hiệp phụ),
  // nhưng tự loại các trận đã kết thúc khi NM7 tải lại playlist.
  const EVENT_KEEP_HOURS = 3;

  const browserHeaders = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36',
    'Accept': 'text/plain,application/vnd.apple.mpegurl,application/x-mpegURL,text/html,*/*;q=0.8',
    'Accept-Language': 'vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7',
    'Cache-Control': 'no-cache',
    'Pragma': 'no-cache'
  };

  function decodeHtml(text) {
    return (text || '')
      .replace(/&amp;/gi, '&')
      .replace(/&quot;/gi, '"')
      .replace(/&#39;/gi, "'")
      .replace(/&lt;/gi, '<')
      .replace(/&gt;/gi, '>');
  }

  function cleanM3U(text) {
    text = decodeHtml(text);
    const start = text.indexOf('#EXTM3U');
    if (start < 0) return '';
    text = text.slice(start);
    const bodyEnd = text.indexOf('</body>');
    if (bodyEnd >= 0) text = text.slice(0, bodyEnd);
    return text.trim() + '\n';
  }

  function isValidM3U(text) {
    return /#EXTM3U/i.test(text) && /#EXTINF/i.test(text);
  }

  function extractM3ULinks(html) {
    const decoded = decodeHtml(html || '');
    const matches = decoded.match(/https?:\/\/[^\s"'<>\\]+\.(?:m3u|m3u8)(?:\?[^\s"'<>\\]*)?/gi) || [];
    return [...new Set(matches)].map(u => u.replace(/[),.;]+$/, ''));
  }

  // Lấy thời gian hiện tại theo múi giờ Việt Nam, không phụ thuộc timezone của Vercel.
  function getVietnamNow() {
    const parts = new Intl.DateTimeFormat('en-US', {
      timeZone: 'Asia/Ho_Chi_Minh',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    }).formatToParts(new Date());

    const get = type => Number(parts.find(p => p.type === type)?.value || 0);
    return { year: get('year'), month: get('month'), day: get('day'), hour: get('hour'), minute: get('minute') };
  }

  function toComparableMinutes(year, month, day, hour, minute) {
    return Date.UTC(year, month - 1, day, hour, minute) / 60000;
  }

  // Các trận trong playlist có dạng: "23:45 16/09 ..." hoặc "00:00 17/09 ...".
  // Nếu một entry không có ngày/giờ như vậy thì giữ nguyên vì không thể kết luận nó đã hết.
  function getEventStartMinutes(extinfLine, now) {
    const match = extinfLine.match(/(?:^|[,\s])(?:[^,]*?)?(\d{1,2}):(\d{2})\s+(\d{1,2})\/(\d{1,2})(?:\s|[,])/);
    if (!match) return null;

    const hour = Number(match[1]);
    const minute = Number(match[2]);
    const day = Number(match[3]);
    const month = Number(match[4]);
    if (hour > 23 || minute > 59 || day < 1 || day > 31 || month < 1 || month > 12) return null;

    let year = now.year;
    let event = toComparableMinutes(year, month, day, hour, minute);
    const nowMinutes = toComparableMinutes(now.year, now.month, now.day, now.hour, now.minute);

    // Xử lý giao năm: nếu ngày/tháng cách hiện tại quá xa thì thử năm trước/năm sau.
    const candidates = [
      { year: now.year - 1, value: toComparableMinutes(now.year - 1, month, day, hour, minute) },
      { year: now.year, value: event },
      { year: now.year + 1, value: toComparableMinutes(now.year + 1, month, day, hour, minute) }
    ];
    const nearest = candidates.reduce((best, item) =>
      Math.abs(item.value - nowMinutes) < Math.abs(best.value - nowMinutes) ? item : best
    );

    year = nearest.year;
    event = nearest.value;
    return { start: event, now: nowMinutes, year };
  }

  function filterExpiredEvents(m3u) {
    const now = getVietnamNow();
    const maxAgeMinutes = EVENT_KEEP_HOURS * 60;
    let removed = 0;

    // Tách playlist thành từng block #EXTINF để xoá cả metadata + URL của trận hết giờ.
    const parts = m3u.split(/(?=#EXTINF)/i);
    const kept = [];

    for (const part of parts) {
      if (!/#EXTINF/i.test(part)) {
        kept.push(part);
        continue;
      }

      const extinfLine = part.split(/\r?\n/, 1)[0];
      const event = getEventStartMinutes(extinfLine, now);

      // Chỉ loại trận đã quá 3 giờ kể từ giờ bắt đầu.
      if (event && event.now - event.start > maxAgeMinutes) {
        removed++;
        continue;
      }

      kept.push(part);
    }

    return { text: kept.join(''), removed };
  }

  async function get(url, headers = {}) {
    const r = await fetch(url, {
      redirect: 'follow',
      cache: 'no-store',
      headers: { ...browserHeaders, ...headers }
    });
    const text = await r.text();
    return { r, text };
  }

  try {
    let finalText = '';
    let lastError = '';

    for (const source of sources) {
      try {
        const result = await get(`${source}${source.includes('?') ? '&' : '?'}nm7_cb=${Date.now()}`);
        const text = result.text;

        // Source may contain the playlist itself.
        if (isValidM3U(text)) {
          finalText = cleanM3U(text);
        }

        // Otherwise find a direct .m3u/.m3u8 URL embedded in the page and fetch it live.
        if (!isValidM3U(finalText)) {
          const links = extractM3ULinks(text);
          for (const link of links.slice(0, 10)) {
            const candidate = await get(link, {
              'Accept': 'text/plain,application/vnd.apple.mpegurl,application/x-mpegURL,*/*;q=0.8',
              'Referer': source
            });
            const cleaned = cleanM3U(candidate.text);
            if (isValidM3U(cleaned)) {
              finalText = cleaned;
              break;
            }
          }
        }

        if (isValidM3U(finalText)) break;
        lastError = `Nguồn không trả về M3U: ${source}`;
      } catch (e) {
        lastError = String(e?.message || e);
      }
    }

    if (!isValidM3U(finalText)) {
      throw new Error(lastError || 'Nguồn không trả về danh sách phát M3U hợp lệ');
    }

    // Lọc các trận đã kết thúc trước khi trả playlist cho NM7.
    const filtered = filterExpiredEvents(finalText);
    finalText = filtered.text;

    res.setHeader('Content-Type', 'audio/x-mpegurl; charset=utf-8');
    res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0');
    res.setHeader('CDN-Cache-Control', 'no-store, max-age=0');
    res.setHeader('Vercel-CDN-Cache-Control', 'no-store, max-age=0');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
    res.status(200).send(finalText);
  } catch (error) {
    res.setHeader('Content-Type', 'text/plain; charset=utf-8');
    res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, max-age=0');
    res.status(502).send(`#EXTM3U\n# ERROR ${String(error?.message || error)}`);
  }
}
