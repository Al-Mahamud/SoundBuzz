package com.example.musictube

import com.example.musictube.utils.YouTubeLinkParser
import com.example.musictube.utils.YouTubeLinkParser.SharedVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeLinkParserTest {

    private val id = "dQw4w9WgXcQ"

    @Test
    fun `parses links shared by the YouTube apps`() {
        assertEquals(SharedVideo(id), YouTubeLinkParser.parse("https://youtu.be/$id?si=AbCdEf123"))
        assertEquals(SharedVideo(id), YouTubeLinkParser.parse("https://www.youtube.com/watch?v=$id&feature=shared"))
        assertEquals(SharedVideo(id), YouTubeLinkParser.parse("https://m.youtube.com/watch?feature=youtu.be&v=$id"))
        assertEquals(SharedVideo(id), YouTubeLinkParser.parse("https://music.youtube.com/watch?v=$id&list=RDAMVM$id"))
        assertEquals(SharedVideo(id), YouTubeLinkParser.parse("https://youtube.com/shorts/$id?si=x"))
        assertEquals(SharedVideo(id), YouTubeLinkParser.parse("https://www.youtube.com/live/$id"))
        assertEquals(SharedVideo(id), YouTubeLinkParser.parse("https://www.youtube.com/embed/$id"))
        assertEquals(SharedVideo(id), YouTubeLinkParser.parse("youtu.be/$id"))
    }

    @Test
    fun `finds the link inside surrounding share text`() {
        assertEquals(
            SharedVideo(id),
            YouTubeLinkParser.parse("Never Gonna Give You Up\nhttps://youtu.be/$id.")
        )
    }

    @Test
    fun `reads the start time`() {
        assertEquals(SharedVideo(id, 42), YouTubeLinkParser.parse("https://youtu.be/$id?t=42"))
        assertEquals(SharedVideo(id, 90), YouTubeLinkParser.parse("https://www.youtube.com/watch?v=$id&t=1m30s"))
        assertEquals(SharedVideo(id, 3723), YouTubeLinkParser.parse("https://www.youtube.com/watch?v=$id#t=1h2m3s"))
        assertEquals(0L, YouTubeLinkParser.parseTimestamp("garbage"))
    }

    @Test
    fun `rejects non video links`() {
        assertNull(YouTubeLinkParser.parse(null))
        assertNull(YouTubeLinkParser.parse("hello there"))
        assertNull(YouTubeLinkParser.parse("https://www.youtube.com/@SomeChannel"))
        assertNull(YouTubeLinkParser.parse("https://www.youtube.com/playlist?list=PL123"))
        assertNull(YouTubeLinkParser.parse("https://example.com/watch?v=$id"))
        assertNull(YouTubeLinkParser.parse("https://youtu.be/short"))
    }
}
