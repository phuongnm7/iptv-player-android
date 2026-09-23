package vn.phuong.iptvplayer.movie;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class NovaHdPlugin implements MoviePlugin {
 private static final String BASE="https://novahd.cc";
 private static final int CONNECT_TIMEOUT_MS=5000;
 private static final int READ_TIMEOUT_MS=8000;
 public String id(){return "novahd";} public String name(){return "NovaHD";}

 public void home(Context c,Callback cb){
  EXECUTOR.execute(()->{
    String[][] defs={{"🔥 Thịnh Hành","/api/trending?type=all"},{"🎬 Phim Lẻ Mới","/api/movies?page=1"},{"📺 Phim Bộ Mới","/api/shows?page=1"},{"💥 Hành Động","/api/movies?page=1&genre=28"},{"🚀 Khoa Học Viễn Tưởng","/api/movies?page=1&genre=878"},{"🎨 Hoạt Hình","/api/movies?page=1&genre=16"},{"👻 Kinh Dị","/api/movies?page=1&genre=27"},{"😂 Hài Hước","/api/movies?page=1&genre=35"}};
    List<MovieContent.Section> out=Collections.synchronizedList(new ArrayList<>());
    List<Thread> jobs=new ArrayList<>();
    for(String[] d:defs){Thread t=new Thread(()->{try{add(out,d[0],d[1]);}catch(Throwable ignored){}},"nova-home");t.start();jobs.add(t);}
    for(Thread t:jobs)try{t.join(9000);}catch(InterruptedException ignored){Thread.currentThread().interrupt();}
    if(out.isEmpty())cb.onError(new IOException("NovaHD không trả dữ liệu hoặc đang chặn API")); else cb.onSuccess(out);
  });
}
 private void add(List<MovieContent.Section>o,String title,String path)throws Exception{JSONArray a=arrays(parse(get(path))); MovieContent.Section s=new MovieContent.Section();s.title=title;for(int i=0;i<a.length();i++)s.items.add(item(a.getJSONObject(i)));if(!s.items.isEmpty())o.add(s);}

 public void search(Context c,String q,Callback cb){EXECUTOR.execute(()->{try{JSONArray a=arrays(parse(get("/api/search?search="+URLEncoder.encode(q,"UTF-8")+"&page=1")));List<MovieContent.Item>o=new ArrayList<>();for(int i=0;i<a.length();i++)o.add(item(a.getJSONObject(i)));cb.onSuccess(o);}catch(Throwable e){cb.onError(e);}});}

 public void detail(Context c,String cid,Callback cb){EXECUTOR.execute(()->{try{String type=cid.startsWith("show:")?"show":"movie";String id=cid.substring(cid.indexOf(':')+1);JSONObject o=new JSONObject(get("/api/"+(type.equals("show")?"shows/":"movies/")+id));MovieContent.Detail d=new MovieContent.Detail();d.item=item(o);d.director=o.optString("director",o.optString("directors",""));JSONArray ss=o.optJSONArray("seasons");if(ss!=null)for(int i=0;i<ss.length();i++){JSONObject x=ss.optJSONObject(i);if(x==null)continue;MovieContent.Season s=new MovieContent.Season();s.id=x.optString("id","");s.name=x.optString("name","Season "+x.optInt("season_number",i+1));s.number=x.optInt("season_number",i+1);JSONArray es=x.optJSONArray("episodes");if(es!=null)for(int j=0;j<es.length();j++){JSONObject e=es.optJSONObject(j);if(e==null)continue;MovieContent.Episode ep=new MovieContent.Episode();ep.id=e.optString("id","");ep.name=e.optString("name","Episode "+e.optInt("episode_number",j+1));ep.number=e.optInt("episode_number",j+1);s.episodes.add(ep);}d.seasons.add(s);}cb.onSuccess(d);}catch(Throwable e){cb.onError(e);}});}

 public void sources(Context c,String cid,String season,String episode,Callback cb){EXECUTOR.execute(()->{try{
   String type=cid.startsWith("show:")?"show":"movie";String id=cid.substring(cid.indexOf(':')+1);
   String p="/api/sources?type="+type+"&tmdbId="+URLEncoder.encode(id,"UTF-8");
   if("show".equals(type))p+="&season="+URLEncoder.encode(season==null?"":season,"UTF-8")+"&episode="+URLEncoder.encode(episode==null?"":episode,"UTF-8");
   JSONObject root=new JSONObject(get(p));JSONArray a=root.optJSONArray("sources");if(a==null)a=root.optJSONArray("data");
   List<MovieContent.Source>o=new ArrayList<>();
   if(a!=null)for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x==null)continue;MovieContent.Source s=new MovieContent.Source();s.url=x.optString("url",x.optString("file",""));s.mime=x.optString("mime","application/x-mpegURL");s.quality=x.optString("quality",x.optString("resolution",""));s.referer=x.optString("referer","");if(!s.url.isEmpty())o.add(s);}
   loadSubtitles(type,id,season,episode,o);
   cb.onSuccess(o);
 }catch(Throwable e){cb.onError(e);}});}

 private void loadSubtitles(String type,String id,String season,String episode,List<MovieContent.Source>sources){
   try{
     String p="/api/subs-status?type="+URLEncoder.encode(type,"UTF-8")+"&tmdbId="+URLEncoder.encode(id,"UTF-8");
     if("show".equals(type))p+="&season="+URLEncoder.encode(season==null?"":season, "UTF-8")+"&episode="+URLEncoder.encode(episode==null?"":episode, "UTF-8");
     Object parsed=parse(get(p)); JSONArray a=arraysNamed(parsed,"subtitles","subs","data","results");
     if(a==null||a.length()==0)return;
     for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x==null)continue;MovieContent.Subtitle sub=new MovieContent.Subtitle();sub.url=x.optString("url",x.optString("file",x.optString("src","")));sub.language=x.optString("language",x.optString("lang",""));sub.label=x.optString("label",sub.language);if(sub.url.isEmpty())continue;for(MovieContent.Source s:sources)s.subtitles.add(sub);}
   }catch(Throwable ignored){}
 }

 private static MovieContent.Item item(JSONObject o){MovieContent.Item x=new MovieContent.Item();x.id=o.optString("id",o.optString("tmdbId",""));x.type=o.optString("type",o.has("seasons")?"show":"movie");x.title=o.optString("title",o.optString("name",""));x.poster=image(o.optString("poster",o.optString("poster_path","")),"w500");x.backdrop=image(o.optString("backdrop",o.optString("backdrop_path","")),"w1280");x.overview=o.optString("overview",o.optString("description",""));x.year=o.optString("year",o.optString("release_date",""));if(x.year.length()>4)x.year=x.year.substring(0,4);x.rating=o.optString("rating",o.optString("vote_average",""));JSONArray g=o.optJSONArray("genres");if(g!=null)for(int i=0;i<g.length();i++)x.genres.add(String.valueOf(g.opt(i)));return x;}
 private static String image(String p,String size){if(p==null||p.isEmpty())return "";if(p.startsWith("http"))return p;return "https://image.tmdb.org/t/p/"+size+(p.startsWith("/")?p:"/"+p);}
 private static Object parse(String r)throws Exception{String s=r.trim();return s.startsWith("[")?new JSONArray(s):new JSONObject(s);}
 private static JSONArray arrays(Object r)throws Exception{return arraysNamed(r,"results","data","movies","shows");}
 private static JSONArray arraysNamed(Object r,String...names)throws Exception{if(r instanceof JSONArray)return(JSONArray)r;JSONObject o=(JSONObject)r;for(String n:names){JSONArray a=o.optJSONArray(n);if(a!=null)return a;}return null;}
 private static String get(String p)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(BASE+p).openConnection();c.setConnectTimeout(CONNECT_TIMEOUT_MS);c.setReadTimeout(READ_TIMEOUT_MS);c.setRequestProperty("User-Agent","NM7-Mobile-Movie/1.10.75");c.setRequestProperty("Accept","application/json");int code=c.getResponseCode();if(code<200||code>=300)throw new IllegalStateException("HTTP "+code);BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();String l;while((l=r.readLine())!=null)b.append(l);r.close();c.disconnect();return b.toString();}
}
