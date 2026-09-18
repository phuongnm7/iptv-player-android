package vn.phuong.iptvplayer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Experimental ReVanced integration foundation for the Mobile 1.10.25 branch. */
public final class RevancedBridge {
    private RevancedBridge() {}

    public static String inspectYouTubeApk(Context context, Uri uri) throws Exception {
        File apk = new File(context.getCacheDir(), "revanced-input.apk");
        try(InputStream in=context.getContentResolver().openInputStream(uri); FileOutputStream out=new FileOutputStream(apk)){
            if(in==null) throw new IllegalArgumentException("Không mở được file APK");
            byte[] buf=new byte[8192]; int n; long total=0;
            while((n=in.read(buf))!=-1){ total+=n; if(total>500L*1024*1024) throw new IllegalArgumentException("APK quá lớn (>500 MB)"); out.write(buf,0,n); }
        }
        PackageManager pm=context.getPackageManager();
        PackageInfo info=pm.getPackageArchiveInfo(apk.getAbsolutePath(),0);
        if(info==null) throw new IllegalArgumentException("File không phải APK hợp lệ");
        String version=info.versionName==null?"?":info.versionName;
        long code=android.os.Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;
        StringBuilder result=new StringBuilder();
        result.append("Package: ").append(info.packageName).append('\\n');
        result.append("Version: ").append(version).append(" (code ").append(code).append(")\\n");
        if(!"com.google.android.youtube".equals(info.packageName)){
            result.append("\\nKhông phải APK YouTube chính thức (com.google.android.youtube).");
        }else{
            result.append("\\nĐây là APK YouTube. Bước tiếp theo của nhánh thử nghiệm là nạp patch bundle và chạy Patcher.");
        }
        apk.delete();
        return result.toString();
    }
}
