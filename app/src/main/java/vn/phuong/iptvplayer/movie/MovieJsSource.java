package vn.phuong.iptvplayer.movie;

import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.json.JSONObject;
import org.json.JSONArray;
import vn.phuong.iptvplayer.movie.MovieModels.MovieDetail;
import vn.phuong.iptvplayer.movie.MovieModels.MovieEpisode;
import vn.phuong.iptvplayer.movie.MovieModels.MovieItem;
import vn.phuong.iptvplayer.movie.MovieModels.Playback;
import vn.phuong.iptvplayer.movie.MovieModels.Subtitle;

public final class MovieJsSource implements MovieSource {
    private final String sourceId,name;
    private final MovieJsRuntime js;
    private final java.util.concurrent.ExecutorService io=java.util.concurrent.Executors.newFixedThreadPool(2);

    public MovieJsSource(Context context,String fileName,String script){
        this.sourceId="plugin_"+Math.abs(fileName.hashCode());
        this.name=fileName.endsWith(".js")?fileName.substring(0,fileName.length()-3):fileName;
        js=new MovieJsRuntime(context,script);
    }

    @Override public String id(){return sourceId;}
    @Override public String name(){return name;}
    @Override public boolean isPlugin(){return true;}

    @Override public void loadHome(Callback<List<MovieItem>> cb){
        js.call("getUrlList",new MovieJsRuntime.Callback(){
            public void done(String url){
                io.execute(()->fetchListAndParse(url,"parseListResponse",cb));
            }
            public void error(String m){cb.onError("Plugin không tạo được URL danh sách: "+m);}
        },MovieJsRuntime.quote("trending"),MovieJsRuntime.quote("{\"page\":1}"));
    }

    @Override public void search(String query, Callback<List<MovieItem>> cb){
        js.call("getUrlSearch",new MovieJsRuntime.Callback(){
            public void done(String url){
                io.execute(()->fetchListAndParse(url,"parseSearchResponse",cb));
            }
            public void error(String m){cb.onError("Plugin không hỗ trợ tìm kiếm: "+m);}
        },MovieJsRuntime.quote(query),MovieJsRuntime.quote("{\"page\":1}"));
    }

    private void fetchListAndParse(String url,String parser,Callback<List<MovieItem>> cb){
        try{
            String json=MovieHttp.get(url,Collections.emptyMap());
            js.call(parser,new MovieJsRuntime.Callback(){
                public void done(String parsed){cb.onSuccess(parseItems(parsed));}
                public void error(String m){cb.onError("Plugin parse: "+m);}
            },MovieJsRuntime.quote(json),MovieJsRuntime.quote(url));
        }catch(Exception e){cb.onError("Plugin HTTP: "+safe(e));}
    }

    @Override public void loadDetail(MovieItem item, Callback<MovieDetail> cb){
        js.call("getUrlDetail",new MovieJsRuntime.Callback(){
            public void done(String url){
                io.execute(()->{
                    try{
                        String json=MovieHttp.get(url,Collections.emptyMap());
                        js.call("parseMovieDetail",new MovieJsRuntime.Callback(){
                            public void done(String parsed){cb.onSuccess(parseDetail(parsed,item));}
                            public void error(String m){cb.onError("Plugin parse detail: "+m);}
                        },MovieJsRuntime.quote(json),MovieJsRuntime.quote(url));
                    }catch(Exception e){cb.onError("Plugin detail HTTP: "+safe(e));}
                });
            }
            public void error(String m){cb.onError("Plugin không tạo được URL chi tiết: "+m);}
        },MovieJsRuntime.quote(item.id));
    }

    @Override public void loadPlayback(MovieEpisode episode, Callback<Playback> cb){
        if(episode.directUrl!=null&&!episode.directUrl.isEmpty()){
            cb.onSuccess(new Playback(episode.directUrl,mimeOf(episode.directUrl),new LinkedHashMap<String,String>(),Collections.emptyList()));
            return;
        }
        io.execute(()->{
            try{
                String url=episode.id;
                String json=MovieHttp.get(url,Collections.emptyMap());
                js.call("parseDetailResponse",new MovieJsRuntime.Callback(){
                    public void done(String parsed){
                        try{
                            JSONObject o=new JSONObject(parsed);
                            String u=o.optString("url","");
                            if(u.isEmpty()){cb.onError("Plugin không trả URL phát");return;}
                            Map<String,String> h=new LinkedHashMap<>();
                            JSONObject ho=o.optJSONObject("headers");
                            if(ho!=null){JSONArray ns=ho.names();if(ns!=null)for(int i=0;i<ns.length();i++){String k=ns.optString(i);h.put(k,ho.optString(k));}}
                            List<Subtitle> subs=new ArrayList<>();
                            JSONArray sa=o.optJSONArray("subtitles");
                            if(sa!=null)for(int i=0;i<sa.length();i++){JSONObject s=sa.optJSONObject(i);if(s!=null&&!s.optString("url","").isEmpty())subs.add(new Subtitle(s.optString("url"),s.optString("lang"),s.optString("label","Subtitle")));}
                            cb.onSuccess(new Playback(u,mimeOf(u),h,subs));
                        }catch(Exception e){cb.onError("Plugin playback parse: "+e.getMessage());}
                    }
                    public void error(String m){cb.onError("Plugin playback: "+m);}
                },MovieJsRuntime.quote(json),MovieJsRuntime.quote(url));
            }catch(Exception e){cb.onError("Plugin stream HTTP: "+safe(e));}
        });
    }

    private List<MovieItem> parseItems(String parsed){
        List<MovieItem> out=new ArrayList<>();
        try{
            JSONObject root=new JSONObject(parsed);
            JSONArray a=root.optJSONArray("items");
            if(a==null)return out;
            for(int i=0;i<a.length();i++){
                JSONObject o=a.optJSONObject(i);if(o==null)continue;
                String id=o.optString("id",""),title=o.optString("title","");
                if(id.isEmpty()||title.isEmpty())continue;
                out.add(new MovieItem(id,title,o.optString("posterUrl"),o.optString("backdropUrl",o.optString("posterUrl")),
                        o.optString("quality",""),extractYear(o.optString("episode_current")),sourceId,id.startsWith("show/"),null));
            }
        }catch(Exception ignored){}
        return out;
    }

    private MovieDetail parseDetail(String parsed,MovieItem seed){
        try{
            JSONObject o=new JSONObject(parsed);List<MovieEpisode> eps=new ArrayList<>();
            JSONArray servers=o.optJSONArray("servers");
            if(servers!=null)for(int i=0;i<servers.length();i++){
                JSONObject s=servers.optJSONObject(i);if(s==null)continue;
                JSONArray es=s.optJSONArray("episodes");if(es==null)continue;
                for(int j=0;j<es.length();j++){
                    JSONObject e=es.optJSONObject(j);if(e==null)continue;
                    String slug=e.optString("slug",""), nm=e.optString("name","Tập "+(j+1));
                    eps.add(new MovieEpisode(e.optString("id"),nm,slug,parseSeason(slug),parseEpisode(slug),null));
                }
            }
            String mid=o.optString("id",seed.id);
            MovieItem m=new MovieItem(mid,o.optString("title",seed.title),o.optString("posterUrl",seed.posterUrl),
                    o.optString("backdropUrl",seed.backdropUrl),o.optString("quality",seed.quality),
                    o.has("year")?String.valueOf(o.optInt("year")):seed.year,seed.sourceId,
                    mid.startsWith("show/"),seed.detailUrl);
            return new MovieDetail(m,o.optString("description",""),o.optString("category","Phim"),
                    o.optString("casts",""),o.optString("director",""),o.optString("rating",""),eps);
        }catch(Exception e){
            return new MovieDetail(seed,"","","","","",Collections.singletonList(new MovieEpisode(seed.id,"▶ Xem phim","full",0,0,null)));
        }
    }

    private static String mimeOf(String u){String x=u==null?"":u.toLowerCase();if(x.contains(".mpd"))return "application/dash+xml";if(x.contains(".m3u8"))return "application/x-mpegURL";return "video/mp4";}
    private static int parseSeason(String s){try{int a=s.indexOf("s"),b=s.indexOf("-e");return Integer.parseInt(s.substring(a+1,b));}catch(Exception e){return 0;}}
    private static int parseEpisode(String s){try{int a=s.indexOf("-e");return Integer.parseInt(s.substring(a+2));}catch(Exception e){return 0;}}
    private static String extractYear(String s){if(s==null)return "";String digits=s.replaceAll("[^0-9]","");return digits.length()>=4?digits.substring(0,4):digits;}
    @Override public void close(){io.shutdownNow();js.destroy();}
    private static String safe(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
}