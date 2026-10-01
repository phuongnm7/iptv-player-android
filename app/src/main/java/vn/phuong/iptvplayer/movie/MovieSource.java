package vn.phuong.iptvplayer.movie;

import vn.phuong.iptvplayer.movie.MovieModels.MovieDetail;
import vn.phuong.iptvplayer.movie.MovieModels.MovieEpisode;
import vn.phuong.iptvplayer.movie.MovieModels.MovieItem;
import vn.phuong.iptvplayer.movie.MovieModels.Playback;

public interface MovieSource {
    interface Callback<T> {
        void onSuccess(T value);
        void onError(String message);
    }
    String id();
    String name();
    boolean isPlugin();
    void loadHome(Callback<java.util.List<MovieItem>> callback);
    void search(String query, Callback<java.util.List<MovieItem>> callback);
    void loadDetail(MovieItem item, Callback<MovieDetail> callback);
    void loadPlayback(MovieEpisode episode, Callback<Playback> callback);
    void close();
}