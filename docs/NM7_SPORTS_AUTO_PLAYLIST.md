# NM7 Sports Auto Playlist

Endpoint: `/api/sports.m3u`

The endpoint fetches the configured upstream M3U on every request, keeps the sports source groups, removes entries with no playable HTTP(S) URL, de-duplicates entries, and removes events more than 3 hours past their advertised start time.

Default upstream:

`https://thcoban.github.io/thtt/tttt.m3u`

To replace it with a source you are authorized to use, set the Vercel environment variable:

`SPORTS_UPSTREAM_URL`

No fixed schedule is required for the endpoint itself: the IPTV app requests the fixed URL and receives the current upstream-derived playlist. A scheduled GitHub Action can be added later if a materialized M3U file is needed.

This endpoint intentionally does not bypass tokens, signatures, authentication, DRM, referrer controls, or other access controls on individual streams.
