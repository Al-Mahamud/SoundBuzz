package com.example.musictube.domain.model

data class LyricLine(
    val startMs: Long,
    val durationMs: Long,
    val text: String
) {
    val endMs: Long
        get() = startMs + durationMs

    fun isActiveAt(currentSecond: Float): Boolean {
        val currentMs = (currentSecond * 1000).toLong()
        return currentMs in startMs..endMs
    }
}
