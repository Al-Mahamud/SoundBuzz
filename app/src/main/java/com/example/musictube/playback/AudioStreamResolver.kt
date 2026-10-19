package com.example.musictube.playback

import android.net.Uri
import com.example.musictube.utils.DiagnosticsLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Resolves a YouTube video ID to a direct audio-only stream URL (no video track),
 * so audio playback downloads roughly 0.5 MB/min instead of a full video stream.
 */
object AudioStreamResolver {

    /** Lowest bitrate (kbps) we accept when picking the most data-efficient stream. */
    private const val MIN_ACCEPTABLE_BITRATE_KBPS = 64

    /** Refresh links a little before YouTube's `expire` deadline. */
    private const val EXPIRY_SAFETY_MARGIN_MS = 10 * 60 * 1000L
    private const val DEFAULT_LIFETIME_MS = 5 * 60 * 60 * 1000L

    data class ResolvedAudio(
        val videoId: String,
        val url: String,
        val mimeType: String?,
        val codec: String?,
        val bitrateKbps: Int,
        val expiresAtMs: Long
    ) {
        val label: String
            get() = listOfNotNull(codec?.substringBefore('.')?.uppercase(), "$bitrateKbps kbps").joinToString(" · ")
    }

    private val cache = ConcurrentHashMap<String, ResolvedAudio>()

    @Volatile
    private var initialized = false

    private fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            NewPipe.init(NewPipeDownloader(client))
            initialized = true
        }
    }

    suspend fun resolve(videoId: String, forceRefresh: Boolean = false): ResolvedAudio = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            cache[videoId]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return@withContext it }
        }

        ensureInitialized()
        val info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$videoId")
        val stream = pickStream(info.audioStreams)
            ?: throw IOException("No audio-only stream available for $videoId")

        val resolved = ResolvedAudio(
            videoId = videoId,
            url = stream.content,
            mimeType = stream.format?.mimeType,
            codec = stream.codec,
            bitrateKbps = stream.kbps(),
            expiresAtMs = expiryOf(stream.content)
        )
        cache[videoId] = resolved
        DiagnosticsLogger.logPlayer("AudioStreamResolver", "Resolved $videoId -> itag ${stream.itag} (${resolved.label})")
        resolved
    }

    fun invalidate(videoId: String) {
        cache.remove(videoId)
    }

    /**
     * Picks the smallest audio-only stream that still sounds good (>= 64 kbps), preferring the
     * original audio track over dubbed/descriptive ones.
     */
    internal fun pickStream(streams: List<AudioStream>): AudioStream? {
        val playable = streams.filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
        val candidates = playable.filter { it.audioTrackType == null || it.audioTrackType == AudioTrackType.ORIGINAL }
            .ifEmpty { playable }
        if (candidates.isEmpty()) return null

        return candidates.filter { it.kbps() >= MIN_ACCEPTABLE_BITRATE_KBPS }.minByOrNull { it.kbps() }
            ?: candidates.maxByOrNull { it.kbps() }
    }

    private fun AudioStream.kbps(): Int = when {
        averageBitrate > 0 -> averageBitrate
        bitrate > 0 -> bitrate / 1000
        else -> 0
    }

    private fun expiryOf(url: String): Long {
        val expireSeconds = try {
            Uri.parse(url).getQueryParameter("expire")?.toLongOrNull()
        } catch (e: Exception) {
            null
        }
        return if (expireSeconds != null) {
            expireSeconds * 1000 - EXPIRY_SAFETY_MARGIN_MS
        } else {
            System.currentTimeMillis() + DEFAULT_LIFETIME_MS
        }
    }

    /** User agent YouTube expects for a given stream URL (it depends on which client produced it). */
    fun userAgentFor(url: String): String = when {
        YoutubeParsingHelper.isAndroidStreamingUrl(url) -> YoutubeParsingHelper.getAndroidUserAgent(null)
        YoutubeParsingHelper.isIosStreamingUrl(url) -> YoutubeParsingHelper.getIosUserAgent(null)
        YoutubeParsingHelper.isVisionOsStreamingUrl(url) -> YoutubeParsingHelper.getVisionOsUserAgent(null)
        else -> NewPipeDownloader.USER_AGENT
    }
}
