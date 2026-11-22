package com.example.musictube.utils

/**
 * Pulls a YouTube video ID (and an optional start time) out of a link or a block of shared text.
 *
 * Handles what the YouTube / YouTube Music apps and browsers share, e.g.
 *  - https://youtu.be/dQw4w9WgXcQ?si=abc
 *  - https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42s
 *  - https://m.youtube.com/watch?v=..., https://music.youtube.com/watch?v=...
 *  - https://www.youtube.com/shorts/..., /live/..., /embed/..., /v/...
 *  - "Check this out https://youtu.be/..." (URL inside surrounding text)
 */
object YouTubeLinkParser {

    data class SharedVideo(val videoId: String, val startSeconds: Long = 0L)

    private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
    private val URL_IN_TEXT = Regex("""(?<![\w.-])(?:https?://)?(?:[\w-]+\.)*(?:youtube\.com|youtu\.be|youtube-nocookie\.com)/\S+""", RegexOption.IGNORE_CASE)
    private val PATH_PREFIXES = listOf("shorts/", "live/", "embed/", "v/", "e/")

    fun parse(text: String?): SharedVideo? {
        if (text.isNullOrBlank()) return null
        return URL_IN_TEXT.findAll(text).firstNotNullOfOrNull { parseUrl(it.value) }
    }

    private fun parseUrl(rawUrl: String): SharedVideo? {
        val url = rawUrl.trimEnd('.', ',', ')', ']', '!', '?', '"', '\'')
        val withoutScheme = url.substringAfter("://", url)
        val host = withoutScheme.substringBefore('/').substringBefore('?').lowercase()
        val afterHost = withoutScheme.substringAfter('/', "")
        val path = afterHost.substringBefore('?').substringBefore('#')
        val query = parseQuery(afterHost.substringAfter('?', "").substringBefore('#'))
        val fragment = afterHost.substringAfter('#', "")

        val videoId = when {
            host == "youtu.be" || host.endsWith(".youtu.be") -> path.substringBefore('/')
            path == "watch" || path.startsWith("watch/") -> query["v"]
            else -> PATH_PREFIXES.firstOrNull { path.startsWith(it) }
                ?.let { path.removePrefix(it).substringBefore('/') }
        }?.takeIf { VIDEO_ID.matches(it) } ?: return null

        val start = query["t"] ?: query["start"] ?: fragment.takeIf { it.startsWith("t=") }?.removePrefix("t=")
        return SharedVideo(videoId, parseTimestamp(start))
    }

    private fun parseQuery(query: String): Map<String, String> = query.split('&')
        .filter { '=' in it }
        .associate { it.substringBefore('=') to it.substringAfter('=') }

    /** Accepts "90", "90s", "1m30s", "1h2m3s". */
    internal fun parseTimestamp(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        value.toLongOrNull()?.let { return it.coerceAtLeast(0L) }
        val match = Regex("""^(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?$""").matchEntire(value.lowercase()) ?: return 0L
        val (h, m, s) = match.destructured
        return (h.toLongOrNull() ?: 0L) * 3600 + (m.toLongOrNull() ?: 0L) * 60 + (s.toLongOrNull() ?: 0L)
    }
}
