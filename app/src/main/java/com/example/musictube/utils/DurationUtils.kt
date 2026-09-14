package com.example.musictube.utils

import java.util.regex.Pattern

object DurationUtils {
    private val ISO_8601_DURATION_PATTERN = Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")

    fun parseIsoDurationToSeconds(iso: String?): Long {
        if (iso.isNullOrBlank()) return 0L
        val matcher = ISO_8601_DURATION_PATTERN.matcher(iso)
        if (matcher.matches()) {
            val hours = matcher.group(1)?.toLongOrNull() ?: 0L
            val minutes = matcher.group(2)?.toLongOrNull() ?: 0L
            val seconds = matcher.group(3)?.toLongOrNull() ?: 0L
            return hours * 3600 + minutes * 60 + seconds
        }
        return 0L
    }

    fun formatSecondsToTime(seconds: Long): String {
        if (seconds <= 0) return "0:00"
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format("%d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%d:%02d", mins, secs)
        }
    }

    fun formatSecondsToTime(seconds: Float): String {
        return formatSecondsToTime(seconds.toLong())
    }
}
