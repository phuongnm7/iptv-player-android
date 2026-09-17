export default async function handler(req, res) {
  const source = 'https://byvn.net/6ZQ5';

  const browserHeaders = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36',
    'Accept': 'text/plain,application/vnd.apple.mpegurl,application/x-mpegURL,text/html,*/*;q=0.8',
    'Accept-Language': 'vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7',
    'Referer': 'https://byvn.net/',
    'Origin': 'https://byvn.net',
    'Cache-Control': 'no-cache',
    'Pragma': 'no-cache'
  };

  function cleanM3U(text) {
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
    // 1) Direct request to the original short URL.
    let result = await get(source);
    let text = result.text;

    // 2) Jina Reader can reach sites that reject Vercel serverless IPs.
    if (!isValidM3U(text)) {
      const jina = `https://r.jina.ai/https://byvn.net/6ZQ5?nm7_cb=${Date.now()}`;
      result = await get(jina, {
        'User-Agent': 'Mozilla/5.0',
        'Accept': 'text/plain,*/*;q=0.8'
      });
      text = result.text;
    }

    // 3) AllOrigins raw proxy as a second independent fallback.
    if (!isValidM3U(text)) {
      const encoded = encodeURIComponent(source);
      const allOrigins = `https://api.allorigins.win/raw?url=${encoded}&nm7_cb=${Date.now()}`;
      result = await get(allOrigins, {
        'User-Agent': 'NM7-IPTV-M3U-Proxy/1.0',
        'Accept': 'text/plain,*/*;q=0.8'
      });
      text = result.text;
    }

    // 4) corsproxy.io fallback.
    if (!isValidM3U(text)) {
      const encoded = encodeURIComponent(source);
      const corsProxy = `https://corsproxy.io/?url=${encoded}&nm7_cb=${Date.now()}`;
      result = await get(corsProxy, {
        'User-Agent': 'NM7-IPTV-M3U-Proxy/1.0',
        'Accept': 'text/plain,*/*;q=0.8'
      });
      text = result.text;
    }

    text = cleanM3U(text);

    if (!isValidM3U(text)) {
      throw new Error('Nguồn không trả về danh sách phát M3U hợp lệ');
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
