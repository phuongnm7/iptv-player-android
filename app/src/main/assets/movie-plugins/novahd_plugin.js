// =============================================================================
// SuperOK Plugin - NovaHD (novahd.cc)
// Kho phim lẻ, phim bộ quốc tế chuẩn Full HD từ NovaHD
// Hỗ trợ stream HLS m3u8 trực tiếp & trích xuất đa phụ đề rời (VTT) trên ExoPlayer / Media3
// =============================================================================

var BASEURL = "https://novahd.cc";
var TMDB_IMG_POSTER = "https://image.tmdb.org/t/p/w500";
var TMDB_IMG_BACKDROP = "https://image.tmdb.org/t/p/w1280";

function getManifest() {
    return JSON.stringify({
        "id": "novahd",
        "name": "NovaHD",
        "version": "1.0.5",
        "description": "Kho phim lẻ, phim bộ quốc tế chất lượng cao Full HD từ NovaHD.cc, hỗ trợ ExoPlayer / Media3 phát trực tiếp m3u8 và đa phụ đề VTT.",
        "info": "Watch Movies & TV Series Free in HD on NOVA with multi-language subtitles.",
        "baseUrl": BASEURL,
        "iconUrl": BASEURL + "/icons/icon-192.png",
        "isEnabled": true,
        "type": "MOVIE",
        "playerType": "media3"
    });
}

function log(msg) {
    if (typeof console !== 'undefined' && console.log) {
        console.log("[novahd] " + msg);
    }
}

function httpGet(url, headers) {
    try {
        if (typeof com !== 'undefined' && com.liskovsoft && com.liskovsoft.smartyoutubetv2) {
            var client = com.liskovsoft.smartyoutubetv2.common.plugin.api.PluginApiClient.INSTANCE;
            if (headers) {
                try {
                    var map = new java.util.HashMap();
                    for (var k in headers) {
                        if (headers.hasOwnProperty(k)) map.put(String(k), String(headers[k]));
                    }
                    return String(client.fetchContentString(url, map) || "");
                } catch(me) {
                    return String(client.fetchContentString(url, null) || "");
                }
            }
            return String(client.fetchContentString(url, null) || "");
        }
    } catch(e) {
        log("httpGet error: " + e);
    }
    return "";
}

// ===== MENU & HOME SECTIONS =====

function getHomeSections() {
    return JSON.stringify([
        { "slug": "trending", "title": "🔥 Thịnh Hành (Trending)", "type": "Horizontal" },
        { "slug": "movies", "title": "🎬 Phim Lẻ Mới (Movies)", "type": "Horizontal" },
        { "slug": "shows", "title": "📺 Phim Bộ Mới (TV Shows)", "type": "Horizontal" },
        { "slug": "movies?genre=Action", "title": "💥 Phim Hành Động (Action)", "type": "Horizontal" },
        { "slug": "movies?genre=Science Fiction", "title": "🚀 Khoa Học Viễn Tưởng (Sci-Fi)", "type": "Horizontal" },
        { "slug": "movies?genre=Animation", "title": "🎨 Phim Hoạt Hình (Animation)", "type": "Horizontal" },
        { "slug": "movies?genre=Horror", "title": "👻 Phim Kinh Dị (Horror)", "type": "Horizontal" },
        { "slug": "movies?genre=Comedy", "title": "😂 Phim Hài Hước (Comedy)", "type": "Horizontal" },
        { "slug": "movies?genre=Adventure", "title": "🧭 Phim Phiêu Lưu (Adventure)", "type": "Horizontal" },
        { "slug": "shows?genre=Action%20%26%20Adventure", "title": "⚔️ Phim Bộ Hành Động", "type": "Horizontal" },
        { "slug": "shows?genre=Sci-Fi%20%26%20Fantasy", "title": "🌌 Phim Bộ Viễn Tưởng", "type": "Horizontal" },
        { "slug": "movies", "title": "Tất Cả Phim Lẻ", "type": "Grid" }
    ]);
}

function getLISTmenu() {
    return [
        { "link": "trending", "name": "Thịnh Hành (Trending)" },
        { "link": "movies", "name": "Phim Lẻ (Movies)" },
        { "link": "shows", "name": "Phim Bộ (TV Shows)" },
        { "link": "movies?genre=Action", "name": "Hành Động" },
        { "link": "movies?genre=Adventure", "name": "Phiêu Lưu" },
        { "link": "movies?genre=Animation", "name": "Hoạt Hình" },
        { "link": "movies?genre=Comedy", "name": "Hài Hước" },
        { "link": "movies?genre=Crime", "name": "Tội Phạm" },
        { "link": "movies?genre=Drama", "name": "Tâm Lý" },
        { "link": "movies?genre=Fantasy", "name": "Kỳ Ảo (Fantasy)" },
        { "link": "movies?genre=Horror", "name": "Kinh Dị" },
        { "link": "movies?genre=Mystery", "name": "Bí Ẩn" },
        { "link": "movies?genre=Romance", "name": "Lãng Mạn" },
        { "link": "movies?genre=Science Fiction", "name": "Khoa Học Viễn Tưởng" },
        { "link": "movies?genre=Thriller", "name": "Giật Gân / Thriller" },
        { "link": "movies?genre=War", "name": "Chiến Tranh" },
        { "link": "shows?genre=Action%20%26%20Adventure", "name": "Bộ: Hành Động & Phiêu Lưu" },
        { "link": "shows?genre=Sci-Fi%20%26%20Fantasy", "name": "Bộ: Viễn Tưởng & Kỳ Ảo" }
    ];
}

function getPrimaryCategories() {
    try {
        var menu = getLISTmenu();
        var result = [];
        for (var i = 0; i < menu.length; i++) {
            result.push({
                "name": menu[i].name,
                "slug": menu[i].link
            });
        }
        return JSON.stringify(result);
    } catch(e) {
        return "[]";
    }
}

function getFilterConfig() {
    try {
        var menu = getLISTmenu();
        var result = [];
        for (var i = 0; i < menu.length; i++) {
            result.push({
                "name": menu[i].name,
                "slug": menu[i].link
            });
        }
        return JSON.stringify({
            category: result
        });
    } catch(e) {
        return JSON.stringify({ category: [] });
    }
}

function parseCategoriesResponse(html) { return getPrimaryCategories(); }
function parseCountriesResponse(html) { return "[]"; }
function parseYearsResponse(html) { return "[]"; }

// ===== URL GENERATION =====

function getUrlList(slug, filtersJson) {
    try {
        var path = slug || "trending";
        var page = 1;

        if (typeof filtersJson === "number") {
            page = filtersJson;
        } else if (filtersJson) {
            try {
                var f = typeof filtersJson === "object" ? filtersJson : JSON.parse(filtersJson);
                if (f.page) page = parseInt(f.page, 10) || 1;
                if (f.category) {
                    if (Array.isArray(f.category) && f.category.length > 0) {
                        path = f.category[0].slug || f.category[0].link || path;
                    } else if (typeof f.category === "string") {
                        path = f.category;
                    }
                }
            } catch(e) {}
        }

        if (path.indexOf("http") === 0) {
            if (page > 1 && path.indexOf("page=") === -1) {
                path += (path.indexOf("?") > -1 ? "&page=" : "?page=") + page;
            }
            return path;
        }

        if (path === "trending" || path === "home" || path === "/" || path === "") {
            if (page > 1) {
                return BASEURL + "/api/movies?page=" + page;
            }
            return BASEURL + "/api/trending?type=all";
        }

        var isShow = path.indexOf("shows") !== -1;
        var endpoint = isShow ? "/api/shows" : "/api/movies";
        var genreParam = "";

        if (path.indexOf("genre=") !== -1) {
            var rawGenre = path.substring(path.indexOf("genre=") + 6);
            if (rawGenre.indexOf("&") !== -1) rawGenre = rawGenre.substring(0, rawGenre.indexOf("&"));
            genreParam = "&genre=" + encodeURIComponent(decodeURIComponent(rawGenre));
        }

        return BASEURL + endpoint + "?page=" + page + genreParam;
    } catch(e) {
        return BASEURL + "/api/trending?type=all";
    }
}

function getUrlSearch(keyword, filtersJson) {
    try {
        var page = 1;
        if (typeof filtersJson === "number") {
            page = filtersJson;
        } else if (filtersJson) {
            try {
                var f = typeof filtersJson === "object" ? filtersJson : JSON.parse(filtersJson);
                if (f.page) page = parseInt(f.page, 10) || 1;
            } catch(e) {}
        }
        var kw = encodeURIComponent(keyword || "");
        return BASEURL + "/api/search?search=" + kw + "&page=" + page;
    } catch(e) {
        return BASEURL + "/api/search?search=" + encodeURIComponent(keyword || "") + "&page=1";
    }
}

function getSearchUrl(keyword, page) {
    var p = typeof page === 'number' ? page : 1;
    return getUrlSearch(keyword, JSON.stringify({ page: p }));
}

function getUrlDetail(id) {
    if (!id) return "";
    if (id.indexOf("http") === 0) return id;

    if (id.indexOf("show/") === 0) {
        var showId = id.replace("show/", "");
        return BASEURL + "/api/shows/" + showId;
    }
    if (id.indexOf("movie/") === 0) {
        var movieId = id.replace("movie/", "");
        return BASEURL + "/api/movies/" + movieId;
    }

    return BASEURL + "/api/movies/" + id.replace(/[^\d]/g, '');
}

function getUrlCategories() { return ""; }
function getUrlCountries() { return ""; }
function getUrlYears() { return ""; }

// ===== PARSE LIST RESPONSE =====

function textValue(v) {
    if (v == null) return "";
    if (typeof v === "string" || typeof v === "number" || typeof v === "boolean") return String(v).trim();
    if (Array.isArray(v)) {
        var arr = [];
        for (var i = 0; i < v.length; i++) {
            var x = textValue(v[i]);
            if (x) arr.push(x);
        }
        return arr.join(", ");
    }
    if (typeof v === "object") {
        var keys = ["vi", "vn", "name", "title", "originalTitle", "original_title", "en", "original", "url", "src", "file", "path", "value"];
        for (var i = 0; i < keys.length; i++) {
            if (v[keys[i]] != null) {
                var x = textValue(v[keys[i]]);
                if (x) return x;
            }
        }
        for (var k in v) {
            if (v.hasOwnProperty(k)) {
                var y = textValue(v[k]);
                if (y) return y;
            }
        }
    }
    return "";
}

function imageValue(v, base) {
    var x = textValue(v);
    if (!x) return "";
    if (x.indexOf("http://") === 0 || x.indexOf("https://") === 0) return x;
    if (x.indexOf("//") === 0) return "https:" + x;
    return base + (x.indexOf("/") === 0 ? x : "/" + x);
}

function payloadObject(data) {
    if (!data || typeof data !== "object") return {};
    if (data.data && typeof data.data === "object" && !Array.isArray(data.data)) {
        return data.data;
    }
    return data;
}

function parseListResponse(jsonStr, url) {
    try {
        if (!jsonStr) return JSON.stringify({ items: [], pagination: { currentPage: 1, totalPages: 1, hasNext: false } });

        var data = typeof jsonStr === "object" ? jsonStr : JSON.parse(String(jsonStr));
        data = payloadObject(data);
        var rawList = [];

        if (Array.isArray(data)) {
            rawList = data;
        } else if (Array.isArray(data.results)) {
            rawList = data.results;
        } else if (Array.isArray(data.movies)) {
            rawList = data.movies;
        } else if (Array.isArray(data.shows)) {
            rawList = data.shows;
        } else if (Array.isArray(data.items)) {
            rawList = data.items;
        }

        var items = [];
        for (var i = 0; i < rawList.length && items.length < 80; i++) {
            var item = rawList[i];
            if (!item) continue;

            var tmdbId = textValue(item.tmdbId != null ? item.tmdbId :
                    (item.tmdb_id != null ? item.tmdb_id : item.id));
            if (!tmdbId) continue;

            var rawType = textValue(item.type || item.mediaType || item.media_type).toLowerCase();
            var isShow = rawType === "show" || rawType === "tv" || rawType === "series"
                    || item.firstAirDate != null || item.first_air_date != null
                    || item.seasons != null || item.episodes != null;
            var itemType = isShow ? "show" : "movie";
            var id = itemType + "/" + tmdbId.replace(/^(movie|show)\//, "");

            var title = textValue(item.title != null ? item.title : item.name);
            if (!title) title = textValue(item.originalTitle || item.original_title);
            if (!title) continue;

            var poster = imageValue(
                    item.posterUrl != null ? item.posterUrl :
                    (item.poster != null ? item.poster : (item.posterPath != null ? item.posterPath : item.poster_path)),
                    TMDB_IMG_POSTER);
            var backdrop = imageValue(
                    item.backdropUrl != null ? item.backdropUrl :
                    (item.backdrop != null ? item.backdrop : (item.backdropPath != null ? item.backdropPath : item.backdrop_path)),
                    TMDB_IMG_BACKDROP);
            if (!backdrop) backdrop = poster;

            var ratingText = textValue(item.voteAverage != null ? item.voteAverage :
                    (item.vote_average != null ? item.vote_average : item.rating));
            var ratingNum = parseFloat(ratingText || "0");
            var quality = ratingNum > 0 ? (ratingNum.toFixed(1) + " ★") : textValue(item.quality || item.resolution) || "FHD";

            var dateText = textValue(item.releaseDate || item.release_date || item.firstAirDate || item.first_air_date || item.year);
            var yearStr = dateText ? (String(dateText).match(/\d{4}/) || [""])[0] : "";
            var episodeCurrent = isShow
                    ? (yearStr ? ("TV Series · " + yearStr) : "TV Series")
                    : (yearStr ? (yearStr + " · Movie") : "Movie");

            items.push({
                "id": id,
                "title": title,
                "posterUrl": poster,
                "backdropUrl": backdrop,
                "quality": quality,
                "episode_current": episodeCurrent
            });
        }

        var currentPage = parseInt(textValue(data.page || data.currentPage || 1), 10) || 1;
        var totalPages = parseInt(textValue(data.totalPages || data.total_pages || 1), 10) || 1;
        if (url && url.indexOf("trending") !== -1 && totalPages < 2) totalPages = 5;

        return JSON.stringify({
            "items": items,
            "pagination": {
                "currentPage": currentPage,
                "totalPages": totalPages,
                "hasNext": currentPage < totalPages || items.length >= 20
            }
        });
    } catch(e) {
        log("parseListResponse error: " + e);
        return JSON.stringify({ items: [], pagination: { currentPage: 1, totalPages: 1, hasNext: false } });
    }
}

function parseSearchResponse(html, url) { return parseListResponse(html, url); }
function parseSearchResult(html, url) { return parseListResponse(html, url); }
function parseHomeResponse(html, url) { return parseListResponse(html, url); }
function parseList(html, url) { return parseListResponse(html, url); }

// ===== PARSE MOVIE DETAIL =====

function parseMovieDetail(jsonStr, url) {
    try {
        var data = typeof jsonStr === "object" ? jsonStr : JSON.parse(jsonStr);
        if (!data) throw new Error("Empty detail JSON");

        var tmdbId = data.tmdbId || data.id || "";
        var reqUrl = url || "";
        var isShow = (reqUrl.indexOf("/shows/") !== -1 || data.seasons != null || data.firstAirDate != null);

        var title = (data.title || data.name || "NovaHD Title").trim();
        var posterUrl = data.posterPath ? (TMDB_IMG_POSTER + data.posterPath) : "";
        var backdropUrl = data.backdropPath ? (TMDB_IMG_BACKDROP + data.backdropPath) : posterUrl;
        var description = (data.overview || "Xem phim chất lượng cao Full HD trên NovaHD.").trim();

        var year = 2026;
        if (data.releaseDate) year = parseInt(String(data.releaseDate).substring(0, 4), 10) || 2026;
        else if (data.firstAirDate) year = parseInt(String(data.firstAirDate).substring(0, 4), 10) || 2026;

        var rating = typeof data.voteAverage === "number" ? data.voteAverage : parseFloat(data.voteAverage || "8.5");

        var castsStr = "";
        if (typeof data.cast === "string") {
            try {
                var castArr = JSON.parse(data.cast);
                if (Array.isArray(castArr)) castsStr = castArr.slice(0, 8).join(", ");
            } catch(ce) {
                castsStr = data.cast;
            }
        } else if (Array.isArray(data.cast)) {
            castsStr = data.cast.slice(0, 8).join(", ");
        }

        var director = data.director || "";
        var category = data.genres || "Phim Quốc Tế";

        var servers = [];

        if (isShow && data.seasons && Array.isArray(data.seasons) && data.seasons.length > 0) {
            for (var sIdx = 0; sIdx < data.seasons.length; sIdx++) {
                var season = data.seasons[sIdx];
                if (!season) continue;

                var sNum = season.seasonNumber != null ? season.seasonNumber : (sIdx + 1);
                var sName = "Mùa " + sNum + (season.name ? (" - " + season.name) : "");
                var epsList = [];

                if (season.episodes && Array.isArray(season.episodes) && season.episodes.length > 0) {
                    for (var eIdx = 0; eIdx < season.episodes.length; eIdx++) {
                        var ep = season.episodes[eIdx];
                        if (!ep) continue;

                        var epNum = ep.episodeNumber != null ? ep.episodeNumber : (eIdx + 1);
                        var epTitle = ep.title ? ("Tập " + epNum + ": " + ep.title) : ("Tập " + epNum);
                        var epStreamUrl = BASEURL + "/api/sources?type=show&tmdbId=" + tmdbId + "&season=" + sNum + "&episode=" + epNum;

                        epsList.push({
                            id: epStreamUrl,
                            name: epTitle,
                            slug: "s" + sNum + "-e" + epNum
                        });
                    }
                } else {
                    var count = season.episodeCount || 10;
                    for (var c = 1; c <= count; c++) {
                        epsList.push({
                            id: BASEURL + "/api/sources?type=show&tmdbId=" + tmdbId + "&season=" + sNum + "&episode=" + c,
                            name: "Tập " + c,
                            slug: "s" + sNum + "-e" + c
                        });
                    }
                }

                if (epsList.length > 0) {
                    servers.push({
                        name: sName,
                        episodes: epsList
                    });
                }
            }
        }

        // Single Movie
        if (servers.length === 0) {
            var movieStreamUrl = BASEURL + "/api/sources?type=movie&tmdbId=" + tmdbId;
            servers.push({
                name: "NovaHD (HLS 1080p)",
                episodes: [
                    {
                        id: movieStreamUrl,
                        name: "Xem Phim (Full HD)",
                        slug: "full"
                    }
                ]
            });
        }

        return JSON.stringify({
            id: (isShow ? "show/" : "movie/") + tmdbId,
            title: title,
            name: title,
            posterUrl: posterUrl,
            backdropUrl: backdropUrl,
            description: description,
            year: year,
            rating: rating,
            quality: "FHD",
            status: "Hoàn Thành",
            category: category,
            casts: castsStr,
            director: director,
            servers: servers
        });
    } catch(e) {
        log("parseMovieDetail error: " + e);
        return JSON.stringify({
            id: url || "error",
            title: "Lỗi tải chi tiết",
            servers: []
        });
    }
}

function parseDetail(html, url) { return parseMovieDetail(html, url); }

// ===== PARSE STREAM PLAYER =====

function parseNovaJsonOrNdjson(value) {
    if (value == null) return null;
    if (typeof value === "object") return value;
    var text = String(value).trim();
    if (!text) return null;
    try { return JSON.parse(text); } catch(e) {}

    var rows = [];
    var lines = text.split(/\\r?\\n/);
    for (var i = 0; i < lines.length; i++) {
        var line = lines[i].trim();
        if (!line || line === ":") continue;
        try { rows.push(JSON.parse(line)); } catch(e) {}
    }
    if (rows.length === 1) return rows[0];
    return rows.length > 0 ? rows : null;
}

function collectNovaSources(node, out) {
    if (node == null) return;
    if (Array.isArray(node)) {
        for (var i = 0; i < node.length; i++) collectNovaSources(node[i], out);
        return;
    }
    if (typeof node !== "object") return;

    if (Array.isArray(node.sources)) {
        for (var j = 0; j < node.sources.length; j++) {
            var src = node.sources[j];
            if (src && typeof src === "object" && src.url) out.push(src);
        }
    }
    if (node.data && node.data !== node) collectNovaSources(node.data, out);
}

function parseDetailResponse(jsonStr, url) {
    try {
        var reqUrl = url ? String(url) : "";
        var data = parseNovaJsonOrNdjson(jsonStr);

        if (reqUrl.indexOf(".m3u8") !== -1 || reqUrl.indexOf(".mp4") !== -1 || reqUrl.indexOf(".mpd") !== -1) {
            return JSON.stringify({
                "url": reqUrl,
                "isEmbed": false,
                "mimeType": reqUrl.indexOf(".mpd") !== -1 ? "application/dash+xml" : "application/x-mpegURL",
                "headers": {
                    "Referer": BASEURL + "/",
                    "Origin": BASEURL,
                    "User-Agent": "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36"
                },
                "subtitles": []
            });
        }

        var sources = [];
        collectNovaSources(data, sources);

        if (sources.length === 0) {
            var targetFetchUrl = "";
            if (reqUrl.indexOf("/api/sources") !== -1) {
                targetFetchUrl = reqUrl;
            } else if (data && data.tmdbId) {
                var showFromData = reqUrl.indexOf("/shows/") !== -1 || data.seasons != null || data.firstAirDate != null;
                targetFetchUrl = BASEURL + "/api/sources?type=" + (showFromData ? "show" : "movie")
                        + "&tmdbId=" + encodeURIComponent(String(data.tmdbId));
            } else if (reqUrl.indexOf("http") === 0) {
                targetFetchUrl = reqUrl;
            }

            if (targetFetchUrl) {
                var fetched = httpGet(targetFetchUrl, {
                    "Accept": "application/x-ndjson, application/json, text/plain, */*",
                    "Referer": BASEURL + "/",
                    "User-Agent": "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36"
                });
                data = parseNovaJsonOrNdjson(fetched);
                sources = [];
                collectNovaSources(data, sources);
            }
        }

        var streamUrl = "";
        var preferredUrl = "";
        var preferredQuality = -1;
        for (var s = 0; s < sources.length; s++) {
            var candidate = textValue(sources[s].url || sources[s].src || sources[s].file);
            if (!candidate) continue;
            if (!streamUrl) streamUrl = candidate;
            var q = textValue(sources[s].quality || sources[s].resolution || sources[s].format).toLowerCase();
            var qn = 0;
            var qm = q.match(/(\d{3,4})/);
            if (qm) qn = parseInt(qm[1], 10) || 0;
            if (q.indexOf("4k") !== -1) qn = 2160;
            if (qn > 0 && qn <= 1080 && qn > preferredQuality) {
                preferredQuality = qn;
                preferredUrl = candidate;
            }
        }

        if (preferredUrl) streamUrl = preferredUrl;

        var subtitles = [];
        var querySource = reqUrl.indexOf("/api/sources") !== -1 ? reqUrl : "";
        var subsUrl = "";
        if (querySource) {
            subsUrl = querySource.replace("/api/sources", "/api/subs-status");
        } else if (data && data.tmdbId) {
            var showForSubs = reqUrl.indexOf("/shows/") !== -1 || data.seasons != null || data.firstAirDate != null;
            subsUrl = BASEURL + "/api/subs-status?type=" + (showForSubs ? "show" : "movie")
                    + "&tmdbId=" + encodeURIComponent(String(data.tmdbId));
        }

        if (subsUrl) {
            var subsResp = httpGet(subsUrl, {
                "Accept": "application/json, text/plain, */*",
                "Referer": BASEURL + "/",
                "User-Agent": "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36"
            });
            var subsJson = parseNovaJsonOrNdjson(subsResp);
            var subNodes = [];
            if (Array.isArray(subsJson)) subNodes = subsJson;
            else if (subsJson) subNodes = [subsJson];
            for (var ni = 0; ni < subNodes.length; ni++) {
                var sn = subNodes[ni];
                if (!sn || !Array.isArray(sn.subtitles)) continue;
                for (var si = 0; si < sn.subtitles.length; si++) {
                    var sItem = sn.subtitles[si];
                    if (!sItem || !sItem.url) continue;
                    var sFullUrl = String(sItem.url).indexOf("http") === 0
                            ? String(sItem.url)
                            : BASEURL + (String(sItem.url).indexOf("/") === 0 ? "" : "/") + String(sItem.url);
                    var sLang = textValue(sItem.lang || sItem.language) || "en";
                    var sLabel = textValue(sItem.label || sItem.name) || sLang.toUpperCase();
                    if (sLang === "vi") sLabel = "Tiếng Việt (VI)";
                    else if (sLang === "en") sLabel = "Tiếng Anh (EN)";
                    subtitles.push({"url": sFullUrl, "lang": sLang, "label": sLabel});
                }
            }
        }

        if (subtitles.length === 0 && data && data.subtitles && Array.isArray(data.subtitles)) {
            for (var si2 = 0; si2 < data.subtitles.length; si2++) {
                var sub = data.subtitles[si2];
                if (!sub || !sub.url) continue;
                subtitles.push({
                    "url": String(sub.url).indexOf("http") === 0 ? String(sub.url) : BASEURL + "/" + String(sub.url).replace(/^\//, ""),
                    "lang": textValue(sub.language || sub.lang) || "en",
                    "label": textValue(sub.label || sub.name) || "Subtitle"
                });
            }
        }

        if (!streamUrl && reqUrl.indexOf("/api/sources") === -1 && reqUrl.indexOf(".m3u8") !== -1) {
            streamUrl = reqUrl;
        }
        if (!streamUrl) throw new Error("NovaHD không trả URL HLS/DASH từ /api/sources");

        var lower = streamUrl.toLowerCase();
        var mime = lower.indexOf(".mpd") !== -1 ? "application/dash+xml"
                : (lower.indexOf(".m3u8") !== -1 ? "application/x-mpegURL" : "video/mp4");

        var resHeaders = {
            "Referer": BASEURL + "/",
            "Origin": BASEURL,
            "User-Agent": "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36"
        };

        return JSON.stringify({
            "url": String(streamUrl),
            "isEmbed": false,
            "mimeType": mime,
            "headers": resHeaders,
            "subtitles": subtitles
        });
    } catch(e) {
        log("parseDetailResponse error: " + e);
        return JSON.stringify({
            "url": "",
            "isEmbed": false,
            "headers": {"Referer": BASEURL + "/", "Origin": BASEURL},
            "subtitles": [],
            "error": String(e && e.message ? e.message : e)
        });
    }
}

function parseEmbedResponse(html, url) { return parseDetailResponse(html, url); }
function parseEmbedPlayer(html, url) { return parseDetailResponse(html, url); }
function parsePlayerUrl(html, url) { return parseDetailResponse(html, url); }
function parseEpisodePlayer(html, url) { return parseDetailResponse(html, url); }