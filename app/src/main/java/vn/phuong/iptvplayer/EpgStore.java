package vn.phuong.iptvplayer;

import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

/** Downloads a current-programme snapshot from an XMLTV feed. */
final class EpgStore {
    static final class Programme {
        final String title;
        final long startMs, endMs;
        Programme(String title,long startMs,long endMs){this.title=title;this.startMs=startMs;this.endMs=endMs;}
        int progress(){long span=endMs-startMs;if(span<=0)return 0;return (int)Math.max(0,Math.min(100,(System.currentTimeMillis()-startMs)*100/span));}
        String startText(){return time(startMs);}
        String endText(){return time(endMs);}
        private static String time(long value){return new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(value));}
    }
    static final class Guide {
        private final Map<String,Programme> programmes;
        Guide(Map<String,Programme> programmes){this.programmes=programmes;}
        Programme find(Channel channel){
            String id=normalize(channel.tvgId()),name=normalize(channel.name());
            Programme p=programmes.get(id);
            if(p==null)p=programmes.get(name);
            if(p==null)p=programmes.get(relaxed(id));
            if(p==null)p=programmes.get(relaxed(name));
            if(p==null&&!name.isEmpty()){
                String target=relaxed(name);
                for(Map.Entry<String,Programme> item:programmes.entrySet()){
                    String key=relaxed(item.getKey());
                    if(!target.isEmpty()&&!key.isEmpty()&&(target.equals(key)||target.contains(key)||key.contains(target))){p=item.getValue();break;}
                }
            }
            return p;
        }
        int size(){return programmes.size();}
    }

    static Guide download(String address)throws Exception{
        if(address==null||!(address.startsWith("http://")||address.startsWith("https://")))throw new IllegalArgumentException("URL EPG phải bắt đầu bằng http:// hoặc https://");
        HttpURLConnection connection=(HttpURLConnection)new URL(address).openConnection();
        connection.setConnectTimeout(15000);connection.setReadTimeout(30000);connection.setInstanceFollowRedirects(true);connection.setRequestProperty("User-Agent","Nm7-IPTV/1.10.1 Android");
        int code=connection.getResponseCode();if(code<200||code>=300)throw new Exception("HTTP "+code);
        try(InputStream raw=new BufferedInputStream(connection.getInputStream());InputStream input=isGzip(address,connection)?new GZIPInputStream(raw):raw){return parse(input,System.currentTimeMillis());}finally{connection.disconnect();}
    }

    static Guide parse(InputStream input,long now)throws Exception{
        XmlPullParser parser=Xml.newPullParser();parser.setInput(input,"UTF-8");
        Map<String,List<String>> displayNames=new HashMap<>();Map<String,Programme> byId=new HashMap<>();
        String channelId="",programmeId="",title="";long start=0,end=0;int event=parser.getEventType();
        while(event!=XmlPullParser.END_DOCUMENT){
            if(event==XmlPullParser.START_TAG){
                String tag=parser.getName();
                if("channel".equals(tag)){channelId=value(parser,"id");}
                else if("display-name".equals(tag)&&!channelId.isEmpty()){
                    String name=parser.nextText();
                    if(name!=null&&!name.trim().isEmpty())displayNames.computeIfAbsent(channelId,k->new ArrayList<>()).add(name.trim());
                }
                else if("programme".equals(tag)){programmeId=value(parser,"channel");start=parseTime(value(parser,"start"));end=parseTime(value(parser,"stop"));title="";}
                else if("title".equals(tag)&&!programmeId.isEmpty()){title=parser.nextText();}
            }
            else if(event==XmlPullParser.END_TAG){
                String tag=parser.getName();
                if("channel".equals(tag))channelId="";
                else if("programme".equals(tag)){
                    if(start<=now&&now<end&&!programmeId.isEmpty())byId.put(programmeId,new Programme(title==null||title.trim().isEmpty()?"Chương trình đang phát":title.trim(),start,end));
                    programmeId="";
                }
            }
            event=parser.next();
        }
        Map<String,Programme> result=new HashMap<>();
        for(Map.Entry<String,Programme> item:byId.entrySet()){
            putAlias(result,item.getKey(),item.getValue());
            List<String> names=displayNames.get(item.getKey());
            if(names!=null)for(String name:names)putAlias(result,name,item.getValue());
        }
        return new Guide(result);
    }

    private static void putAlias(Map<String,Programme> result,String value,Programme programme){
        String normal=normalize(value);if(!normal.isEmpty())result.put(normal,programme);
        String relaxed=relaxed(normal);if(!relaxed.isEmpty())result.put(relaxed,programme);
    }
    private static boolean isGzip(String url,HttpURLConnection connection){String encoding=connection.getContentEncoding();String type=connection.getContentType();return url.toLowerCase(Locale.ROOT).endsWith(".gz")||(encoding!=null&&encoding.toLowerCase(Locale.ROOT).contains("gzip"))||(type!=null&&type.toLowerCase(Locale.ROOT).contains("gzip"));}
    private static String value(XmlPullParser parser,String name){String value=parser.getAttributeValue(null,name);return value==null?"":value.trim();}
    private static long parseTime(String value){if(value==null)return 0;String clean=value.trim().replaceAll("\\s+"," ");String[] patterns={"yyyyMMddHHmmss Z","yyyyMMddHHmm Z","yyyyMMddHHmmss","yyyyMMddHHmm"};for(String pattern:patterns)try{SimpleDateFormat f=new SimpleDateFormat(pattern,Locale.ROOT);if(!pattern.contains("Z"))f.setTimeZone(TimeZone.getDefault());f.setLenient(false);Date d=f.parse(clean);if(d!=null)return d.getTime();}catch(Exception ignored){}return 0;}
    private static String normalize(String value){return value==null?"":value.trim().toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]","");}
    private static String relaxed(String value){
        String n=normalize(value);
        return n.replaceAll("(?i)(fullhd|fhd|uhd|4k|1080p|720p|hd)$","")
                .replaceAll("(?i)^(kenh|channel)","")
                .replaceAll("(?i)(television|channel)$","");
    }
}
