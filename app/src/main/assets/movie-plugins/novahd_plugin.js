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

function parseListResponse(jsonStr, url) {
    try {
        if (!jsonStr) return JSON.stringify({ items: [], pagination: { currentPage: 1, totalPages: 1, hasNext: false } });

        var data = typeof jsonStr === "object" ? jsonStr : JSON.parse(jsonStr);
        var rawList = [];

        if (Array.isArray(data)) {
            rawList = data;
        } else if (data.results && Array.isArray(data.results)) {
            rawList = data.results;
        }

        var items = [];
        for (var i = 0; i < rawList.length; i++) {
            var item = rawList[i];
            if (!item) continue;

            var tmdbId = item.tmdbId || item.id;
            if (!tmdbId) continue;

            var isShow = (item.type === "show" || item.type === "tv" || item.firstAirDate != null || item.seasons != null);
            var itemType = isShow ? "show" : "movie";
            var id = itemType + "/" + tmdbId;

            var title = (item.title || item.name || "").trim();
            if (!title) continue;

            var poster = item.posterPath ? (TMDB_IMG_POSTER + item.posterPath) : "";
            var backdrop = item.backdropPath ? (TMDB_IMG_BACKDROP + item.backdropPath) : poster;

            var ratingNum = typeof item.voteAverage === "number" ? item.voteAverage : parseFloat(item.voteAverage || "0");
            var quality = ratingNum > 0 ? (ratingNum.toFixed(1) + " ★") : "FHD";

            var yearStr = "";
            if (item.releaseDate) yearStr = String(item.releaseDate).substring(0, 4);
            else if (item.firstAirDate) yearStr = String(item.firstAirDate).substring(0, 4);

            var episodeCurrent = isShow ? (yearStr ? ("TV Series · " + yearStr) : "TV Series") : (yearStr ? (yearStr + " · Movie") : "Movie");

            items.push({
                "id": id,
                "title": title,
                "posterUrl": poster,
                "backdropUrl": backdrop,
                "quality": quality,
                "episode_current": episodeCurrent
            });
        }

        var currentPage = 1;
        var totalPages = 1;
        if (data.page) currentPage = parseInt(data.page, 10) || 1;
        if (data.totalPages) totalPages = parseInt(data.totalPages, 10) || 1;
        else if (url && url.indexOf("trending") !== -1) totalPages = 5;

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

function parseDetailResponse(jsonStr, url) {
    try {
        var reqUrl = url ? String(url) : "";
        var data = null;

        if (jsonStr) {
            try {
                data = typeof jsonStr === "object" ? jsonStr : JSON.parse(jsonStr);
            } catch(e) {}
        }

        // Nếu reqUrl đã là link .m3u8 hoặc .mp4 trực tiếp
        if (reqUrl.indexOf(".m3u8") !== -1 || reqUrl.indexOf(".mp4") !== -1) {
            return JSON.stringify({
                "url": reqUrl,
                "isEmbed": false,
                "headers": {
                    "Referer": BASEURL + "/",
                    "Origin": BASEURL,
                    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                },
                "subtitles": []
            });
        }

        // Nếu data chưa có field sources (ví dụ app truyền detailHtml vào), phải fetch từ endpoint /api/sources
        if (!data || !data.sources || !Array.isArray(data.sources) || data.sources.length === 0) {
            var targetFetchUrl = "";

            if (reqUrl.indexOf("/api/sources") !== -1) {
                targetFetchUrl = reqUrl;
            } else if (data && data.tmdbId) {
                var isShow = (reqUrl.indexOf("shows") !== -1 || data.seasons != null || data.firstAirDate != null);
                targetFetchUrl = BASEURL + "/api/sources?type=" + (isShow ? "show" : "movie") + "&tmdbId=" + data.tmdbId;
            } else if (reqUrl.indexOf("http") === 0) {
                targetFetchUrl = reqUrl;
            }

            if (targetFetchUrl) {
                var fetched = httpGet(targetFetchUrl, {
                    "Referer": BASEURL + "/",
                    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                });
                if (fetched) {
                    try { data = JSON.parse(fetched); } catch(e) {}
                }
            }
        }

        var streamUrl = "";
        var subtitles = [];

        if (data && data.sources && Array.isArray(data.sources) && data.sources.length > 0) {
            var firstSrc = data.sources[0];
            streamUrl = firstSrc.url || "";

            for (var s = 0; s < data.sources.length; s++) {
                var src = data.sources[s];
                if (src.quality === "1080p" || src.quality === "4k") {
                    streamUrl = src.url;
                    break;
                }
            }
        }

        // Trích xuất Subtitles từ /api/subs-status
        var subsUrl = "";
        var querySource = reqUrl.indexOf("/api/sources") !== -1 ? reqUrl : "";

        if (querySource) {
            subsUrl = querySource.replace("/api/sources", "/api/subs-status");
        } else if (data && data.tmdbId) {
            var isShowForSubs = (reqUrl.indexOf("shows") !== -1 || data.seasons != null || data.firstAirDate != null);
            subsUrl = BASEURL + "/api/subs-status?type=" + (isShowForSubs ? "show" : "movie") + "&tmdbId=" + data.tmdbId;
        }

        if (subsUrl) {
            var subsResp = httpGet(subsUrl, {
                "Referer": BASEURL + "/",
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            });
            if (subsResp) {
                try {
                    var subsJson = JSON.parse(subsResp);
                    if (subsJson && subsJson.subtitles && Array.isArray(subsJson.subtitles)) {
                        for (var i = 0; i < subsJson.subtitles.length; i++) {
                            var sItem = subsJson.subtitles[i];
                            if (sItem && sItem.url) {
                                var sFullUrl = sItem.url.indexOf("http") === 0 ? sItem.url : (BASEURL + (sItem.url.indexOf("/") === 0 ? "" : "/") + sItem.url);
                                var sLang = sItem.lang || sItem.language || "en";
                                var sLabel = sItem.label || sLang.toUpperCase();
                                if (sLang === "vi") sLabel = "Tiếng Việt (VI)";
                                else if (sLang === "en") sLabel = "Tiếng Anh (EN)";
                                else if (sLang === "es") sLabel = "Tây Ban Nha (ES)";
                                else if (sLang === "pt") sLabel = "Bồ Đào Nha (PT)";
                                else if (sLang === "fr") sLabel = "Tiếng Pháp (FR)";

                                subtitles.push({
                                    url: sFullUrl,
                                    lang: sLang,
                                    label: sLabel
                                });
                            }
                        }
                    }
                } catch(se) {}
            }
        }

        // Fallback subtitles có sẵn trong data.subtitles
        if (subtitles.length === 0 && data && data.subtitles && Array.isArray(data.subtitles)) {
            for (var subIdx = 0; subIdx < data.subtitles.length; subIdx++) {
                var sub = data.subtitles[subIdx];
                if (sub && sub.url) {
                    var fullSubUrl = sub.url.indexOf("http") === 0 ? sub.url : (BASEURL + (sub.url.indexOf("/") === 0 ? "" : "/") + sub.url);
                    subtitles.push({
                        url: fullSubUrl,
                        lang: sub.language || sub.lang || "en",
                        label: sub.label || "Subtitle"
                    });
                }
            }
        }

        if (!streamUrl) {
            streamUrl = reqUrl;
        }

        // Gắn danh sách tất cả các track phụ đề qua #sub= cho ExoPlayer (Media3)
        if (subtitles.length > 0 && streamUrl.indexOf("#sub=") === -1) {
            var subParts = [];
            for (var k = 0; k < subtitles.length; k++) {
                subParts.push(subtitles[k].url + "|" + subtitles[k].lang + "|" + subtitles[k].label);
            }
            var subParam = subParts.join(";");
            streamUrl += "#sub=" + encodeURIComponent(subParam);
        }

        // Tạo custom JS để tự động gắn các track phụ đề vào phần tử <video> khi WebPlayer khởi chạy
        var customSubJs = "";
        if (subtitles.length > 0) {
            customSubJs = "(function(){" +
                "function addTracks(){" +
                "  var v = document.getElementById('video') || document.querySelector('video');" +
                "  if(!v){ setTimeout(addTracks, 300); return; }" +
                "  var subs = " + JSON.stringify(subtitles) + ";" +
                "  for(var i=0; i<subs.length; i++){" +
                "    var tr = document.createElement('track');" +
                "    tr.kind = 'subtitles';" +
                "    tr.label = subs[i].label;" +
                "    tr.srclang = subs[i].lang;" +
                "    tr.src = subs[i].url;" +
                "    if(subs[i].lang === 'vi' || i === 0) tr.default = true;" +
                "    v.appendChild(tr);" +
                "  }" +
                "}" +
                "addTracks();" +
            "})();";
        }

        var resHeaders = {
            "Referer": BASEURL + "/",
            "Origin": BASEURL,
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        };
        if (customSubJs) {
            resHeaders["Custom-Js"] = customSubJs;
        }

        return JSON.stringify({
            "url": String(streamUrl),
            "isEmbed": false,
            "headers": resHeaders,
            "subtitles": subtitles
        });
    } catch(e) {
        log("parseDetailResponse error: " + e);
        return JSON.stringify({
            "url": url ? String(url) : "",
            "isEmbed": false,
            "headers": { "Referer": BASEURL + "/" },
            "subtitles": []
        });
    }
}

function parseEmbedResponse(html, url) { return parseDetailResponse(html, url); }
function parseEmbedPlayer(html, url) { return parseDetailResponse(html, url); }
function parsePlayerUrl(html, url) { return parseDetailResponse(html, url); }
function parseEpisodePlayer(html, url) { return parseDetailResponse(html, url); }