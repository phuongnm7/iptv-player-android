# NM7 playlist source analysis

## Input snapshot

Source file supplied in the handover conversation on 2026-09-17.

The snapshot is a merged M3U-style text containing repeated `#EXTM3U` markers and multiple channel groups. It contains both playable HLS URLs and entries whose stream value is `None`.

Local normalization performed during analysis:

- `566` `#EXTINF` blocks detected.
- `542` unique playable entry definitions after exact deduplication.
- `302` unique HTTP/HTTPS stream URLs.
- normalized playlist size: `176869` UTF-8 bytes.
- normalized playlist SHA-256: `3eecfa1ef330bd972e781a21eadab21588b1bce20c7bfb4431017bb865798a1d`.

## Groups found

- Socolive TV: 125
- Cola TV: 117
- Xôi Lạc Z TV: 52
- Vua Sân Cỏ TV: 51
- Khán Đài TV: 44
- Gà Vàng 33 TV: 42
- S8 TV: 32
- Giờ Vàng TV: 29
- Bia Ôm TV: 26
- Gà Vàng TV: 24
- Sao Kê TV: 15
- Chuối Chiên TV: 9

## Important stream formats

The snapshot contains ordinary HTTPS HLS URLs as well as `#EXTVLCOPT:http-referrer=...` followed by an HTTPS HLS URL. The referrer directive must be preserved when such an entry is retained.

## Updater status

The repository workflow `.github/workflows/update-nm7-playlist.yml` runs every 10 minutes and supports `workflow_dispatch`. GitHub documents scheduled workflows using POSIX cron; the minimum supported interval is 5 minutes, so the current 10-minute schedule is valid.

The workflow currently supports two modes:

1. If repository variable `NM7_SOURCE_URL` is configured, fetch and normalize that direct M3U/M3U8 source.
2. If `NM7_SOURCE_URL` is absent, decode the existing four bundled chunks and use them as a safe snapshot. This prevents repeated scheduled failures while no authorized live source URL is available.

## Remaining blocker

The supplied snapshot does not itself identify a single authorized live M3U/M3U8 source that can be fetched periodically to obtain future events. The stream URLs inside the snapshot are output/playback URLs, not necessarily the upstream playlist endpoint. Therefore an automatic updater cannot be made genuinely live from the snapshot alone without identifying an authorized upstream source/API.

Do not invent a source URL and do not use `byvn.net`.

## Production endpoint

`https://byvn-m3u-proxy.vercel.app/api/nm7-private.m3u`

The endpoint is already configured to read the four bundled chunks. The next live-source integration should continue to use this endpoint without changing its public URL.
