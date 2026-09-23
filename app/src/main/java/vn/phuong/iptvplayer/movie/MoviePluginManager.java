package vn.phuong.iptvplayer.movie;
import android.content.Context; import java.util.ArrayList; import java.util.Collections; import java.util.List;
public final class MoviePluginManager {
 private static final List<MoviePlugin> PLUGINS=new ArrayList<>();
 private MoviePluginManager(){}
 public static synchronized void register(MoviePlugin p){if(p==null)return; for(MoviePlugin x:PLUGINS)if(x.id().equals(p.id()))return; PLUGINS.add(p);}
 public static synchronized void unregister(String id){if(id==null)return; PLUGINS.removeIf(p->id.equals(p.id()));}
 public static synchronized List<MoviePlugin> all(){return Collections.unmodifiableList(new ArrayList<>(PLUGINS));}
 public static synchronized MoviePlugin find(String id){if(id==null)return null; for(MoviePlugin p:PLUGINS)if(id.equals(p.id()))return p; return null;}
 public static synchronized void registerDefaults(){if(find("novahd")==null)register(new NovaHdPlugin());}
 public static String contentId(String pluginId,String id){return pluginId+":"+(id==null?"":id);}
 public static String[] splitContentId(String id){if(id==null)return new String[]{"",""}; int p=id.indexOf(':'); return p<=0?new String[]{"",id}:new String[]{id.substring(0,p),id.substring(p+1)};}
}