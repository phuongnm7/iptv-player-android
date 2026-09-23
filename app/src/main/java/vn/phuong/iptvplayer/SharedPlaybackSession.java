package vn.phuong.iptvplayer;

import android.content.Context;
import android.os.Bundle;

public final class SharedPlaybackSession {
    public static final String TAB_IPTV = "iptv";
    public static final String TAB_YOUTUBE = "youtube";
    public static final String TAB_MOVIE = "movie";
    public static final String TAB_MOVIE = "movie";
    private static final String PREFS = "nm7_shared_playback";
    private static final String KEY_TAB = "tab";
    private static final String KEY_YOUTUBE_BACKGROUND = "youtube_background";
    private static final String KEY_NAME = "name", KEY_URL = "url", KEY_MIME = "mime", KEY_POSITION = "position", KEY_PLAYING = "playing", KEY_HEADERS = "headers", KEY_OPTIONS = "options";
    private SharedPlaybackSession() {}
    public static synchronized void setTab(Context c,String tab){prefs(c).edit().putString(KEY_TAB,tab).apply();}
    public static synchronized String tab(Context c){return prefs(c).getString(KEY_TAB,TAB_IPTV);}
    public static synchronized void setYoutubeBackground(Context c,boolean active){prefs(c).edit().putBoolean(KEY_YOUTUBE_BACKGROUND,active).apply();}
    public static synchronized boolean isYoutubeBackground(Context c){return prefs(c).getBoolean(KEY_YOUTUBE_BACKGROUND,false);}
    public static synchronized void clearTransientState(Context c){prefs(c).edit().putBoolean(KEY_YOUTUBE_BACKGROUND,false).putString(KEY_TAB,TAB_IPTV).apply();}
    public static synchronized void saveIptv(Context c,String name,String url,String mime,Bundle headers,java.util.ArrayList<String> options,long position,boolean playing){
        android.content.SharedPreferences.Editor e=prefs(c).edit().putString(KEY_NAME,name==null?"":name).putString(KEY_URL,url==null?"":url).putString(KEY_MIME,mime==null?"":mime).putLong(KEY_POSITION,position).putBoolean(KEY_PLAYING,playing);
        e.putString(KEY_HEADERS,encodeBundle(headers)); e.putString(KEY_OPTIONS,options==null?"":android.text.TextUtils.join("\u001f",options)); e.apply();
    }
    public static synchronized State loadIptv(Context c){android.content.SharedPreferences p=prefs(c);String url=p.getString(KEY_URL,"");if(url==null||url.isEmpty())return null;java.util.ArrayList<String>o=new java.util.ArrayList<>();String raw=p.getString(KEY_OPTIONS,"");if(raw!=null&&!raw.isEmpty())for(String x:raw.split("\\\\u001f",-1))if(!x.isEmpty())o.add(x);return new State(p.getString(KEY_NAME,"IPTV"),url,p.getString(KEY_MIME,""),decodeBundle(p.getString(KEY_HEADERS,"")),o,p.getLong(KEY_POSITION,0L),p.getBoolean(KEY_PLAYING,true));}
    public static synchronized void clearIptv(Context c){prefs(c).edit().remove(KEY_NAME).remove(KEY_URL).remove(KEY_MIME).remove(KEY_POSITION).remove(KEY_PLAYING).remove(KEY_HEADERS).remove(KEY_OPTIONS).apply();}
    private static android.content.SharedPreferences prefs(Context c){return c.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    private static String encodeBundle(Bundle b){if(b==null||b.isEmpty())return "";StringBuilder o=new StringBuilder();for(String k:b.keySet()){String v=b.getString(k);if(v==null)continue;if(o.length()>0)o.append("\u001e");o.append(android.util.Base64.encodeToString(k.getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.NO_WRAP)).append(':').append(android.util.Base64.encodeToString(v.getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.NO_WRAP));}return o.toString();}
    private static Bundle decodeBundle(String raw){Bundle o=new Bundle();if(raw==null||raw.isEmpty())return o;for(String p:raw.split("\\u001e")){int s=p.indexOf(':');if(s<=0)continue;try{o.putString(new String(android.util.Base64.decode(p.substring(0,s),android.util.Base64.DEFAULT),java.nio.charset.StandardCharsets.UTF_8),new String(android.util.Base64.decode(p.substring(s+1),android.util.Base64.DEFAULT),java.nio.charset.StandardCharsets.UTF_8));}catch(IllegalArgumentException ignored){}}return o;}
    public static final class State{public final String name,url,mime;public final Bundle headers;public final java.util.ArrayList<String> options;public final long position;public final boolean playing;State(String n,String u,String m,Bundle h,java.util.ArrayList<String>o,long p,boolean y){name=n;url=u;mime=m;headers=h;options=o;position=p;playing=y;}}
}
