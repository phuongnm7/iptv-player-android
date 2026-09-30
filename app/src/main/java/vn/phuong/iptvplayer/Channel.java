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

        // Prefer the deterministic channel catalog for known VTV/VTVCab ids.
        // This avoids waiting on a stale/broken playlist logo before reaching
        // the same channel artwork that Super OK can resolve independently.
        for (String fallback : logoCatalogFallbacks()) {
            if (fallback != null && !fallback.isEmpty()) unique.put(fallback, Boolean.TRUE);
        }

        // Explicit playlist metadata remains a fallback for channels whose
        // provider supplies a custom/current logo (ON Kids, ON Music, etc.).
        if (!logoUrl.isEmpty()) unique.put(logoUrl, Boolean.TRUE);
        if (!tvgLogo.isEmpty()) unique.put(tvgLogo, Boolean.TRUE);
        if (!iconUrl.isEmpty()) unique.put(iconUrl, Boolean.TRUE);

        return new ArrayList<>(unique.keySet());
    }

    private List<String> logoCatalogFallbacks() {
        LinkedHashMap<String, Boolean> urls = new LinkedHashMap<>();
        String id = tvgId.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        String normalizedName = name.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();

        // VTV catalog: stable public logo set used by multiple IPTV catalogs.
        String vtv = null;
        if (id.matches("vtv1hd") || normalizedName.matches("vtv 1( hd)?")) vtv = "1";
        else if (id.matches("vtv2hd") || normalizedName.matches("vtv 2( hd)?")) vtv = "2";
        else if (id.matches("vtv3hd") || normalizedName.matches("vtv 3( hd)?")) vtv = "3";
        else if (id.matches("vtv4hd") || normalizedName.matches("vtv 4( hd)?")) vtv = "4";
        else if (id.matches("vtv5hd") || normalizedName.matches("vtv 5( hd)?")) vtv = "5";
        else if (id.matches("vtv6hd") || normalizedName.matches("vtv 6( hd)?")) vtv = "6";
        else if (id.matches("vtv7hd") || normalizedName.matches("vtv 7( hd)?")) vtv = "7";
        else if (id.matches("vtv8hd") || normalizedName.matches("vtv 8( hd)?")) vtv = "8";
        else if (id.matches("vtv9hd") || normalizedName.matches("vtv 9( hd)?")) vtv = "9";
        if (vtv != null) urls.put("https://cdn.hqth.me/logo/thumbs/" + vtv + ".png", Boolean.TRUE);

        if (id.equals("vtv5hdtnb") || normalizedName.contains("vtv 5 tây nam bộ")) {
            urls.put("https://cdn.hqth.me/logo/thumbs/6.png", Boolean.TRUE);
            urls.put("https://i.imgur.com/mIUWkDx.png", Boolean.TRUE);
        }
        if (id.equals("vtv5hdtn") || normalizedName.contains("vtv 5 tây nguyên")) {
            urls.put("https://cdn.hqth.me/logo/thumbs/7.png", Boolean.TRUE);
            urls.put("https://i.imgur.com/R8c2swd.png", Boolean.TRUE);
        }

        // VTVCab catalog. The four embedded sports channels currently have
        // onsports*.VN ids and no tvg-logo, so name/id matching is intentional.
        if (id.equals("onsportsvn") || id.equals("vtvcab3hd")
                || normalizedName.contains("vtvcab 3") || normalizedName.matches("on sports( hd)?")) {
            urls.put("https://img.vnmedia.xyz/logo/on-sports.jpg", Boolean.TRUE);
            urls.put("https://cdn.hqth.me/logo/thumbs/14.png", Boolean.TRUE);
        }
        if (id.equals("onsportsplusvn") || id.equals("vtvcab6hd")
                || normalizedName.contains("vtvcab 6") || normalizedName.matches("on sports\+.*")) {
            urls.put("https://img.vnmedia.xyz/logo/on-sportsplus.jpg", Boolean.TRUE);
            urls.put("https://cdn.hqth.me/logo/thumbs/17.png", Boolean.TRUE);
        }
        if (id.equals("onfootballvn") || id.equals("vtvcab16hd")
                || normalizedName.contains("vtvcab 16") || normalizedName.matches("on football.*")) {
            urls.put("https://img.vnmedia.xyz/logo/on-football.jpg", Boolean.TRUE);
            urls.put("https://cdn.hqth.me/logo/thumbs/24.png", Boolean.TRUE);
        }
        if (id.equals("onsportsnewsvn") || id.equals("vtvcab18hd")
                || normalizedName.contains("vtvcab 18") || normalizedName.matches("on sports news.*")) {
            urls.put("https://img.vnmedia.xyz/logo/on-sportsnews.jpg", Boolean.TRUE);
            urls.put("https://cdn.hqth.me/logo/thumbs/26.png", Boolean.TRUE);
        }

        // Remaining numbered VTVCab catalog entries. These are deliberately
        // secondary fallbacks; explicit playlist logos always remain first.
        String cab = null;
        if (id.equals("vtvcab1hd")) cab = "12";
        else if (id.equals("vtvcab2hd")) cab = "13";
        else if (id.equals("vtvcab5hd")) cab = "16";
        else if (id.equals("vtvcab7hd")) cab = "18";
        else if (id.equals("vtvcab8hd")) cab = "19";
        else if (id.equals("vtvcab9hd")) cab = "20";
        else if (id.equals("vtvcab10hd")) cab = "21";
        else if (id.equals("vtvcab12hd")) cab = "22";
        else if (id.equals("vtvcab15hd")) cab = "23";
        else if (id.equals("vtvcab17hd")) cab = "25";
        else if (id.equals("vtvcab19hd")) cab = "27";
        else if (id.equals("vtvcab20hd")) cab = "28";
        else if (id.equals("vtvcab21hd")) cab = "29";
        else if (id.equals("vtvcab22hd")) cab = "30";
        if (cab != null) urls.put("https://cdn.hqth.me/logo/thumbs/" + cab + ".png", Boolean.TRUE);

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
