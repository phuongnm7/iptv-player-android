package vn.phuong.iptvplayer;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/** Extracts the build-time embedded ReVanced patch bundle into app-private storage. */
public final class RevancedBundleStore {
    private static final String ASSET = "revanced/patches.rvp";
    private static final String FILE_NAME = "patches.rvp";

    private RevancedBundleStore() {}

    public static File getBundle(Context context) throws Exception {
        File dir = new File(context.getFilesDir(), "revanced");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Không tạo được thư mục ReVanced");
        }
        File out = new File(dir, FILE_NAME);
        if (out.isFile() && out.length() > 0) return out;

        try (InputStream in = context.getAssets().open(ASSET);
             FileOutputStream stream = new FileOutputStream(out)) {
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) != -1) stream.write(buffer, 0, n);
        }
        if (!out.isFile() || out.length() == 0) {
            throw new IllegalStateException("Patch bundle ReVanced không tồn tại trong APK");
        }
        return out;
    }

    public static String status(Context context) {
        try {
            File file = getBundle(context);
            return "Patch bundle đã tích hợp • " + humanBytes(file.length());
        } catch (Exception e) {
            return "Patch bundle chưa sẵn sàng: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private static String humanBytes(long value) {
        if (value < 1024) return value + " B";
        if (value < 1024 * 1024) return String.format(java.util.Locale.ROOT, "%.1f KB", value / 1024d);
        return String.format(java.util.Locale.ROOT, "%.1f MB", value / (1024d * 1024d));
    }
}
