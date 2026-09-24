package vn.phuong.iptvplayer;

import java.util.Locale;

public final class CatchupUrlBuilder {
    private CatchupUrlBuilder() {}
    public static String build(String liveUrl, String type, String source, long startEpochSeconds, long endEpochSeconds) {
        if (source == null || source.trim().isEmpty()) return "";
        long now = Math.max(startEpochSeconds, endEpochSeconds);
        long duration = Math.max(1L, endEpochSeconds - startEpochSeconds);
        long offset = duration;
        String url = replaceTokens(source.trim(), startEpochSeconds, endEpochSeconds, duration, offset);
        String normalizedType = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (isAbsoluteHttp(url)) return url;
        if ("shift".equals(normalizedType)) {
            String separator = liveUrl.contains("?") ? "&" : "?";
            return liveUrl + separator + "utc=" + startEpochSeconds + "&lutc=" + now;
        }
        if ("append".equals(normalizedType) || normalizedType.isEmpty() || "default".equals(normalizedType)) {
            if (url.startsWith("?") || url.startsWith("&")) {
                return liveUrl + (liveUrl.contains("?") && url.startsWith("?") ? "&" : url);
            }
            return liveUrl + (liveUrl.endsWith("?") || liveUrl.endsWith("&") ? "" : (liveUrl.contains("?") ? "&" : "?")) + url;
        }
        return url;
    }
    private static String replaceTokens(String value,long start,long end,long duration,long offset) {
        return value
            .replace("$"+"{start}",Long.toString(start)).replace("$"+"{utc}",Long.toString(start))
            .replace("{start}",Long.toString(start)).replace("{utc}",Long.toString(start))
            .replace("$"+"{timestamp}",Long.toString(start)).replace("$"+"{end}",Long.toString(end)).replace("{end}",Long.toString(end))
            .replace("$"+"{duration}",Long.toString(duration)).replace("{duration}",Long.toString(duration))
            .replace("$"+"{offset}",Long.toString(offset)).replace("{offset}",Long.toString(offset));
    }
    private static boolean isAbsoluteHttp(String value) {
        String lower=value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://")||lower.startsWith("https://");
    }
}