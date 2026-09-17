export default async function handler(req, res) {
  const sources = [
    'https://thanhtungitblog.blogspot.com/2026/06/httpsthanhtungitblog.blogspot.comtonghopnguon.html',
    'https://byvn.net/6ZQ5'
  ];

  const browserHeaders = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36',
    'Accept': 'text/plain,application/vnd.apple.mpegurl,application/x-mpegURL,text/html,*/*;q=0.8',
    'Accept-Language': 'vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7',
    'Cache-Control': 'no-cache',
    'Pragma': 'no-cache'
  };

  function decodeHtml(text) {
    return text
      .replace(/&amp;/gi, '&')
      .replace(/&quot;/gi, '"')
      .replace(/&#39;/gi, "'")
      .replace(/&lt;/gi, '<')
      .replace(/&gt;/gi, '>');
  }

  function cleanM3U(text) {
    text = decodeHtml(text || '');
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

    res.setHeader('Content-Type', 'audio/x-mpegurl; charset=utf-8');
    res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0');
    res.setHeader('CDN-Cache-Control', 'no-store, max-age=0');
    res.setHeader('Vercel-CDN-Cache-Control', 'no-store, max-age=0');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
    res.status(200).send(finalText);
  } catch (error) {
    res.setHeader('Content-Type', 'text/plain; charset=utf-8');
    res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0');
    res.status(502).send(`#EXTM3U\n# ERROR ${String(error?.message || error)}`);
  }
}
