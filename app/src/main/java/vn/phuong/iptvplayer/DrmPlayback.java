package vn.phuong.iptvplayer;

import android.media.MediaDrm;
import android.util.Base64;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager;
import androidx.media3.exoplayer.drm.FrameworkMediaDrm;
import androidx.media3.exoplayer.drm.HttpMediaDrmCallback;
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback;
import androidx.media3.exoplayer.drm.MediaDrmCallback;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;

@UnstableApi
final class DrmPlayback {
    static void configure(DrmSpec spec, MediaItem.Builder item, DefaultMediaSourceFactory media) throws Exception {
        if (!spec.problem.isEmpty()) throw new IllegalArgumentException(spec.problem);
        if (!spec.hasDrm()) return;
        if (!MediaDrm.isCryptoSchemeSupported(spec.uuid()))
            throw new IllegalArgumentException("Thiết bị không hỗ trợ " + spec.system + ". PlayReady thường cần Android TV.");
        MediaItem.DrmConfiguration.Builder config = new MediaItem.DrmConfiguration.Builder(spec.uuid()).setMultiSession(true);
        MediaDrmCallback callback;
        if (spec.localClearKey()) {
            callback = new LocalMediaDrmCallback(clearKeyResponse(spec.license));
        } else {
            config.setLicenseUri(spec.license).setLicenseRequestHeaders(spec.headers);
            // Stream cookies are intentionally not forwarded to another license host.
            callback = new HttpMediaDrmCallback(spec.license, true, new DefaultHttpDataSource.Factory()
                    .setUserAgent("IPTV-Player/1.3 Android").setConnectTimeoutMs(15000)
                    .setReadTimeoutMs(20000).setDefaultRequestProperties(spec.headers));
        }
        DefaultDrmSessionManager manager = new DefaultDrmSessionManager.Builder()
                .setUuidAndExoMediaDrmProvider(spec.uuid(), FrameworkMediaDrm.DEFAULT_PROVIDER)
                .setMultiSession(true).build(callback);
        media.setDrmSessionManagerProvider(unused -> manager);
        item.setDrmConfiguration(config.build());
    }

    static byte[] clearKeyResponse(String source) throws Exception {
        JSONArray keys = new JSONArray();
        try {
            if (source.trim().startsWith("{")) {
                JSONObject input = new JSONObject(source);
                if (input.has("keys")) {
                    JSONArray array = input.getJSONArray("keys");
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject key = array.getJSONObject(i);
                        if (!key.optString("kty", "oct").equals("oct")) throw new IllegalArgumentException();
                        String kid = key.getString("kid"), value = key.getString("k");
                        if (!kid.matches("[A-Za-z0-9_-]{22}={0,2}") || !value.matches("[A-Za-z0-9_-]{22}={0,2}")) throw new IllegalArgumentException();
                        keys.put(jwk(Base64.decode(kid, Base64.URL_SAFE), Base64.decode(value, Base64.URL_SAFE)));
                    }
                } else {
                    Iterator<String> names = input.keys();
                    while (names.hasNext()) { String kid = names.next(); keys.put(jwk(hex(kid), hex(input.getString(kid)))); }
                }
            } else {
                for (String pair : source.split(",")) {
                    String[] values = pair.trim().split(":", -1);
                    if (values.length != 2) throw new IllegalArgumentException();
                    keys.put(jwk(hex(values[0]), hex(values[1])));
                }
            }
            if (keys.length() == 0 || keys.length() > 32) throw new IllegalArgumentException();
            return new JSONObject().put("keys", keys).put("type", "temporary").toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception error) {
            throw new IllegalArgumentException("ClearKey không hợp lệ: cần KID và KEY 16 byte, dạng hex hoặc JWK. Không hiển thị khóa trong thông báo lỗi.");
        }
    }

    private static JSONObject jwk(byte[] kid, byte[] key) throws Exception {
        if (kid.length != 16 || key.length != 16) throw new IllegalArgumentException();
        int flags = Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING;
        return new JSONObject().put("kty", "oct").put("kid", Base64.encodeToString(kid, flags)).put("k", Base64.encodeToString(key, flags));
    }
    private static byte[] hex(String value) {
        String clean = value.trim().replace("-", "");
        if (!clean.matches("(?i)[0-9a-f]{32}")) throw new IllegalArgumentException();
        byte[] bytes = new byte[16];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
        return bytes;
    }
}
