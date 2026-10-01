package vn.phuong.iptvplayer.movie;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MovieModels {
    private MovieModels() {}

    public static final class MovieItem {
        public final String id, title, posterUrl, backdropUrl, quality, year, sourceId, detailUrl;
        public final boolean show;
        public MovieItem(String id, String title, String posterUrl, String backdropUrl, String quality,
                         String year, String sourceId, boolean show, String detailUrl) {
            this.id=id; this.title=title; this.posterUrl=posterUrl; this.backdropUrl=backdropUrl;
            this.quality=quality; this.year=year; this.sourceId=sourceId; this.show=show; this.detailUrl=detailUrl;
        }
    }

    public static final class MovieEpisode {
        public final String id, name, slug, directUrl;
        public final int season, episode;
        public MovieEpisode(String id, String name, String slug, int season, int episode, String directUrl) {
            this.id=id; this.name=name; this.slug=slug; this.season=season; this.episode=episode; this.directUrl=directUrl;
        }
    }

    public static final class MovieDetail {
        public final MovieItem movie;
        public final String description, category, cast, director, rating;
        public final List<MovieEpisode> episodes;
        public MovieDetail(MovieItem movie, String description, String category, String cast,
                           String director, String rating, List<MovieEpisode> episodes) {
            this.movie=movie; this.description=description; this.category=category;
            this.cast=cast; this.director=director; this.rating=rating;
            this.episodes=episodes == null ? new ArrayList<MovieEpisode>() : episodes;
        }
    }

    public static final class Subtitle {
        public final String url, lang, label;
        public Subtitle(String url, String lang, String label) {
            this.url=url; this.lang=lang; this.label=label;
        }
    }

    public static final class Playback {
        public final String url, mime;
        public final Map<String,String> headers;
        public final List<Subtitle> subtitles;
        public Playback(String url, String mime, Map<String,String> headers, List<Subtitle> subtitles) {
            this.url=url; this.mime=mime;
            this.headers=headers == null ? new LinkedHashMap<String,String>() : headers;
            this.subtitles=subtitles == null ? new ArrayList<Subtitle>() : subtitles;
        }
    }
}