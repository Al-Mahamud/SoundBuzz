package com.example.musictube.domain.model

data class CategoryPlaylist(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val thumbnailUrl: String,
    val trackCount: Int,
    val tracks: List<Track> = emptyList()
)
