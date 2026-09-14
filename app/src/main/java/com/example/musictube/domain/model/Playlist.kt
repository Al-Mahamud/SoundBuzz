package com.example.musictube.domain.model

data class Playlist(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val thumbnail: String? = null,
    val trackCount: Int = 0
)

data class PlaylistItem(
    val playlistId: Long,
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)
