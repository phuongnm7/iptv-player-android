package vn.phuong.iptvplayer.movie;

import android.os.Handler;
import android.os.Looper;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONTokener;

public final class MovieJsRuntime {
    public interface Callback { void done(String value); void error(String message); }
    private final Handler main=new Handler(Looper.getMainLooper());
    private final WebView webView;
    private boolean ready;
    private String script;
    private Runnable readyAction;

    public MovieJsRuntime(android.content.Context context,String pluginScript){
        webView=new WebView(context);
        WebSettings s=webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        webView.setVisibility(WebView.GONE);
        webView.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){
                eval(pluginScript+"\n;void(0);",new Callback(){
                    @Override public void done(String value){ready=true;if(readyAction!=null){Runnable r=readyAction;readyAction=null;r.run();}}
                    @Override public void error(String message){}
                });
            }
        });
        main.post(()->webView.loadDataWithBaseURL("https://nm7.local/","<html><body></body></html>","text/html","UTF-8",null));
        script=pluginScript;
    }

    public void whenReady(Runnable r){main.post(()->{if(ready)r.run();else readyAction=r;});}

    public void call(String function,String... jsArgs,Callback cb){
        whenReady(()->{
            StringBuilder b=new StringBuilder("(typeof ").append(function).append("==='function')?").append(function).append("(");
            for(int i=0;i<jsArgs.length;i++){if(i>0)b.append(",");b.append(jsArgs[i]);}
            b.append("):null;");
            eval(b.toString(),cb);
        });
    }

    private void eval(String expr,Callback cb){
        main.post(()->webView.evaluateJavascript(expr,value->{
            try{
                if(value==null||"null".equals(value)){cb.error("JS trả về null");return;}
                Object decoded=new JSONTokener(value).nextValue();
                cb.done(decoded==null? "":String.valueOf(decoded));
            }catch(Exception e){cb.error("JS parse: "+e.getMessage());}
        }));
    }

    public static String quote(String s){
        if(s==null)return "null";
        try{return org.json.JSONObject.quote(s);}catch(Exception e){return """";}
    }

    public void destroy(){main.post(()->{ready=false;webView.stopLoading();webView.destroy();});}
}