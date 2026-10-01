package vn.phuong.iptvplayer.movie;

import android.os.Handler;
import android.os.Looper;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONTokener;
import java.util.ArrayDeque;
import java.util.Queue;

public final class MovieJsRuntime {
    public interface Callback { void done(String value); void error(String message); }
    private final Handler main=new Handler(Looper.getMainLooper());
    private final WebView webView;
    private final Queue<Runnable> pending=new ArrayDeque<>();
    private boolean ready;
    private boolean destroyed;

    public MovieJsRuntime(android.content.Context context,String pluginScript){
        webView=new WebView(context.getApplicationContext());
        WebSettings s=webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        webView.setVisibility(WebView.GONE);
        webView.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){
                eval(pluginScript+"\n;void(0);",new Callback(){
                    @Override public void done(String value){
                        ready=true;
                        while(!pending.isEmpty()) pending.remove().run();
                    }
                    @Override public void error(String message){
                        while(!pending.isEmpty()) pending.remove().run();
                    }
                });
            }
        });
        main.post(()->webView.loadDataWithBaseURL("https://nm7.local/","<html><body></body></html>","text/html","UTF-8",null));
    }

    public void whenReady(Runnable r){
        main.post(()->{
            if(destroyed)return;
            if(ready)r.run();else pending.add(r);
        });
    }

    public void call(String function, Callback cb, String... jsArgs){
        whenReady(()->{
            StringBuilder b=new StringBuilder("(typeof ").append(function).append("==='function')?").append(function).append("(");
            for(int i=0;i<jsArgs.length;i++){if(i>0)b.append(",");b.append(jsArgs[i]);}
            b.append("):null;");
            eval(b.toString(),cb);
        });
    }

    private void eval(String expr,Callback cb){
        main.post(()->{
            if(destroyed){cb.error("JS runtime đã đóng");return;}
            webView.evaluateJavascript(expr,value->{
                try{
                    if(value==null||"null".equals(value)){cb.error("JS trả về null");return;}
                    Object decoded=new JSONTokener(value).nextValue();
                    cb.done(decoded==null?"":String.valueOf(decoded));
                }catch(Exception e){cb.error("JS parse: "+e.getMessage());}
            });
        });
    }

    public static String quote(String s){
        if(s==null)return "null";
        try{return org.json.JSONObject.quote(s);}catch(Exception e){return "\\"\\"";}
    }

    public void destroy(){main.post(()->{destroyed=true;ready=false;pending.clear();webView.stopLoading();webView.destroy();});}
}