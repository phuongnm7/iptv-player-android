package vn.phuong.iptvplayer.movie;

import android.webkit.JavascriptInterface;
import android.os.Handler;
import android.os.Looper;
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
    private boolean ready;
    private boolean destroyed;
    private String initError;

    public MovieJsRuntime(android.content.Context context, String pluginScript) {
        android.content.Context visualContext = context;
        webView = new WebView(visualContext);

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
                evalRaw(
                        "(function(){try{" +
                                pluginScript +
                                "\n;NM7Bridge.scriptReady('');" +
                                "}catch(e){NM7Bridge.scriptError(String(e && e.stack ? e.stack : e));}})();",
                        new Callback() {
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
                        }
                );
            }

            @Override public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                initError = "WebView: " + description;
                ready = false;
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
                // Keep the failure deterministic instead of silently queueing forever.
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
                cb.error("Không khởi tạo được plugin JS: " + (initError == null ? "không rõ lỗi" : initError));
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
                            "{NM7Bridge.callbackError(id,'Plugin không có hàm " + escapeJs(function) + "');return;}" +
                            "var r=" + function + "(" + args + ");" +
                            "Promise.resolve(r).then(function(v){" +
                            "NM7Bridge.callbackDone(id,v==null?'':String(v));" +
                            "},function(e){" +
                            "NM7Bridge.callbackError(id,String(e&&e.stack?e.stack:e));" +
                            "});" +
                            "}catch(e){NM7Bridge.callbackError(id,String(e&&e.stack?e.stack:e));}" +
                            "})();";

            webView.evaluateJavascript(expression, ignored -> {});
        });
    }

    private void evalRaw(String expr, Callback cb) {
        main.post(() -> {
            if (destroyed) {
                cb.error("JS runtime đã đóng");
                return;
            }
            webView.evaluateJavascript(expr, value -> {
                // The bridge is authoritative for plugin bootstrap and async calls.
                if (value == null || "null".equals(value)) {
                    // Bootstrap signals through NM7Bridge; ordinary eval can legitimately return null.
                    if (ready) cb.done("");
                    return;
                }
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
                ready = true;
                initError = null;
            });
        }

        @JavascriptInterface
        public void scriptError(String message) {
            main.post(() -> {
                if (destroyed) return;
                ready = false;
                initError = message == null ? "Plugin JS lỗi" : message;
                while (!pending.isEmpty()) pending.remove().run();
            });
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
            return """";
        }
    }

    public void destroy() {
        main.post(() -> {
            destroyed = true;
            ready = false;
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