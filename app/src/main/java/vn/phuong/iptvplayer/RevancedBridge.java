package vn.phuong.iptvplayer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * ReVanced integration foundation for the Mobile 1.10.25 branch.
 *
 * The bridge deliberately keeps the patching input in app-private storage until the
 * patch pipeline is ready. A patched YouTube APK is a separate Android package;
 * embedding a second APK inside NM7 does not install it as GmsCore/YouTube.
 */
public final class RevancedBridge {
    private static final String YOUTUBE_PACKAGE = "com.google.android.youtube";
    private static final String GMSCORE_PACKAGE = "app.revanced.android.gms";

    private RevancedBridge() {}

    public static String inspectYouTubeApk(Context context, Uri uri) throws Exception {
        File apk = copyToPrivateInput(context, uri);
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(apk.getAbsolutePath(), 0);
            if (info == null) throw new IllegalArgumentException("File không phải APK hợp lệ");

            String version = info.versionName == null ? "?" : info.versionName;
            long code = android.os.Build.VERSION.SDK_INT >= 28
                    ? info.getLongVersionCode() : info.versionCode;

            StringBuilder result = new StringBuilder();
            result.append("Package: ").append(info.packageName).append('\n');
            result.append("Version: ").append(version).append(" (code ").append(code).append(")\n");

            if (!YOUTUBE_PACKAGE.equals(info.packageName)) {
                result.append("\nKhông phải APK YouTube chính thức (com.google.android.youtube).");
            } else {
                result.append("\nAPK YouTube hợp lệ.");
                result.append("\nGmsCore: ")
                        .append(isInstalled(pm, GMSCORE_PACKAGE) ? "đã có" : "chưa có");
                result.append("\n\nNhánh này đang chuẩn bị pipeline Patcher + patch bundle.");
            }
            return result.toString();
        } finally {
            apk.delete();
        }
    }

    public static boolean isGmsCoreInstalled(Context context) {
        return isInstalled(context.getPackageManager(), GMSCORE_PACKAGE);
    }

    private static boolean isInstalled(PackageManager pm, String packageName) {
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    private static File copyToPrivateInput(Context context, Uri uri) throws Exception {
        File dir = new File(context.getCacheDir(), "revanced");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Không tạo được thư mục làm việc ReVanced");
        }
        File apk = new File(dir, "input.apk");
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(apk)) {
            if (in == null) throw new IllegalArgumentException("Không mở được file APK");
            byte[] buf = new byte[8192];
            int n;
            long total = 0;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > 500L * 1024 * 1024) {
                    throw new IllegalArgumentException("APK quá lớn (> 500 MB)");
                }
                out.write(buf, 0, n);
            }
        }
        return apk;
    }
}
