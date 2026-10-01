package vn.phuong.iptvplayer.movie;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class MovieHttp {
    private MovieHttp() {}

    private static final String UA = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/131.0 Mobile Safari/537.36 NM7-Movie/1.10.114";
    private static final int MAX_RETRIES = 3;

    public static String get(String url, Map<String, String> headers) throws Exception {
        return request("GET", url, headers, null);
    }

    public static String postJson(String url, Map<String, String> headers, String body) throws Exception {
        return request("POST", url, headers, body);
    }

    public static String postText(String url, Map<String, String> headers, String body) throws Exception {
        return request("POST_TEXT", url, headers, body);
    }

    public static Map<String, String> novaHeaders(String url) {
        return novaHeaders(url, NOVA_VISITOR);
    }

    public static Map<String, String> novaHeaders(String url, String visitorId) {
        LinkedHashMap<String, String> h = new LinkedHashMap<>();
        String base = "https://novahd.cc";
        String visitor = visitorId == null || visitorId.isEmpty()
                ? NOVA_VISITOR : visitorId;

        h.put("Accept", url != null && url.contains("/api/sources")
                ? "application/x-ndjson" : "*/*");
        h.put("Accept-Language", "en-US,en;q=0.9");
        h.put("Origin", base);
        h.put("x-nova-visitor", visitor);
        h.put("DNT", "1");
        h.put("Sec-GPC", "1");
        h.put("Sec-Fetch-Dest", "empty");
        h.put("Sec-Fetch-Mode", "cors");
        h.put("Sec-Fetch-Site", "same-origin");
        h.put("Referer", novaReferer(url, base));
        return h;
    }

    private static final String NOVA_VISITOR = UUID.randomUUID().toString();

    private static String novaReferer(String url, String base) {
        if (url == null || url.isEmpty()) return base + "/";
        try {
            URI u = new URI(url);
            String path = u.getPath() == null ? "" : u.getPath();
            String query = u.getRawQuery() == null ? "" : u.getRawQuery();
            if (path.startsWith("/api/sources")) {
                String type = queryValue(query, "type");
                String tmdbId = queryValue(query, "tmdbId");
                if ("show".equalsIgnoreCase(type) && tmdbId != null) {
                    String season = queryValue(query, "season");
                    String episode = queryValue(query, "episode");
                    return base + "/watch/s/" + tmdbId + "/" +
                            (season == null || season.isEmpty() ? "1" : season) + "/" +
                            (episode == null || episode.isEmpty() ? "1" : episode);
                }
                if (tmdbId != null) return base + "/watch/m/" + tmdbId;
            }
        } catch (Exception ignored) {}
        return base + "/";
    }

    private static String decodeUtf8(String value) {
        if (value == null) return "";
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
    }

    private static String queryValue(String query, String wanted) {
        if (query == null || query.isEmpty()) return null;
        for (String part : query.split("&")) {
            int p = part.indexOf('=');
            if (p < 0) continue;
            String key = decodeUtf8(part.substring(0, p));
            if (!wanted.equals(key)) continue;
            return decodeUtf8(part.substring(p + 1));
        }
        return null;
    }

    private static String request(String method, String url, Map<String, String> headers, String body) throws Exception {
        Exception last = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            HttpURLConnection c = null;
            try {
                URL u = new URL(url);
                c = (HttpURLConnection) u.openConnection();
                c.setInstanceFollowRedirects(true);
                c.setUseCaches(false);
                c.setConnectTimeout(15000);
                c.setReadTimeout(25000);
                c.setRequestMethod("POST_TEXT".equals(method) || "POST".equals(method) ? "POST" : "GET");
                c.setRequestProperty("User-Agent", UA);
                c.setRequestProperty("Accept", url != null && url.contains("/api/sources")
                        ? "application/json, application/x-ndjson, text/plain, */*"
                        : "application/json,text/plain,text/html,*/*");
                c.setRequestProperty("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7");
                c.setRequestProperty("Cache-Control", "no-cache");
                c.setRequestProperty("Pragma", "no-cache");

                if (headers != null) {
                    for (Map.Entry<String, String> e : headers.entrySet()) {
                        if (e.getKey() != null && e.getValue() != null) {
                            c.setRequestProperty(e.getKey(), e.getValue());
                        }
                    }
                }

                if (body != null) {
                    c.setDoOutput(true);
                    c.setRequestProperty(
                            "Content-Type",
                            "POST_TEXT".equals(method)
                                    ? "application/x-www-form-urlencoded; charset=UTF-8"
                                    : "application/json; charset=UTF-8"
                    );
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    c.getOutputStream().write(bytes);
                    c.getOutputStream().flush();
                }

                int code = c.getResponseCode();
                if (code >= 200 && code < 300) {
                    try (InputStream in = new BufferedInputStream(c.getInputStream())) {
                        return readLimited(in);
                    }
                }

                String errorBody = "";
                try (InputStream in = c.getErrorStream()) {
                    if (in != null) errorBody = readLimited(in);
                }
                String detail = errorBody.replaceAll("\\s+", " ").trim();
                if (detail.length() > 220) detail = detail.substring(0, 220);

                String message = "HTTP " + code + (detail.isEmpty() ? "" : " — " + detail);
                last = new Exception(message);

                if (code != 429 && code != 502 && code != 503 && code != 504) {
                    throw last;
                }
            } catch (Exception e) {
                last = e;
                if (!isTransient(e) || attempt + 1 >= MAX_RETRIES) throw e;
            } finally {
                if (c != null) c.disconnect();
            }

            try {
                Thread.sleep(450L * (attempt + 1));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new Exception("Kết nối bị gián đoạn");
            }
        }

        throw last == null ? new Exception("HTTP request thất bại") : last;
    }

    private static boolean isTransient(Exception e) {
        String m = e.getMessage();
        if (m == null) return true;
        return m.startsWith("HTTP 429") || m.startsWith("HTTP 502")
                || m.startsWith("HTTP 503") || m.startsWith("HTTP 504")
                || m.contains("timed out") || m.contains("Connection reset")
                || m.contains("connection abort");
    }

    private static String readLimited(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        int total = 0;
        while ((n = in.read(buf)) != -1) {
            total += n;
            if (total > 12 * 1024 * 1024) {
                throw new Exception("Phản hồi quá lớn");
            }
            out.write(buf, 0, n);
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }
}