export default async function handler(req, res) {
  const source = 'https://byvn.net/6ZQ5';

  const headers = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36',
    'Accept': 'text/plain,application/vnd.apple.mpegurl,application/x-mpegURL,*/*;q=0.8',
    'Accept-Language': 'vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7',
    'Referer': 'https://byvn.net/',
    'Origin': 'https://byvn.net',
    'Cache-Control': 'no-cache',
    'Pragma': 'no-cache'
  };

  async function fetchSource(url, extraHeaders = {}) {
    return fetch(url, {
      redirect: 'follow',
      cache: 'no-store',
      headers: { ...headers, ...extraHeaders }
    });
  }

  function cleanM3U(text) {
    const start = text.indexOf('#EXTM3U');
    if (start >= 0) text = text.slice(start);

    const bodyEnd = text.indexOf('</body>');
    if (bodyEnd >= 0) text = text.slice(0, bodyEnd);

    return text.trim() + '\n';
  }

  function isValidM3U(text) {
    return text.includes('#EXTM3U') && text.includes('#EXTINF');
  }

  try {
    // First try the origin directly with browser-like headers.
    let upstream = await fetchSource(source);
    let text = await upstream.text();

    // If byvn blocks Vercel with 403, use Jina Reader as a browser-capable
    // fallback. A timestamp prevents the Reader URL itself from being reused
    // from its short cache window.
    if (!upstream.ok || !isValidM3U(text)) {
      const readerUrl = `https://r.jina.ai/http://byvn.net/6ZQ5?nm7_cb=${Date.now()}`;
      const reader = await fetch(readerUrl, {
        redirect: 'follow',
        cache: 'no-store',
        headers: {
          'User-Agent': 'NM7-IPTV-M3U-Proxy/1.0',
          'Accept': 'text/plain,*/*;q=0.8'
        }
      });

      if (!reader.ok) {
        throw new Error(`Source HTTP ${upstream.status}; fallback HTTP ${reader.status}`);
      }

      text = await reader.text();
    }

    text = cleanM3U(text);

    if (!isValidM3U(text)) {
      throw new Error('Source did not return a valid M3U playlist');
    }

    res.setHeader('Content-Type', 'audio/x-mpegurl; charset=utf-8');
    res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0');
    res.setHeader('CDN-Cache-Control', 'no-store, max-age=0');
    res.setHeader('Vercel-CDN-Cache-Control', 'no-store, max-age=0');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
    res.status(200).send(text);
  } catch (error) {
    res.setHeader('Content-Type', 'text/plain; charset=utf-8');
    res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, max-age=0');
    res.status(502).send(`#EXTM3U\n# ERROR ${String(error?.message || error)}`);
  }
}
