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
        if (!logoUrl.isEmpty()) unique.put(logoUrl, Boolean.TRUE);
        if (!tvgLogo.isEmpty()) unique.put(tvgLogo, Boolean.TRUE);
        if (!iconUrl.isEmpty()) unique.put(iconUrl, Boolean.TRUE);
        return new ArrayList<>(unique.keySet());
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
