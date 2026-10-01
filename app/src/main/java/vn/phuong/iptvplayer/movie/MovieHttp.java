package vn.phuong.iptvplayer.movie;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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
                c.setRequestProperty("Accept", "application/json,text/plain,text/html,*/*");
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