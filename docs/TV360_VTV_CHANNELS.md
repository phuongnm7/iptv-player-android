# TV360 VTV channel catalog

Verified from public TV360 channel pages and the user's authenticated playback inspection on 2026-09-19.

## Channels

| Name | TV360 channel ID (`ch`) | TV360 page |
|---|---:|---|
| VTV1 HD | 2 | https://tv360.vn/tv/vtv1-hd?ch=2 |
| VTV2 HD | 3 | https://tv360.vn/tv/vtv2-hd?ch=3 |
| VTV3 HD | 4 | https://tv360.vn/tv/vtv3-hd?ch=4 |
| VTV4 HD | 5 | https://tv360.vn/tv/vtv4-hd?ch=5 |
| VTV5 HD | 6 | https://tv360.vn/tv/vtv5-hd?ch=6 |
| VTV6 HD | 10043 | https://tv360.vn/tv/vtv6-hd?ch=10043 |
| VTV7 HD | 58 | https://tv360.vn/tv/vtv7-hd?ch=58 |
| VTV8 HD | 59 | https://tv360.vn/tv/vtv8-hd?ch=59 |
| VTV9 HD | 60 | https://tv360.vn/tv/vtv9-hd?ch=60 |
| VTV10 | unknown | Not verified on TV360 |

## Playback notes

The authenticated TV360 web session was observed to request:

1. `get-link?...&secured=true`
2. an HLS master playlist (`.m3u8`)
3. HLS variant playlists for 480p/720p/1080p.

The master playlist observed for VTV1 exposed 854x480, 1280x720, 1920x1080/25fps and 1920x1080/50fps variants.

Do **not** commit authenticated `cdntoken`, cookies, Authorization headers, access tokens, refresh tokens, or other session credentials. Those values are session-bound and should not be used as permanent playlist URLs.

This catalog is metadata only. A future TV360 playback adapter should obtain playback authorization through an authorized TV360 playback mechanism rather than hard-coding a captured session URL.

## Sources

- TV360 VTV1: https://tv360.vn/tv/vtv1-hd?c=-2&ch=2
- TV360 VTV6: https://tv360.vn/tv/vtv6-hd?c=-2&ch=10043
- TV360 Smart TV guide: https://static-zlr.tv360.vn/tv360-static/support/v1.0.2/hdsd/TV360_HD_SmartTV.pdf
