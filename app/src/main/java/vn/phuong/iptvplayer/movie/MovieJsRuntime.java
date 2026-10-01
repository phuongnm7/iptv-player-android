package vn.phuong.iptvplayer.movie;

import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONTokener;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class MovieJsRuntime {
    public interface Callback {
        void done(String value);
        void error(String message);
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private final WebView webView;
    private final Queue<Runnable> pending = new ArrayDeque<>();
    private final Map<String, Callback> callbacks = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    private Callback bootstrapCallback;
    private boolean ready;
    private boolean destroyed;
    private String initError;

    public MovieJsRuntime(android.content.Context context, String pluginScript) {
        webView = new WebView(context);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        webView.setVisibility(android.view.View.GONE);

        webView.addJavascriptInterface(new Bridge(), "NM7Bridge");
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                bootstrapCallback = new Callback() {
                    @Override public void done(String ignored) {
                        ready = true;
                        initError = null;
                        while (!pending.isEmpty()) pending.remove().run();
                    }

                    @Override public void error(String message) {
                        ready = false;
                        initError = message;
                        while (!pending.isEmpty()) pending.remove().run();
                    }
                };

                String script = pluginScript == null ? "" : pluginScript;
                evalRaw(
                        "(function(){try{" +
                                script +
                                "\n;NM7Bridge.scriptReady('');" +
                                "}catch(e){NM7Bridge.scriptError(String(e&&e.stack?e.stack:e));}})();",
                        new Callback() {
                            @Override public void done(String ignored) {}
                            @Override public void error(String message) {
                                if (bootstrapCallback != null) bootstrapCallback.error(message);
                            }
                        }
                );
            }

            @Override public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                initError = "WebView: " + description;
                ready = false;
                if (bootstrapCallback != null) {
                    Callback cb = bootstrapCallback;
                    bootstrapCallback = null;
                    cb.error(initError);
                }
            }
        });

        main.post(() -> webView.loadDataWithBaseURL(
                "https://nm7.local/",
                "<html><body></body></html>",
                "text/html",
                "UTF-8",
                null
        ));
    }

    public void whenReady(Runnable r) {
        main.post(() -> {
            if (destroyed) return;
            if (ready) {
                r.run();
            } else if (initError != null) {
                r.run();
            } else {
                pending.add(r);
            }
        });
    }

    public void call(String function, Callback cb, String... jsArgs) {
        whenReady(() -> {
            if (destroyed) {
                cb.error("JS runtime đã đóng");
                return;
            }
            if (!ready) {
                cb.error("Không khởi tạo được plugin JS: " +
                        (initError == null ? "không rõ lỗi" : initError));
                return;
            }

            String id = "c" + sequence.incrementAndGet();
            callbacks.put(id, cb);

            StringBuilder args = new StringBuilder();
            for (int i = 0; i < jsArgs.length; i++) {
                if (i > 0) args.append(",");
                args.append(jsArgs[i]);
            }

            String expression =
                    "(function(){" +
                            "var id=" + quote(id) + ";" +
                            "try{" +
                            "if(typeof " + function + " !== 'function')" +
                            "{NM7Bridge.callbackError(id," +
                            quote("Plugin không có hàm " + escapeJs(function)) +
                            ");return;}" +
                            "var r=" + function + "(" + args + ");" +
                            "Promise.resolve(r).then(function(v){" +
                            "var out=(v==null)?'':((typeof v==='string'||typeof v==='number'||typeof v==='boolean')?String(v):JSON.stringify(v));" +
                            "NM7Bridge.callbackDone(id,out);" +
                            "},function(e){" +
                            "NM7Bridge.callbackError(id,String(e&&e.stack?e.stack:e));" +
                            "});" +
                            "}catch(e){NM7Bridge.callbackError(id,String(e&&e.stack?e.stack:e));}" +
                            "})();";

            webView.evaluateJavascript(expression, ignored -> {});
        });
    }


    private String pluginCompatibilityLayer() {
        return String.join("\n",
                "window.NM7MapToJson=function(m){try{if(m&&m._data)return JSON.stringify(m._data);return JSON.stringify(m||{});}catch(e){return '{}';}};",
                "window.java=window.java||{};java.util=java.util||{};java.util.HashMap=function(){this._data={};this.put=function(k,v){this._data[String(k)]=String(v);};this.get=function(k){return this._data[String(k)];};this.toString=function(){return '[object HashMap]';};};",
                "window.com={liskovsoft:{smartyoutubetv2:{common:{plugin:{api:{PluginApiClient:{INSTANCE:{fetchContentString:function(url,map){return NM7Bridge.httpGet(String(url),window.NM7MapToJson(map));}}}}}}}}};",
                "window.fetch=window.fetch||function(url,opts){opts=opts||{};var h=opts.headers||{};var method=String(opts.method||'GET').toUpperCase();var body=opts.body==null?null:String(opts.body);var raw=method==='POST'?NM7Bridge.httpPost(String(url),JSON.stringify(h),body):NM7Bridge.httpGet(String(url),JSON.stringify(h));var textValue=String(raw==null?'':raw);return Promise.resolve({ok:true,status:200,url:String(url),text:function(){return Promise.resolve(textValue);},json:function(){return Promise.resolve(JSON.parse(textValue));},headers:{get:function(){return null;}}});};",
                "window.axios=window.axios||{get:function(url,cfg){cfg=cfg||{};var h=cfg.headers||{};var body=NM7Bridge.httpGet(String(url),JSON.stringify(h));return Promise.resolve({data:JSON.parse(body),status:200,statusText:'OK',headers:{},config:cfg});},post:function(url,data,cfg){cfg=cfg||{};var h=cfg.headers||{};var body=NM7Bridge.httpPost(String(url),JSON.stringify(h),typeof data==='string'?data:JSON.stringify(data));return Promise.resolve({data:JSON.parse(body),status:200,statusText:'OK',headers:{},config:cfg});}};";
                "window.NM7Http={get:function(url,headers){return NM7Bridge.httpGet(String(url),JSON.stringify(headers||{}));},post:function(url,headers,body){return NM7Bridge.httpPost(String(url),JSON.stringify(headers||{}),String(body==null?'':body));}};"
        );
    }

    private void evalRaw(String expr, Callback cb) {
        main.post(() -> {
            if (destroyed) {
                cb.error("JS runtime đã đóng");
                return;
            }
            webView.evaluateJavascript(expr, value -> {
                if (value == null || "null".equals(value)) return;
                try {
                    Object decoded = new JSONTokener(value).nextValue();
                    cb.done(decoded == null ? "" : String.valueOf(decoded));
                } catch (Exception e) {
                    cb.done(value);
                }
            });
        });
    }

    private final class Bridge {
        @JavascriptInterface
        public void scriptReady(String ignored) {
            main.post(() -> {
                if (destroyed) return;
                if (bootstrapCallback != null) {
                    Callback cb = bootstrapCallback;
                    bootstrapCallback = null;
                    cb.done("");
                } else {
                    ready = true;
                    initError = null;
                    while (!pending.isEmpty()) pending.remove().run();
                }
            });
        }

        @JavascriptInterface
        public void scriptError(String message) {
            main.post(() -> {
                if (destroyed) return;
                ready = false;
                initError = message == null ? "Plugin JS lỗi" : message;
                if (bootstrapCallback != null) {
                    Callback cb = bootstrapCallback;
                    bootstrapCallback = null;
                    cb.error(initError);
                } else {
                    while (!pending.isEmpty()) pending.remove().run();
                }
            });
        }

        @JavascriptInterface
        public String httpGet(String url, String headersJson) {
            try {
                return MovieHttp.get(url, bridgeHeaders(url, headersJson));
            } catch (Exception e) {
                return "";
            }
        }

        @JavascriptInterface
        public String httpPost(String url, String headersJson, String body) {
            try {
                return MovieHttp.postText(url, bridgeHeaders(url, headersJson), body == null ? "" : body);
            } catch (Exception e) {
                return "";
            }
        }

        private Map<String,String> parseHeaders(String json) {
            Map<String,String> out = new java.util.LinkedHashMap<>();
            if (json == null || json.trim().isEmpty()) return out;
            try {
                JSONObject o = new JSONObject(json);
                java.util.Iterator<String> it = o.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    Object v = o.opt(k);
                    if (v != null && v != JSONObject.NULL) out.put(k, String.valueOf(v));
                }
            } catch (Exception ignored) {}
            return out;
        }

        @JavascriptInterface
        public void callbackDone(String id, String value) {
            deliver(id, true, value == null ? "" : value);
        }

        @JavascriptInterface
        public void callbackError(String id, String message) {
            deliver(id, false, message == null ? "JS plugin lỗi" : message);
        }

        private void deliver(String id, boolean success, String value) {
            Callback cb = callbacks.remove(id);
            if (cb == null) return;
            main.post(() -> {
                if (destroyed) {
                    cb.error("JS runtime đã đóng");
                } else if (success) {
                    cb.done(value);
                } else {
                    cb.error(value);
                }
            });
        }
    }

    private static String escapeJs(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
    }

    public static String quote(String s) {
        if (s == null) return "null";
        try {
            return org.json.JSONObject.quote(s);
        } catch (Exception e) {
            return "\"\"";
        }
    }

    public void destroy() {
        main.post(() -> {
            destroyed = true;
            ready = false;
            bootstrapCallback = null;
            pending.clear();
            for (Callback cb : callbacks.values()) {
                try { cb.error("JS runtime đã đóng"); } catch (Exception ignored) {}
            }
            callbacks.clear();
            webView.stopLoading();
            webView.removeJavascriptInterface("NM7Bridge");
            webView.destroy();
        });
    }
}