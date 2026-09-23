package vn.phuong.iptvplayer.movie;

import android.content.Context;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public interface MoviePlugin {
    String id();
    String name();
    void home(Context context, Callback callback);
    void search(Context context, String query, Callback callback);
    void detail(Context context, String contentId, Callback callback);
    void sources(Context context, String contentId, String season, String episode, Callback callback);
    interface Callback { void onSuccess(Object data); void onError(Throwable error); }
    Executor EXECUTOR = Executors.newCachedThreadPool();
}