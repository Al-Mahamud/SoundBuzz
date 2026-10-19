package com.example.musictube.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.DeliveryMethod

class AudioStreamResolverTest {

    private fun stream(
        id: String,
        kbps: Int,
        trackType: AudioTrackType? = null,
        delivery: DeliveryMethod = DeliveryMethod.PROGRESSIVE_HTTP
    ): AudioStream = AudioStream.Builder()
        .setId(id)
        .setContent("https://example.googlevideo.com/videoplayback?id=$id", true)
        .setMediaFormat(MediaFormat.WEBMA_OPUS)
        .setDeliveryMethod(delivery)
        .setAverageBitrate(kbps)
        .setAudioTrackType(trackType)
        .build()

    @Test
    fun `picks the smallest stream that is at least 64 kbps`() {
        val streams = listOf(stream("139", 48), stream("249", 50), stream("250", 70), stream("140", 128), stream("251", 160))
        assertEquals("250", AudioStreamResolver.pickStream(streams)?.id)
    }

    @Test
    fun `falls back to the best stream when all are below 64 kbps`() {
        val streams = listOf(stream("139", 48), stream("249", 50))
        assertEquals("249", AudioStreamResolver.pickStream(streams)?.id)
    }

    @Test
    fun `prefers the original audio track over dubbed ones`() {
        val streams = listOf(
            stream("dubbed", 70, AudioTrackType.DUBBED),
            stream("original", 128, AudioTrackType.ORIGINAL)
        )
        assertEquals("original", AudioStreamResolver.pickStream(streams)?.id)
    }

    @Test
    fun `ignores streams that are not progressive http`() {
        val streams = listOf(stream("dash", 70, delivery = DeliveryMethod.DASH))
        assertNull(AudioStreamResolver.pickStream(streams))
    }
}
