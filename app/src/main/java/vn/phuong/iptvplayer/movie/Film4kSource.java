package vn.phuong.iptvplayer.movie;

import android.content.Context;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vn.phuong.iptvplayer.movie.MovieModels.MovieDetail;
import vn.phuong.iptvplayer.movie.MovieModels.MovieEpisode;
import vn.phuong.iptvplayer.movie.MovieModels.MovieItem;
import vn.phuong.iptvplayer.movie.MovieModels.Playback;

public final class Film4kSource implements MovieSource {
    private static final String[] BASES={"https://fiml4k.fun","https://film4k.annnekkk.com","https://film4k.net"};
    private final ExecutorService io=Executors.newFixedThreadPool(2);
    private volatile String workingBase=BASES[0];

    @Override public String id(){return "film4k";}
    @Override public String name(){return "Film4k";}
    @Override public boolean isPlugin(){return false;}

    private Map<String,String> headers(){
        Map<String,String> h=new LinkedHashMap<>();
        h.put("Referer",workingBase+"/"); h.put("Origin",workingBase);
        return h;
    }

    private String fetchFirst(String path) throws Exception {
        Exception last=null;
        String[] tries={workingBase};
        List<String> all=new ArrayList<>(); Collections.addAll(all,BASES); all.remove(workingBase); all.add(0,workingBase);
        for(String base:all){
            try{
                String url=base+path;
                String r=MovieHttp.get(url,headersFor(base));
                if(r!=null&&!r.trim().isEmpty()){workingBase=base;return r;}
            }catch(Exception e){last=e;}
        }
        throw new Exception(last==null?"Không kết nối được Film4k":last.getMessage());
    }

    private Map<String,String> headersFor(String base){
        Map<String,String> h=new LinkedHashMap<>(); h.put("Referer",base+"/"); h.put("Origin",base); return h;
    }

    @Override public void loadHome(Callback<List<MovieItem>> cb){
        io.execute(()->{
            try{
                String json=fetchFirst("/api/home?");
                List<MovieItem> items=MovieJson.parseList(json,workingBase,id());
                if(items.isEmpty()) throw new Exception("Film4k không trả về danh sách phim");
                cb.onSuccess(items);
            }catch(Exception e){cb.onError("Film4k: "+safe(e));}
        });
    }

    @Override public void search(String query, Callback<List<MovieItem>> cb){
        io.execute(()->{
            try{
                String q=URLEncoder.encode(query,StandardCharsets.UTF_8.name());
                String json;
                try{json=fetchFirst("/api/search?search="+q);}catch(Exception e){json=fetchFirst("/api/home?search="+q);}
                List<MovieItem> items=MovieJson.parseList(json,workingBase,id());
                cb.onSuccess(items);
            }catch(Exception e){cb.onError("Không tìm thấy: "+safe(e));}
        });
    }

    @Override public void loadDetail(MovieItem item, Callback<MovieDetail> cb){
        io.execute(()->{
            try{
                String detail=item.detailUrl;
                if(detail==null||detail.isEmpty()) detail=workingBase+"/api/title/"+URLEncoder.encode(item.id,StandardCharsets.UTF_8.name());
                String json=MovieHttp.get(detail,headers());
                cb.onSuccess(MovieJson.parseDetail(json,workingBase,item));
            }catch(Exception e){cb.onError("Không tải được chi tiết phim: "+safe(e));}
        });
    }

    @Override public void loadPlayback(MovieEpisode ep, Callback<Playback> cb){
        io.execute(()->{
            try{
                if(ep.directUrl!=null&&!ep.directUrl.isEmpty()){
                    cb.onSuccess(new Playback(ep.directUrl,ep.directUrl.contains(".mpd")?"application/dash+xml":"application/x-mpegURL",headers(),Collections.emptyList()));
                    return;
                }
                String req=ep.id;
                if(req==null||req.isEmpty()) throw new Exception("Thiếu URL xem phim");
                String json=MovieHttp.get(req,headers());
                Playback p=MovieJson.parsePlayback(json,req,workingBase);
                if(p==null){
                    String ticket=MovieJson.findTicket(new org.json.JSONTokener(json).nextValue());
                    if(ticket!=null&&!ticket.isEmpty()){
                        String enc=URLEncoder.encode(ticket,StandardCharsets.UTF_8.name());
                        String r=MovieHttp.get(workingBase+"/api/play-ticket?ticket="+enc,headers());
                        p=MovieJson.parsePlayback(r,workingBase+"/api/play-ticket?ticket="+enc,workingBase);
                        if(p==null){
                            String body=new org.json.JSONObject().put("ticket",ticket).toString();
                            r=MovieHttp.postJson(workingBase+"/api/play-ticket",headers(),body);
                            p=MovieJson.parsePlayback(r,workingBase+"/api/play-ticket",workingBase);
                        }
                    }
                }
                if(p==null) throw new Exception("API không trả về link phát HLS/DASH");
                cb.onSuccess(p);
            }catch(Exception e){cb.onError("Không lấy được luồng phim: "+safe(e));}
        });
    }

    private static String safe(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
    @Override public void close(){io.shutdownNow();}
}