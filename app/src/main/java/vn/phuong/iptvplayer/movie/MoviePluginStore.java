package vn.phuong.iptvplayer.movie;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MoviePluginStore {
    private MoviePluginStore() {}
    private static File dir(Context c){File d=new File(c.getFilesDir(),"movie-plugins");if(!d.exists())d.mkdirs();return d;}
    public static List<File> list(Context c){
        File[] a=dir(c).listFiles((d,n)->n!=null&&n.toLowerCase().endsWith(".js"));
        List<File> out=new ArrayList<>();if(a!=null){Arrays.sort(a,(x,y)->x.getName().compareToIgnoreCase(y.getName()));for(File f:a)out.add(f);}return out;
    }
    public static File importFile(Context c,Uri uri,String suggested) throws Exception{
        String name=suggested==null||suggested.trim().isEmpty()?"movie-plugin.js":suggested.trim();
        if(!name.toLowerCase().endsWith(".js"))name+=".js";
        name=name.replaceAll("[^A-Za-z0-9._-]","_");
        File f=new File(dir(c),name);
        try(InputStream in=c.getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(f)){
            if(in==null)throw new Exception("Không mở được file");
            byte[] b=new byte[8192];int n,total=0;while((n=in.read(b))!=-1){total+=n;if(total>1024*1024)throw new Exception("Plugin lớn hơn 1 MB");out.write(b,0,n);}
        }
        String text=read(f);
        if(!text.contains("getUrlList")||!text.contains("parseListResponse")){f.delete();throw new Exception("File không đúng chuẩn Movie Plugin của NM7");}
        return f;
    }
    public static String read(File f) throws Exception{
        java.io.FileInputStream in=new java.io.FileInputStream(f);try{java.io.ByteArrayOutputStream b=new java.io.ByteArrayOutputStream();byte[] x=new byte[8192];int n;while((n=in.read(x))!=-1)b.write(x,0,n);return b.toString("UTF-8");}finally{in.close();}
    }
}