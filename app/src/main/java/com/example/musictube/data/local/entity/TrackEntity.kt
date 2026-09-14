package com.example.musictube.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.musictube.domain.model.Track

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey
    val id: String, // youtubeVideoId
    val youtubeVideoId: String,
    val title: String,
    val artist: String,
    val channel: String,
    val thumbnailUrl: String,
    val duration: String,
    val durationSeconds: Long = 0L,
    val category: String,
    val publishedAt: String,
    val createdAt: Long = System.currentTimeMillis(),
    val viewCount: Long = 0L
) {
    fun toDomain(isFavorite: Boolean = false): Track = Track(
        id = id,
        youtubeVideoId = youtubeVideoId,
        title = title,
        artist = artist,
        channel = channel,
        thumbnailUrl = thumbnailUrl,
        duration = duration,
        durationSeconds = durationSeconds,
        category = category,
        publishedAt = publishedAt,
        createdAt = createdAt,
        viewCount = viewCount,
        isFavorite = isFavorite
    )

    companion object {
        fun fromDomain(track: Track): TrackEntity = TrackEntity(
            id = track.youtubeVideoId,
            youtubeVideoId = track.youtubeVideoId,
            title = track.title,
            artist = track.artist,
            channel = track.channel,
            thumbnailUrl = track.thumbnailUrl,
            duration = track.duration,
            durationSeconds = track.durationSeconds,
            category = track.category,
            publishedAt = track.publishedAt,
            createdAt = track.createdAt,
            viewCount = track.viewCount
        )
    }
}
