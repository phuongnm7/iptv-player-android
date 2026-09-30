package vn.phuong.iptvplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class Channel {
    private final String name;
    private final String group;
    private final String url;
    private final String tvgLogo;
    private final String logoUrl;
    private final String iconUrl;
    private final String tvgId;
    private final Map<String, String> headers;
    private boolean selected = true;
    private String originalExtInf = "";
    private final List<String> options = new ArrayList<>();

    public Channel(String name, String group, String url, String logo, String tvgId,
                   Map<String, String> headers) {
        this(name, group, url, logo, logo, "", tvgId, headers);
    }

    public Channel(String name, String group, String url, String tvgLogo, String logoUrl,
                   String iconUrl, String tvgId, Map<String, String> headers) {
        this.name = clean(name, "Kênh không tên");
        this.group = clean(group, "Chưa phân nhóm");
        this.url = clean(url, "");
        this.tvgLogo = clean(tvgLogo, "");
        this.logoUrl = clean(logoUrl, "");
        this.iconUrl = clean(iconUrl, "");
        this.tvgId = clean(tvgId, "");
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }

    public String name() { return name; }
    public String group() { return group; }
    public String url() { return url; }

    /** Original tvg-logo attribute from the playlist. */
    public String logo() { return tvgLogo; }
    public String tvgLogo() { return tvgLogo; }

    /** Explicit logo/logo_url attribute when present. */
    public String logoUrl() { return logoUrl; }

    /** icon/icon_url is the final metadata fallback. */
    public String iconUrl() { return iconUrl; }

    /**
     * Super OK-style effective logo resolution:
     * explicit logo URL -> tvg-logo -> icon URL.
     */
    public String effectiveLogoUrl() {
        if (!logoUrl.isEmpty()) return logoUrl;
        if (!tvgLogo.isEmpty()) return tvgLogo;
        return iconUrl;
    }

    /**
     * Ordered unique candidates let the UI retry a secondary logo source without
     * changing the channel's metadata.
     */
    public List<String> logoCandidates() {
        LinkedHashMap<String, Boolean> unique = new LinkedHashMap<>();

        // Keep provider-supplied metadata FIRST. The previous 1.10.111 build
        // put speculative catalog URLs before these and could spend seconds
        // timing out before reaching the real logo.
        if (!logoUrl.isEmpty()) unique.put(logoUrl, Boolean.TRUE);
        if (!tvgLogo.isEmpty()) unique.put(tvgLogo, Boolean.TRUE);
        if (!iconUrl.isEmpty()) unique.put(iconUrl, Boolean.TRUE);

        // Verified fallbacks sourced from the same IPTV/Worker catalog used by
        // NM7. These are only appended when the provider metadata fails.
        for (String fallback : logoCatalogFallbacks()) {
            if (fallback != null && !fallback.isEmpty()) unique.put(fallback, Boolean.TRUE);
        }
        return new ArrayList<>(unique.keySet());
    }

    private List<String> logoCatalogFallbacks() {
        LinkedHashMap<String, Boolean> urls = new LinkedHashMap<>();
        String id = tvgId.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        String normalizedName = name.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();

        // VTV fallback catalog from the NM7 VTV logo Worker override.
        if (id.equals("vtv2hd") || normalizedName.equals("vtv 2")) {
            urls.put("https://r2.epg.io.vn/2026-09-17/2.png", Boolean.TRUE);
        } else if (id.equals("vtv3hd") || normalizedName.equals("vtv 3")) {
            urls.put("https://i.ytimg.com/vi/4jr8Y13NSME/maxresdefault.jpg", Boolean.TRUE);
        } else if (id.equals("vtv4hd") || normalizedName.equals("vtv 4")) {
            urls.put("https://raw.githubusercontent.com/vuminhthanh12/Logo/refs/heads/main/VTV4.jpg", Boolean.TRUE);
        } else if (id.equals("vtv5hd") || normalizedName.equals("vtv 5")) {
            urls.put("https://s12811.cdn.mytvnet.vn/vimages/b5/5c/cc/c2/22/29/b5cc2-pvtv5hd-channel-unkn.png", Boolean.TRUE);
            urls.put("https://r2.epg.io.vn/2026-09-17/5.png", Boolean.TRUE);
        } else if (id.equals("vtv5hdtnb") || normalizedName.equals("vtv 5 tay nam bo")) {
            urls.put("https://r2.epg.io.vn/2026-09-17/6.png", Boolean.TRUE);
            urls.put("https://raw.githubusercontent.com/vuminhthanh12/vuminhthanh12/refs/heads/main/maxresdefault.jpg", Boolean.TRUE);
        } else if (id.equals("vtv5hdtn") || normalizedName.equals("vtv 5 tay nguyen")) {
            urls.put("https://s7730.cdn.mytvnet.vn/vimages/d4/46/61/1c/cf/f6/d461c-pvtv5tynguynhd-channel-unkn.png", Boolean.TRUE);
            urls.put("https://r2.epg.io.vn/2026-09-17/7.png", Boolean.TRUE);
        } else if (id.equals("vtv6hd") || normalizedName.equals("vtv 6")) {
            urls.put("https://raw.githubusercontent.com/vuminhthanh12/Logo/refs/heads/main/VTV6.png", Boolean.TRUE);
        } else if (id.equals("vtv7hd") || normalizedName.equals("vtv 7")) {
            urls.put("https://r2.epg.io.vn/2026-09-17/9.png", Boolean.TRUE);
        } else if (id.equals("vtv8hd") || normalizedName.equals("vtv 8")) {
            urls.put("https://raw.githubusercontent.com/vuminhthanh12/vuminhthanh12/refs/heads/main/vtv8.jpg", Boolean.TRUE);
        } else if (id.equals("vtv9hd") || normalizedName.equals("vtv 9")) {
            urls.put("https://raw.githubusercontent.com/vuminhthanh12/Logo/refs/heads/main/VTV9.png", Boolean.TRUE);
        } else if (id.equals("vtv10hd") || normalizedName.equals("vtv 10")) {
            urls.put("https://raw.githubusercontent.com/vuminhthanh12/vuminhthanh12/refs/heads/main/VTV10.png", Boolean.TRUE);
        }

        // Verified VTVCab artwork from NM7's unified sports source.
        if (id.equals("onsportsvn") || id.equals("vtvcab3hd")
                || normalizedName.contains("vtvcab 3")) {
            urls.put("https://freem3u.xyz/static/images/vtvcab/vtvcab3.png", Boolean.TRUE);
        } else if (id.equals("onsportsplusvn") || id.equals("vtvcab6hd")
                || normalizedName.contains("vtvcab 6")) {
            urls.put("https://freem3u.xyz/static/images/vtvcab/vtvcab6.png", Boolean.TRUE);
        } else if (id.equals("onfootballvn") || id.equals("vtvcab16hd")
                || normalizedName.contains("vtvcab 16")) {
            urls.put("https://freem3u.xyz/static/images/vtvcab/vtvcab16.png", Boolean.TRUE);
        } else if (id.equals("onsportsnewsvn") || id.equals("vtvcab18hd")
                || normalizedName.contains("vtvcab 18")) {
            urls.put("https://freem3u.xyz/static/images/vtvcab/vtvcab18.png", Boolean.TRUE);
        } else if (id.equals("ongolfvn") || id.equals("vtvcab23hd")
                || normalizedName.contains("vtvcab 23")) {
            urls.put("https://freem3u.xyz/static/images/vtvcab/vtvcab23.png", Boolean.TRUE);
        }

        return new ArrayList<>(urls.keySet());
    }

    public String tvgId() { return tvgId; }
    public Map<String, String> headers() { return headers; }
    public boolean selected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
    public String originalExtInf() { return originalExtInf; }
    public void setOriginalExtInf(String line) { originalExtInf = line; }
    public List<String> options() { return options; }

    public String mimeHint() {
        for (String option : options) {
            String lower = option.toLowerCase(java.util.Locale.ROOT);
            if (lower.endsWith("manifest_type=hls")) return "application/x-mpegURL";
            if (lower.endsWith("manifest_type=mpd")) return "application/dash+xml";
            if (lower.startsWith("#kodiprop:mimetype=")) return option.substring(option.indexOf('=') + 1).trim();
        }
        return "";
    }

    public boolean needsDrm() {
        for (String option : options) {
            if (option.toLowerCase(java.util.Locale.ROOT).contains("inputstream.adaptive.license")) return true;
        }
        return false;
    }

    public String identityKey() {
        Map<String, String> normalized = new TreeMap<>();
        for (Map.Entry<String, String> header : headers.entrySet()) {
            normalized.put(header.getKey().toLowerCase(java.util.Locale.ROOT), header.getValue());
        }
        StringBuilder key = new StringBuilder();
        appendKeyPart(key, url.trim());
        key.append(normalized.size()).append(':');
        for (Map.Entry<String, String> header : normalized.entrySet()) {
            appendKeyPart(key, header.getKey());
            appendKeyPart(key, header.getValue());
        }
        key.append(options.size()).append(':');
        for (String option : options) appendKeyPart(key, option);
        return key.toString();
    }

    private static void appendKeyPart(StringBuilder target, String value) {
        target.append(value.length()).append(':').append(value);
    }

    @Override public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Channel)) return false;
        return identityKey().equals(((Channel) object).identityKey());
    }

    @Override public int hashCode() { return Objects.hash(identityKey()); }
}
