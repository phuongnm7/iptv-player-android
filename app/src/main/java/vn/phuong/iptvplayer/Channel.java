package vn.phuong.iptvplayer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.List;

public final class Channel {
    private final String name;
    private final String group;
    private final String url;
    private final String tvgLogo;
    private final String logoUrl;
    private final String tvgId;
    private final Map<String, String> headers;
    private boolean selected = true;
    private String originalExtInf = "";
    private final List<String> options = new ArrayList<>();

    public Channel(String name, String group, String url, String logo, String tvgId,
                   Map<String, String> headers) {
        this(name, group, url, logo, logo, tvgId, headers);
    }

    /** Super OK keeps tvg-logo and the explicit logo URL separately, then binds an effective URL. */
    public Channel(String name, String group, String url, String tvgLogo, String logoUrl,
                   String tvgId, Map<String, String> headers) {
        this.name = clean(name, "Kênh không tên");
        this.group = clean(group, "Chưa phân nhóm");
        this.url = clean(url, "");
        this.tvgLogo = clean(tvgLogo, "");
        this.logoUrl = clean(logoUrl, "");
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
    /** Original tvg-logo value, retained for round-trip export. */
    public String logo() { return tvgLogo; }
    public String tvgLogo() { return tvgLogo; }
    public String logoUrl() { return logoUrl; }
    /** Same precedence used by Super OK's M3uSource.getEffectiveLogoUrl(). */
    public String effectiveLogoUrl() { return !logoUrl.isEmpty() ? logoUrl : tvgLogo; }
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
        // Paths, query tokens and header values are case-sensitive.
        Map<String, String> normalized = new TreeMap<>();
        for (Map.Entry<String, String> header : headers.entrySet()) {
            normalized.put(header.getKey().toLowerCase(java.util.Locale.ROOT), header.getValue());
        }
        // Length-prefix each field: Map/List.toString() is ambiguous when
        // values themselves contain commas, brackets or equals signs.
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
