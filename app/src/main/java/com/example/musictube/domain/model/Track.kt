package com.example.musictube.domain.model

data class Track(
    val id: String,
    val youtubeVideoId: String,
    val title: String,
    val artist: String,
    val channel: String = artist,
    val thumbnailUrl: String,
    val duration: String = "",
    val durationSeconds: Long = 0L,
    val category: String = "General",
    val publishedAt: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val viewCount: Long = 0L,
    val isFavorite: Boolean = false
)
