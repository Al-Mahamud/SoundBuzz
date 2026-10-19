package com.example.musictube.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource

/**
 * HTTP data source for googlevideo audio URLs.
 *
 * YouTube resets connections that carry no `Range` header, while ExoPlayer omits it when reading
 * a file from the start, so we always send one. Stream URLs also only work with the user agent of
 * the YouTube client that produced them.
 */
@OptIn(UnstableApi::class)
object YouTubeAudioDataSource {

    fun factory(): DataSource.Factory {
        val http = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)

        return ResolvingDataSource.Factory(http) { dataSpec ->
            val url = dataSpec.uri.toString()
            val range = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
                "bytes=${dataSpec.position}-"
            } else {
                "bytes=${dataSpec.position}-${dataSpec.position + dataSpec.length - 1}"
            }
            dataSpec.withAdditionalHeaders(
                mapOf(
                    "User-Agent" to AudioStreamResolver.userAgentFor(url),
                    "Range" to range
                )
            )
        }
    }
}
