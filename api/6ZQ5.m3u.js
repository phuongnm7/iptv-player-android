export default async function handler(req, res) {
  const source = 'https://byvn.net/6ZQ5';

  try {
    const upstream = await fetch(source, {
      redirect: 'follow',
      headers: {
        'User-Agent': 'NM7-IPTV-M3U-Proxy/1.0',
        'Accept': '*/*'
      }
    });

    if (!upstream.ok) {
      throw new Error(`Source HTTP ${upstream.status}`);
    }

    let text = await upstream.text();
    const start = text.indexOf('#EXTM3U');
    if (start >= 0) text = text.slice(start);

    const bodyEnd = text.indexOf('</body>');
    if (bodyEnd >= 0) text = text.slice(0, bodyEnd);

    if (!text.includes('#EXTM3U') || !text.includes('#EXTINF')) {
      throw new Error('Source did not return a valid M3U playlist');
    }

    res.setHeader('Content-Type', 'audio/x-mpegurl; charset=utf-8');
    res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.status(200).send(text.trim() + '\n');
  } catch (error) {
    res.setHeader('Content-Type', 'text/plain; charset=utf-8');
    res.status(502).send(`#EXTM3U\n# ERROR ${String(error?.message || error)}`);
  }
}
