package com.example.musictube.data.remote.innertube

import com.example.musictube.domain.model.LyricLine
import com.example.musictube.domain.model.Track
import com.example.musictube.utils.DiagnosticsLogger
import com.example.musictube.utils.DurationUtils
import com.google.gson.JsonArray
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

    private const val SEARCH_ENDPOINT = "https://www.youtube.com/youtubei/v1/search"
    private const val PLAYER_ENDPOINT = "https://www.youtube.com/youtubei/v1/player"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val CAPTION_PATTERN = Pattern.compile("<p\\s+t=\"(\\d+)\"\\s+d=\"(\\d+)\"[^>]*>([\\s\\S]*?)</p>")

    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val payload = JsonObject().apply {
                val context = JsonObject().apply {
                    val client = JsonObject().apply {
                        addProperty("clientName", "ANDROID")
                        addProperty("clientVersion", "20.10.38")
                        addProperty("hl", "en")
                        addProperty("gl", "US")
                    }
                    add("client", client)
                }
                add("context", context)
                addProperty("query", query)
            }

            val request = Request.Builder()
                .url(SEARCH_ENDPOINT)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("User-Agent", "com.google.android.youtube/20.10.38 (Linux; U; Android 14)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                DiagnosticsLogger.logApi("POST", SEARCH_ENDPOINT, response.code, "InnerTube search failed", "", false)
                return@withContext emptyList()
            }

            val rawJson = response.body?.string().orEmpty()
            parseSearchResponse(rawJson)
        } catch (e: Exception) {
            DiagnosticsLogger.logApi("POST", SEARCH_ENDPOINT, -1, "InnerTube error: ${e.message}", e.stackTraceToString(), false)
            emptyList()
        }
    }

    private fun parseSearchResponse(jsonString: String): List<Track> {
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
                        .ifBlank { "YouTube Music" }

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
                            category = "Search",
                            viewCount = viewCount
                        )
                    )
                }
            }
        } catch (e: Exception) {
            DiagnosticsLogger.logPlayer("InnerTubeParse", "Error parsing search: ${e.message}")
        }
        return tracks
    }

    suspend fun getLyrics(videoId: String): List<LyricLine> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext emptyList()

        try {
            val payload = JsonObject().apply {
                val context = JsonObject().apply {
                    val client = JsonObject().apply {
                        addProperty("clientName", "ANDROID")
                        addProperty("clientVersion", "20.10.38")
                        addProperty("hl", "en")
                    }
                    add("client", client)
                }
                add("context", context)
                addProperty("videoId", videoId)
            }

            val request = Request.Builder()
                .url(PLAYER_ENDPOINT)
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
