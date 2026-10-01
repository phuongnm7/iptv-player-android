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
    private static final String UA = "NM7-Movie/1.10.113 Android";

    public static String get(String url, Map<String,String> headers) throws Exception {
        return request("GET", url, headers, null);
    }

    public static String postJson(String url, Map<String,String> headers, String body) throws Exception {
        return request("POST", url, headers, body);
    }

    private static String request(String method, String url, Map<String,String> headers, String body) throws Exception {
        HttpURLConnection c = null;
        try {
            URL u = new URL(url);
            c = (HttpURLConnection)u.openConnection();
            c.setInstanceFollowRedirects(true);
            c.setUseCaches(false);
            c.setConnectTimeout(15000);
            c.setReadTimeout(25000);
            c.setRequestMethod(method);
            c.setRequestProperty("User-Agent", UA);
            c.setRequestProperty("Accept", "application/json,text/plain,*/*");
            c.setRequestProperty("Cache-Control", "no-cache");
            if (headers != null) for (Map.Entry<String,String> e : headers.entrySet()) {
                if (e.getKey()!=null && e.getValue()!=null) c.setRequestProperty(e.getKey(), e.getValue());
            }
            if (body != null) {
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                c.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) throw new Exception("HTTP " + code);
            try (InputStream in = new BufferedInputStream(c.getInputStream())) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n, total=0;
                while ((n=in.read(buf))!=-1) {
                    total += n;
                    if (total > 12*1024*1024) throw new Exception("Phản hồi quá lớn");
                    out.write(buf,0,n);
                }
                return out.toString(StandardCharsets.UTF_8.name());
            }
        } finally {
            if (c != null) c.disconnect();
        }
    }
}