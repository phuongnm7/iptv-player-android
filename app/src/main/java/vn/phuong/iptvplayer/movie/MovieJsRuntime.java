package vn.phuong.iptvplayer.movie;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONTokener;
import org.json.JSONObject;

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
    private final Context context;
    private final WebView webView;
    private final String pluginScript;
    private final String originUrl;
    private final Queue<Runnable> pending = new ArrayDeque<>();
    private final Map<String, Callback> callbacks = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    private Callback bootstrapCallback;
    private boolean ready;
    private boolean destroyed;
    private String initError;

    private Dialog cloudflareDialog;
    private WebView cloudflareWebView;
    private Runnable cloudflarePoller;
    private final java.util.ArrayDeque<Callback> cloudflareWaiters = new java.util.ArrayDeque<>();

    public MovieJsRuntime(android.content.Context context, String pluginScript) {
        this(context, pluginScript, "https://nm7.local/");
    }

    public MovieJsRuntime(android.content.Context context, String pluginScript, String originUrl) {
        this.context = context;
        this.pluginScript = pluginScript == null ? "" : pluginScript;
        this.originUrl = originUrl == null ? "" : originUrl.trim();
        webView = new WebView(context);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36");
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        CookieManager.getInstance().flush();
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

                if (originUrl.toLowerCase().contains("novahd.cc")) {
                    waitForBrowserChallenge(0);
                } else {
                    bootstrapPlugin();
                }
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

        main.post(() -> {
            String origin = originUrl == null ? "" : originUrl.trim();
            if (origin.startsWith("http://") || origin.startsWith("https://")) {
                webView.loadUrl(origin);
            } else {
                webView.loadDataWithBaseURL(
                        "https://nm7.local/",
                        "<html><body></body></html>",
                        "text/html",
                        "UTF-8",
                        null
                );
            }
        });
    }

    private void waitForBrowserChallenge(int attempt) {
        if (destroyed) return;
        String probe =
                "(function(){" +
                        "var t=(document.title||'').toLowerCase();" +
                        "var b=(document.body?document.body.innerText:'').toLowerCase();" +
                        "var s=t+' '+b;" +
                        "return JSON.stringify({" +
                            "challenge:/just a moment|attention required|cloudflare/.test(s)," +
                            "title:document.title||'',href:location.href||''" +
                        "});" +
                "})()";
        webView.evaluateJavascript(probe, value -> {
            boolean challenge = false;
            try {
                String json = String.valueOf(new JSONTokener(value).nextValue());
                JSONObject o = new JSONObject(json);
                challenge = o.optBoolean("challenge", false);
            } catch (Exception ignored) {}

            if (challenge) {
                // The hidden WebView is deliberately not used to solve the challenge.
                // Cloudflare may require a visible browser context and user interaction.
                solveNovaCloudflare(new Callback() {
                    @Override public void done(String ignored) {
                        if (!destroyed) bootstrapPlugin();
                    }

                    @Override public void error(String message) {
                        if (bootstrapCallback != null) bootstrapCallback.error(message);
                    }
                });
                return;
            }
            bootstrapPlugin();
        });
    }

    private void bootstrapPlugin() {
        String script = pluginScript;
        // Load the compatibility bridge first, then evaluate the plugin in the
        // page's global scope. Top-level plugin functions must remain global so
        // getManifest/getUrlSearch/getUrlDetail/... can be called later.
        evalRaw(
                "(function(){try{" +
                        pluginCompatibilityLayer() +
                        "\nwindow.eval(" + quote(script) + ");" +
                        "\nNM7Bridge.scriptReady('');" +
                        "}catch(e){NM7Bridge.scriptError(String(e&&e.stack?e.stack:e));}})();",
                new Callback() {
                    @Override public void done(String ignored) {}
                    @Override public void error(String message) {
                        if (bootstrapCallback != null) bootstrapCallback.error(message);
                    }
                }
        );
    }

    /**
     * Opens a visible browser challenge for NovaHD. The user completes any
     * Cloudflare verification; CookieManager then shares the resulting
     * session with the hidden plugin WebView.
     */
    public void solveNovaCloudflare(Callback cb) {
        main.post(() -> {
            if (destroyed) {
                cb.error("JS runtime đã đóng");
                return;
            }

            if (!(context instanceof Activity)) {
                cb.error("NovaHD đang yêu cầu xác minh Cloudflare; môi trường hiện tại không có Activity để mở trang xác minh.");
                return;
            }

            Activity activity = (Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                cb.error("Không thể mở xác minh NovaHD vì Activity đã đóng.");
                return;
            }

            cloudflareWaiters.add(cb);
            if (cloudflareDialog != null && cloudflareDialog.isShowing()) {
                return;
            }

            final long startedAt = System.currentTimeMillis();
            LinearLayout root = new LinearLayout(activity);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(24, 18, 24, 12);
            root.setBackgroundColor(Color.rgb(20, 20, 31));

            TextView title = new TextView(activity);
            title.setText("Xác minh NovaHD");
            title.setTextColor(Color.WHITE);
            title.setTextSize(19f);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, 0, 0, 8);
            root.addView(title, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView info = new TextView(activity);
            info.setText("Hoàn tất xác minh Cloudflare nếu được yêu cầu. Cửa sổ sẽ tự đóng sau khi nhận được phiên xác minh.");
            info.setTextColor(Color.LTGRAY);
            info.setTextSize(13f);
            info.setGravity(Gravity.CENTER);
            info.setPadding(0, 0, 0, 10);
            root.addView(info, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            ProgressBar progress = new ProgressBar(activity);
            root.addView(progress, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 6));

            WebView browser = new WebView(activity);
            WebSettings bs = browser.getSettings();
            bs.setJavaScriptEnabled(true);
            bs.setDomStorageEnabled(true);
            bs.setDatabaseEnabled(true);
            bs.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
            bs.setJavaScriptCanOpenWindowsAutomatically(true);
            bs.setSupportMultipleWindows(false);
            bs.setUserAgentString("Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36");
            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            cm.setAcceptThirdPartyCookies(browser, true);
            cm.flush();
            browser.setWebChromeClient(new WebChromeClient());
            browser.setWebViewClient(new WebViewClient() {});
            cloudflareWebView = browser;

            root.addView(browser, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            Dialog dialog = new Dialog(activity);
            dialog.setTitle("NovaHD");
            dialog.setContentView(root);
            dialog.setOnDismissListener(d -> {
                if (cloudflarePoller != null) {
                    main.removeCallbacks(cloudflarePoller);
                    cloudflarePoller = null;
                }
                if (cloudflareWebView != null) {
                    cloudflareWebView.stopLoading();
                    cloudflareWebView.destroy();
                    cloudflareWebView = null;
                }
                cloudflareDialog = null;
            });
            cloudflareDialog = dialog;

            dialog.setOnCancelListener(d -> finishCloudflare(false, "NovaHD Cloudflare chưa được xác minh."));
            dialog.show();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(
                        (int)(activity.getResources().getDisplayMetrics().widthPixels * 0.94f),
                        (int)(activity.getResources().getDisplayMetrics().heightPixels * 0.82f));
            }

            CookieManager.getInstance().removeExpiredCookie();
            browser.loadUrl("https://novahd.cc/");

            cloudflarePoller = new Runnable() {
                @Override public void run() {
                    if (destroyed) {
                        finishCloudflare(false, "JS runtime đã đóng");
                        return;
                    }

                    CookieManager.getInstance().flush();
                    String cookies = CookieManager.getInstance().getCookie("https://novahd.cc/");
                    if (cookies != null && cookies.contains("cf_clearance")) {
                        info.setText("Đã xác minh NovaHD. Đang tiếp tục tải phim…");
                        finishCloudflare(true, "");
                        return;
                    }

                    if (System.currentTimeMillis() - startedAt >= 120000L) {
                        finishCloudflare(false, "Xác minh NovaHD hết thời gian.");
                        return;
                    }

                    main.postDelayed(this, 1000L);
                }
            };
            main.post(cloudflarePoller);
        });
    }

    private void finishCloudflare(boolean success, String error) {
        main.post(() -> {
            if (cloudflarePoller != null) {
                main.removeCallbacks(cloudflarePoller);
                cloudflarePoller = null;
            }
            Dialog d = cloudflareDialog;
            cloudflareDialog = null;
            WebView w = cloudflareWebView;
            cloudflareWebView = null;
            if (w != null) {
                w.stopLoading();
                w.destroy();
            }
            if (d != null && d.isShowing()) d.dismiss();

            while (!cloudflareWaiters.isEmpty()) {
                Callback waiter = cloudflareWaiters.remove();
                try {
                    if (success) waiter.done("");
                    else waiter.error(error == null ? "NovaHD Cloudflare chưa được xác minh." : error);
                } catch (Exception ignored) {}
            }
        });
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

    /**
     * Fetches a URL from the WebView's real browser context. This is used for
     * Cloudflare-protected movie APIs such as NovaHD so requests carry the
     * WebView cookies/browser fingerprint instead of Java HttpURLConnection.
     */
    public void webGet(String url, Map<String, String> headers, Callback cb) {
        whenReady(() -> {
            if (destroyed) {
                cb.error("JS runtime đã đóng");
                return;
            }
            if (!ready) {
                cb.error("Không khởi tạo được WebView: " +
                        (initError == null ? "không rõ lỗi" : initError));
                return;
            }

            String id = "w" + sequence.incrementAndGet();
            callbacks.put(id, cb);

            org.json.JSONObject jsonHeaders = new org.json.JSONObject();
            if (headers != null) {
                for (Map.Entry<String, String> e : headers.entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) {
                        try {
                            jsonHeaders.put(e.getKey(), e.getValue());
                        } catch (org.json.JSONException ignored) {
                            // Ignore an invalid header entry; the browser supplies its own
                            // restricted request headers (Origin/Referer/User-Agent/etc.).
                        }
                    }
                }
            }

            String expression =
                    "(async function(){" +
                            "var id=" + quote(id) + ";" +
                            "var url=" + quote(url) + ";" +
                            "var supplied=JSON.parse(" + quote(jsonHeaders.toString()) + ");" +
                            "var h={};" +
                            "Object.keys(supplied).forEach(function(k){" +
                                "if(!/^(origin|referer|user-agent|sec-)/i.test(k)) h[k]=supplied[k];" +
                            "});" +
                            "for(var attempt=0;attempt<4;attempt++){" +
                                "try{" +
                                    "var r=await window.fetch(url,{" +
                                        "method:'GET',headers:h,credentials:'include'," +
                                        "cache:'no-store',redirect:'follow'" +
                                    "});" +
                                    "var body=await r.text();" +
                                    "if(r.status>=200 && r.status<300){" +
                                        "NM7Bridge.callbackDone(id,body);return;" +
                                    "}" +
                                    "if(r.status!==403 || attempt>=3){" +
                                        "var detail=String(body||'').replace(/\\s+/g,' ').trim();" +
                                        "if(detail.length>500) detail=detail.substring(0,500);" +
                                        "NM7Bridge.callbackError(id,'HTTP '+r.status+' — '+detail);return;" +
                                    "}" +
                                    "await new Promise(function(resolve){setTimeout(resolve,1200);});" +
                                "}catch(e){" +
                                    "if(attempt>=3){" +
                                        "NM7Bridge.callbackError(id,String(e&&e.stack?e.stack:e));return;" +
                                    "}" +
                                    "await new Promise(function(resolve){setTimeout(resolve,800);});" +
                                "}" +
                            "}" +
                            "NM7Bridge.callbackError(id,'WebView fetch thất bại');" +
                    "})();";

            webView.evaluateJavascript(expression, ignored -> {});
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

        private Map<String,String> bridgeHeaders(String url, String json) {
            Map<String,String> out = parseHeaders(json);
            if (url != null && url.contains("novahd.cc")) {
                Map<String,String> defaults = MovieHttp.novaHeaders(url);
                defaults.putAll(out);
                return defaults;
            }
            return out;
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
            if (cloudflarePoller != null) {
                main.removeCallbacks(cloudflarePoller);
                cloudflarePoller = null;
            }
            if (cloudflareWebView != null) {
                cloudflareWebView.stopLoading();
                cloudflareWebView.destroy();
                cloudflareWebView = null;
            }
            if (cloudflareDialog != null && cloudflareDialog.isShowing()) {
                cloudflareDialog.dismiss();
            }
            cloudflareDialog = null;
            while (!cloudflareWaiters.isEmpty()) {
                try { cloudflareWaiters.remove().error("JS runtime đã đóng"); } catch (Exception ignored) {}
            }
            webView.stopLoading();
            webView.removeJavascriptInterface("NM7Bridge");
            webView.destroy();
        });
    }
}