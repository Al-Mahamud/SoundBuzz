package com.example.musictube

import com.example.musictube.utils.DurationUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class DurationUtilsTest {

    @Test
    fun `test ISO 8601 duration parsing to seconds`() {
        assertEquals(225L, DurationUtils.parseIsoDurationToSeconds("PT3M45S"))
        assertEquals(200L, DurationUtils.parseIsoDurationToSeconds("PT3M20S"))
        assertEquals(3600L, DurationUtils.parseIsoDurationToSeconds("PT1H"))
        assertEquals(3723L, DurationUtils.parseIsoDurationToSeconds("PT1H2M3S"))
        assertEquals(45L, DurationUtils.parseIsoDurationToSeconds("PT45S"))
        assertEquals(0L, DurationUtils.parseIsoDurationToSeconds(null))
        assertEquals(0L, DurationUtils.parseIsoDurationToSeconds(""))
    }

    @Test
    fun `test seconds formatting to human readable time`() {
        assertEquals("3:45", DurationUtils.formatSecondsToTime(225L))
        assertEquals("0:00", DurationUtils.formatSecondsToTime(0L))
        assertEquals("1:02:03", DurationUtils.formatSecondsToTime(3723L))
        assertEquals("0:45", DurationUtils.formatSecondsToTime(45L))
    }
}
