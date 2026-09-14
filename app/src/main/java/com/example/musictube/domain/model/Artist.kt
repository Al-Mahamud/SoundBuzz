package com.example.musictube.domain.model

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String,
    val subscriberCount: String? = null,
    val description: String? = null,
    val popularTracks: List<Track> = emptyList()
)
