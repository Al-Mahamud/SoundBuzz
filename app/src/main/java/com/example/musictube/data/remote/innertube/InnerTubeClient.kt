package com.example.musictube.data.remote.innertube

import com.example.musictube.domain.model.Artist
import com.example.musictube.domain.model.Category
import com.example.musictube.domain.model.LyricLine
import com.example.musictube.domain.model.Track
import com.example.musictube.utils.DiagnosticsLogger
import com.example.musictube.utils.DurationUtils
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object InnerTubeClient {

    private const val YT_SEARCH_ENDPOINT = "https://www.youtube.com/youtubei/v1/search"
    private const val YT_PLAYER_ENDPOINT = "https://www.youtube.com/youtubei/v1/player"
    private const val YTM_SEARCH_ENDPOINT = "https://music.youtube.com/youtubei/v1/search"
    private const val YTM_BROWSE_ENDPOINT = "https://music.youtube.com/youtubei/v1/browse"

    // YouTube Music Filter Params
    private const val PARAM_SONGS = "EgWKAQIIAWoSEAUQDhAJEAQQAxAKEBAQFRAR"
    private const val PARAM_ARTISTS = "EgWKAQIgAWoSEAUQDhAJEAQQAxAKEBAQFRAR"
    private const val PARAM_VIDEOS = "EgWKAQIQAWoSEAUQDhAJEAQQAxAKEBAQFRAR"

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val CAPTION_PATTERN = Pattern.compile("<p\\s+t=\"(\\d+)\"\\s+d=\"(\\d+)\"[^>]*>([\\s\\S]*?)</p>")

    private fun getWebRemixContext(): JsonObject {
        return JsonObject().apply {
            val client = JsonObject().apply {
                addProperty("clientName", "WEB_REMIX")
                addProperty("clientVersion", "1.20240101.01.00")
                addProperty("hl", "en")
                addProperty("gl", "US")
            }
            add("client", client)
        }
    }

    private fun getAndroidContext(): JsonObject {
        return JsonObject().apply {
            val client = JsonObject().apply {
                addProperty("clientName", "ANDROID")
                addProperty("clientVersion", "20.10.38")
                addProperty("hl", "en")
                addProperty("gl", "US")
            }
            add("client", client)
        }
    }

    // ==========================================
    // 1. DYNAMIC CATEGORIES (Moods & Genres)
    // ==========================================
    suspend fun browseMoodsAndGenres(): List<Category> = withContext(Dispatchers.IO) {
        try {
            val payload = JsonObject().apply {
                add("context", getWebRemixContext())
                addProperty("browseId", "FEmusic_moods_and_genres")
            }

            val request = Request.Builder()
                .url(YTM_BROWSE_ENDPOINT)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
                .header("Referer", "https://music.youtube.com/")
                .header("Origin", "https://music.youtube.com")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val rawJson = response.body?.string().orEmpty()
            val root = JsonParser.parseString(rawJson).asJsonObject
            val contents = root.getAsJsonObject("contents")
                ?.getAsJsonObject("singleColumnBrowseResultsRenderer")
                ?.getAsJsonArray("tabs")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents") ?: return@withContext emptyList()

            val categories = mutableListOf<Category>()
            for (section in contents) {
                val gridRenderer = section.asJsonObject.getAsJsonObject("gridRenderer") ?: continue
                val items = gridRenderer.getAsJsonArray("items") ?: continue

                for (item in items) {
                    val button = item.asJsonObject.getAsJsonObject("musicNavigationButtonRenderer") ?: continue
                    val name = extractText(button.getAsJsonObject("buttonText"))
                    if (name.isBlank()) continue

                    val colorLong = button.getAsJsonObject("solid")
                        ?.get("leftStripeColor")?.asLong ?: 0xFF6200EE

                    val id = name.lowercase().replace(Regex("[^a-z0-9]"), "_")
                    categories.add(
                        Category(
                            id = id,
                            name = name,
                            description = "YouTube Music Category",
                            primaryColor = colorLong
                        )
                    )
                }
            }
            categories
        } catch (e: Exception) {
            DiagnosticsLogger.logApi("POST", YTM_BROWSE_ENDPOINT, -1, "InnerTube categories error: ${e.message}", "", false)
            emptyList()
        }
    }

    // ==========================================
    // 2. AUDIO SEARCH (YouTube Music API)
    // ==========================================
    suspend fun searchAudio(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val payload = JsonObject().apply {
                add("context", getWebRemixContext())
                addProperty("query", query)
                addProperty("params", PARAM_SONGS)
            }

            val request = Request.Builder()
                .url(YTM_SEARCH_ENDPOINT)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
                .header("Referer", "https://music.youtube.com/")
                .header("Origin", "https://music.youtube.com")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val rawJson = response.body?.string().orEmpty()
            parseYtmTracksResponse(rawJson, "Audio")
        } catch (e: Exception) {
            DiagnosticsLogger.logApi("POST", YTM_SEARCH_ENDPOINT, -1, "InnerTube audio search error: ${e.message}", "", false)
            emptyList()
        }
    }

    // ==========================================
    // 3. VIDEO SEARCH (YouTube Video API)
    // ==========================================
    suspend fun searchVideo(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val payload = JsonObject().apply {
                add("context", getAndroidContext())
                addProperty("query", query)
            }

            val request = Request.Builder()
                .url(YT_SEARCH_ENDPOINT)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("User-Agent", "com.google.android.youtube/20.10.38 (Linux; U; Android 14)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val rawJson = response.body?.string().orEmpty()
            parseYtVideoResponse(rawJson)
        } catch (e: Exception) {
            DiagnosticsLogger.logApi("POST", YT_SEARCH_ENDPOINT, -1, "InnerTube video search error: ${e.message}", "", false)
            emptyList()
        }
    }

    // ==========================================
    // 4. UNIFIED SEARCH (Audio priority with video fallback)
    // ==========================================
    suspend fun search(query: String): List<Track> {
        val audioTracks = searchAudio(query)
        if (audioTracks.isNotEmpty()) return audioTracks
        return searchVideo(query)
    }

    // ==========================================
    // 5. ARTIST-WISE SEARCH (YouTube Music API)
    // ==========================================
    suspend fun searchArtists(query: String): List<Artist> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val payload = JsonObject().apply {
                add("context", getWebRemixContext())
                addProperty("query", query)
                addProperty("params", PARAM_ARTISTS)
            }

            val request = Request.Builder()
                .url(YTM_SEARCH_ENDPOINT)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
                .header("Referer", "https://music.youtube.com/")
                .header("Origin", "https://music.youtube.com")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val rawJson = response.body?.string().orEmpty()
            val root = JsonParser.parseString(rawJson).asJsonObject
            val contents = root.getAsJsonObject("contents")
                ?.getAsJsonObject("tabbedSearchResultsRenderer")
                ?.getAsJsonArray("tabs")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents") ?: return@withContext emptyList()

            val artists = mutableListOf<Artist>()
            for (sec in contents) {
                val shelf = sec.asJsonObject.getAsJsonObject("musicShelfRenderer") ?: continue
                val items = shelf.getAsJsonArray("contents") ?: continue

                for (item in items) {
                    val r = item.asJsonObject.getAsJsonObject("musicResponsiveListItemRenderer") ?: continue
                    val flexCols = r.getAsJsonArray("flexColumns") ?: continue
                    if (flexCols.size() < 1) continue

                    val name = extractText(flexCols.get(0).asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")?.get("text"))
                    if (name.isBlank()) continue

                    val info = if (flexCols.size() > 1) {
                        extractText(flexCols.get(1).asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")?.get("text"))
                    } else ""

                    val browseId = r.getAsJsonObject("navigationEndpoint")
                        ?.getAsJsonObject("browseEndpoint")
                        ?.get("browseId")?.asString ?: name.lowercase().replace(" ", "_")

                    val thumb = extractBestThumbnail(
                        r.getAsJsonObject("thumbnail")?.getAsJsonObject("musicThumbnailRenderer")?.getAsJsonObject("thumbnail"),
                        ""
                    )

                    artists.add(
                        Artist(
                            id = browseId,
                            name = name,
                            imageUrl = thumb,
                            subscriberCount = info.ifBlank { "Popular Artist" },
                            description = "Artist on YouTube Music"
                        )
                    )
                }
            }
            artists
        } catch (e: Exception) {
            DiagnosticsLogger.logApi("POST", YTM_SEARCH_ENDPOINT, -1, "InnerTube artist search error: ${e.message}", "", false)
            emptyList()
        }
    }

    // ==========================================
    // 6. ARTIST DETAILS & TOP TRACKS
    // ==========================================
    suspend fun getArtistDetails(artistNameOrBrowseId: String): Artist? = withContext(Dispatchers.IO) {
        if (artistNameOrBrowseId.isBlank()) return@withContext null

        try {
            var browseId = artistNameOrBrowseId
            if (!browseId.startsWith("UC")) {
                val found = searchArtists(artistNameOrBrowseId)
                if (found.isNotEmpty()) {
                    browseId = found.first().id
                }
            }

            val payload = JsonObject().apply {
                add("context", getWebRemixContext())
                addProperty("browseId", browseId)
            }

            val request = Request.Builder()
                .url(YTM_BROWSE_ENDPOINT)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
                .header("Referer", "https://music.youtube.com/")
                .header("Origin", "https://music.youtube.com")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val rawJson = response.body?.string().orEmpty()
            val root = JsonParser.parseString(rawJson).asJsonObject

            val header = root.getAsJsonObject("header")?.getAsJsonObject("musicImmersiveHeaderRenderer")
                ?: root.getAsJsonObject("header")?.getAsJsonObject("musicVisualHeaderRenderer")

            val artistName = extractText(header?.get("title")).ifBlank { artistNameOrBrowseId }
            val description = extractText(header?.get("description"))

            val thumb = extractBestThumbnail(
                header?.getAsJsonObject("thumbnail")?.getAsJsonObject("musicThumbnailRenderer")?.getAsJsonObject("thumbnail"),
                ""
            )

            val topTracks = mutableListOf<Track>()
            val contents = root.getAsJsonObject("contents")
                ?.getAsJsonObject("singleColumnBrowseResultsRenderer")
                ?.getAsJsonArray("tabs")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents")

            if (contents != null && contents.size() > 0) {
                val shelf = contents.get(0).asJsonObject.getAsJsonObject("musicShelfRenderer")
                val items = shelf?.getAsJsonArray("contents")
                if (items != null) {
                    for (item in items) {
                        val r = item.asJsonObject.getAsJsonObject("musicResponsiveListItemRenderer") ?: continue
                        val flexCols = r.getAsJsonArray("flexColumns") ?: continue
                        if (flexCols.size() < 1) continue

                        val title = extractText(flexCols.get(0).asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")?.get("text"))
                        if (title.isBlank()) continue

                        val artistText = if (flexCols.size() > 1) {
                            extractText(flexCols.get(1).asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")?.get("text"))
                        } else artistName

                        val videoId = r.getAsJsonObject("overlay")
                            ?.getAsJsonObject("musicItemThumbnailOverlayRenderer")
                            ?.getAsJsonObject("content")
                            ?.getAsJsonObject("musicPlayButtonRenderer")
                            ?.getAsJsonObject("playNavigationEndpoint")
                            ?.getAsJsonObject("watchEndpoint")
                            ?.get("videoId")?.asString
                            ?: r.getAsJsonObject("navigationEndpoint")
                                ?.getAsJsonObject("watchEndpoint")
                                ?.get("videoId")?.asString
                            ?: continue

                        val itemThumb = extractBestThumbnail(
                            r.getAsJsonObject("thumbnail")?.getAsJsonObject("musicThumbnailRenderer")?.getAsJsonObject("thumbnail"),
                            videoId
                        )

                        topTracks.add(
                            Track(
                                id = videoId,
                                youtubeVideoId = videoId,
                                title = title,
                                artist = artistText.ifBlank { artistName },
                                channel = artistName,
                                thumbnailUrl = itemThumb,
                                duration = "3:30",
                                durationSeconds = 210,
                                category = "Audio / Music"
                            )
                        )
                    }
                }
            }

            Artist(
                id = browseId,
                name = artistName,
                imageUrl = thumb.ifBlank { "https://img.youtube.com/vi/4NRXx6U8ABQ/hqdefault.jpg" },
                description = description,
                popularTracks = topTracks
            )
        } catch (e: Exception) {
            DiagnosticsLogger.logApi("POST", YTM_BROWSE_ENDPOINT, -1, "InnerTube artist details error: ${e.message}", "", false)
            null
        }
    }

    // ==========================================
    // 7. HOME SECTIONS: TRENDING, POPULAR & NEW RELEASES
    // ==========================================
    suspend fun getTrendingTracks(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = searchAudio("Trending Top Hits")
        if (tracks.isNotEmpty()) return@withContext tracks
        searchVideo("Trending Music 2026")
    }

    suspend fun getPopularTracks(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = searchAudio("Popular Music Hits")
        if (tracks.isNotEmpty()) return@withContext tracks
        searchVideo("Global Top 50 Music")
    }

    suspend fun getNewReleases(): List<Track> = withContext(Dispatchers.IO) {
        try {
            val payload = JsonObject().apply {
                add("context", getWebRemixContext())
                addProperty("browseId", "FEmusic_new_releases")
            }

            val request = Request.Builder()
                .url(YTM_BROWSE_ENDPOINT)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
                .header("Referer", "https://music.youtube.com/")
                .header("Origin", "https://music.youtube.com")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext searchAudio("New Releases 2026")

            val rawJson = response.body?.string().orEmpty()
            val root = JsonParser.parseString(rawJson).asJsonObject
            val contents = root.getAsJsonObject("contents")
                ?.getAsJsonObject("singleColumnBrowseResultsRenderer")
                ?.getAsJsonArray("tabs")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents") ?: return@withContext searchAudio("New Releases 2026")

            val tracks = mutableListOf<Track>()
            for (sec in contents) {
                val carousel = sec.asJsonObject.getAsJsonObject("musicCarouselShelfRenderer") ?: continue
                val items = carousel.getAsJsonArray("contents") ?: continue

                for (item in items) {
                    val twoRow = item.asJsonObject.getAsJsonObject("musicTwoRowItemRenderer") ?: continue
                    val title = extractText(twoRow.get("title"))
                    val subtitle = extractText(twoRow.get("subtitle"))
                    if (title.isBlank()) continue

                    val videoId = twoRow.getAsJsonObject("thumbnailOverlay")
                        ?.getAsJsonObject("musicItemThumbnailOverlayRenderer")
                        ?.getAsJsonObject("content")
                        ?.getAsJsonObject("musicPlayButtonRenderer")
                        ?.getAsJsonObject("playNavigationEndpoint")
                        ?.getAsJsonObject("watchEndpoint")
                        ?.get("videoId")?.asString
                        ?: twoRow.getAsJsonObject("navigationEndpoint")
                            ?.getAsJsonObject("watchEndpoint")
                            ?.get("videoId")?.asString
                        ?: continue

                    val thumb = extractBestThumbnail(
                        twoRow.getAsJsonObject("thumbnailRenderer")?.getAsJsonObject("musicThumbnailRenderer")?.getAsJsonObject("thumbnail"),
                        videoId
                    )

                    tracks.add(
                        Track(
                            id = videoId,
                            youtubeVideoId = videoId,
                            title = title,
                            artist = subtitle.ifBlank { "YouTube Music" },
                            channel = subtitle.ifBlank { "YouTube Music" },
                            thumbnailUrl = thumb,
                            duration = "3:40",
                            durationSeconds = 220,
                            category = "New Releases"
                        )
                    )
                }
            }
            if (tracks.isNotEmpty()) tracks else searchAudio("New Releases 2026")
        } catch (e: Exception) {
            searchAudio("New Releases 2026")
        }
    }

    suspend fun getCategoryTracks(categoryName: String): List<Track> = withContext(Dispatchers.IO) {
        val tracks = searchAudio("$categoryName music")
        if (tracks.isNotEmpty()) return@withContext tracks
        searchVideo("$categoryName songs")
    }

    // ==========================================
    // 8. PARSING HELPERS
    // ==========================================
    private fun parseYtmTracksResponse(jsonString: String, categoryTag: String): List<Track> {
        val tracks = mutableListOf<Track>()
        try {
            val root = JsonParser.parseString(jsonString).asJsonObject
            val contents = root.getAsJsonObject("contents")
                ?.getAsJsonObject("tabbedSearchResultsRenderer")
                ?.getAsJsonArray("tabs")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents") ?: return emptyList()

            for (sec in contents) {
                val shelf = sec.asJsonObject.getAsJsonObject("musicShelfRenderer") ?: continue
                val items = shelf.getAsJsonArray("contents") ?: continue

                for (item in items) {
                    val r = item.asJsonObject.getAsJsonObject("musicResponsiveListItemRenderer") ?: continue
                    val flexCols = r.getAsJsonArray("flexColumns") ?: continue
                    if (flexCols.size() < 1) continue

                    val title = extractText(flexCols.get(0).asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")?.get("text"))
                    if (title.isBlank()) continue

                    val artistText = if (flexCols.size() > 1) {
                        extractText(flexCols.get(1).asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")?.get("text"))
                    } else "YouTube Music"

                    val videoId = r.getAsJsonObject("overlay")
                        ?.getAsJsonObject("musicItemThumbnailOverlayRenderer")
                        ?.getAsJsonObject("content")
                        ?.getAsJsonObject("musicPlayButtonRenderer")
                        ?.getAsJsonObject("playNavigationEndpoint")
                        ?.getAsJsonObject("watchEndpoint")
                        ?.get("videoId")?.asString
                        ?: r.getAsJsonObject("navigationEndpoint")
                            ?.getAsJsonObject("watchEndpoint")
                            ?.get("videoId")?.asString
                        ?: continue

                    val thumb = extractBestThumbnail(
                        r.getAsJsonObject("thumbnail")?.getAsJsonObject("musicThumbnailRenderer")?.getAsJsonObject("thumbnail"),
                        videoId
                    )

                    tracks.add(
                        Track(
                            id = videoId,
                            youtubeVideoId = videoId,
                            title = title,
                            artist = artistText,
                            channel = artistText,
                            thumbnailUrl = thumb,
                            duration = "3:30",
                            durationSeconds = 210,
                            category = categoryTag
                        )
                    )
                }
            }
        } catch (e: Exception) {
            DiagnosticsLogger.logPlayer("YTMParse", "Error parsing YTM search: ${e.message}")
        }
        return tracks
    }

    private fun parseYtVideoResponse(jsonString: String): List<Track> {
        val tracks = mutableListOf<Track>()
        try {
            val root = JsonParser.parseString(jsonString).asJsonObject
            val contents = root.getAsJsonObject("contents") ?: return emptyList()
            val sectionListRenderer = contents.getAsJsonObject("sectionListRenderer") ?: return emptyList()
            val sections = sectionListRenderer.getAsJsonArray("contents") ?: return emptyList()

            for (section in sections) {
                val itemSectionRenderer = section.asJsonObject.getAsJsonObject("itemSectionRenderer") ?: continue
                val items = itemSectionRenderer.getAsJsonArray("contents") ?: continue

                for (item in items) {
                    val itemObj = item.asJsonObject
                    val videoRenderer = itemObj.getAsJsonObject("compactVideoRenderer")
                        ?: itemObj.getAsJsonObject("videoRenderer")
                        ?: continue

                    val videoId = videoRenderer.get("videoId")?.asString ?: continue
                    val title = extractText(videoRenderer.get("title"))
                    if (title.isBlank()) continue

                    val artist = extractText(videoRenderer.get("shortBylineText"))
                        .ifBlank { extractText(videoRenderer.get("longBylineText")) }
                        .ifBlank { "YouTube" }

                    val durationText = extractText(videoRenderer.get("lengthText"))
                    val durationSeconds = DurationUtils.parseTimeStringToSeconds(durationText)

                    val thumbnail = extractBestThumbnail(videoRenderer.getAsJsonObject("thumbnail"), videoId)
                    val viewCount = extractViewCount(videoRenderer.get("viewCountText"))

                    tracks.add(
                        Track(
                            id = videoId,
                            youtubeVideoId = videoId,
                            title = title,
                            artist = artist,
                            channel = artist,
                            thumbnailUrl = thumbnail,
                            duration = durationText.ifBlank { DurationUtils.formatSecondsToTime(durationSeconds) },
                            durationSeconds = durationSeconds,
                            category = "Video",
                            viewCount = viewCount
                        )
                    )
                }
            }
        } catch (e: Exception) {
            DiagnosticsLogger.logPlayer("YTParse", "Error parsing YT video search: ${e.message}")
        }
        return tracks
    }

    suspend fun getLyrics(videoId: String): List<LyricLine> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext emptyList()

        try {
            val payload = JsonObject().apply {
                add("context", getAndroidContext())
                addProperty("videoId", videoId)
            }

            val request = Request.Builder()
                .url(YT_PLAYER_ENDPOINT)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("User-Agent", "com.google.android.youtube/20.10.38 (Linux; U; Android 14)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val rawJson = response.body?.string().orEmpty()
            val root = JsonParser.parseString(rawJson).asJsonObject

            val captions = root.getAsJsonObject("captions")
                ?.getAsJsonObject("playerCaptionsTracklistRenderer")
                ?.getAsJsonArray("captionTracks") ?: return@withContext emptyList()

            var bestTrackUrl: String? = null
            for (caption in captions) {
                val obj = caption.asJsonObject
                val lang = obj.get("languageCode")?.asString.orEmpty()
                val url = obj.get("baseUrl")?.asString.orEmpty()
                if (url.isNotBlank()) {
                    if (lang.startsWith("en", ignoreCase = true)) {
                        bestTrackUrl = url
                        break
                    } else if (bestTrackUrl == null) {
                        bestTrackUrl = url
                    }
                }
            }

            if (bestTrackUrl.isNullOrBlank()) return@withContext emptyList()

            val captionRequest = Request.Builder().url(bestTrackUrl).build()
            val captionResponse = httpClient.newCall(captionRequest).execute()
            if (!captionResponse.isSuccessful) return@withContext emptyList()

            val xmlBody = captionResponse.body?.string().orEmpty()
            parseCaptionsXml(xmlBody)
        } catch (e: Exception) {
            DiagnosticsLogger.logPlayer("InnerTubeLyrics", "Error fetching lyrics: ${e.message}")
            emptyList()
        }
    }

    private fun parseCaptionsXml(xml: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val matcher = CAPTION_PATTERN.matcher(xml)

        while (matcher.find()) {
            val startMs = matcher.group(1)?.toLongOrNull() ?: 0L
            val durationMs = matcher.group(2)?.toLongOrNull() ?: 0L
            val rawText = matcher.group(3).orEmpty()

            val cleanText = cleanHtmlEntities(rawText).trim()
            if (cleanText.isNotBlank() && cleanText != "[]" && !cleanText.equals("[Music]", ignoreCase = true)) {
                lines.add(LyricLine(startMs = startMs, durationMs = durationMs, text = cleanText))
            }
        }
        return lines
    }

    private fun cleanHtmlEntities(input: String): String {
        return input
            .replace("&#39;", "'")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("\n", " ")
            .replace(Regex("<[^>]*>"), "")
            .trim()
    }

    private fun extractText(element: com.google.gson.JsonElement?): String {
        if (element == null || element.isJsonNull) return ""
        if (element.isJsonPrimitive) return element.asString

        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has("simpleText")) {
                return obj.get("simpleText").asString
            }
            if (obj.has("runs")) {
                val runs = obj.getAsJsonArray("runs")
                val sb = StringBuilder()
                for (run in runs) {
                    if (run.isJsonObject && run.asJsonObject.has("text")) {
                        sb.append(run.asJsonObject.get("text").asString)
                    }
                }
                return sb.toString().trim()
            }
        }
        return ""
    }

    private fun extractBestThumbnail(thumbObj: JsonObject?, videoId: String): String {
        if (thumbObj != null && thumbObj.has("thumbnails")) {
            val arr = thumbObj.getAsJsonArray("thumbnails")
            if (arr != null && arr.size() > 0) {
                val last = arr.get(arr.size() - 1).asJsonObject
                val url = last.get("url")?.asString
                if (!url.isNullOrBlank()) {
                    return if (url.startsWith("//")) "https:$url" else url
                }
            }
        }
        return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
    }

    private fun extractViewCount(element: com.google.gson.JsonElement?): Long {
        val text = extractText(element)
        if (text.isBlank()) return 0L
        val digitsOnly = text.filter { it.isDigit() }
        return digitsOnly.toLongOrNull() ?: 0L
    }
}
