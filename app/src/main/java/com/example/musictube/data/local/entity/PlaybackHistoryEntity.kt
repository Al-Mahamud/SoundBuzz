package com.example.musictube.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.musictube.domain.model.Track

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey
    val videoId: String,
    val title: String,
    val artist: String,
    val channel: String = artist,
    val thumbnailUrl: String,
    val playedAt: Long = System.currentTimeMillis(),
    val duration: String = "",
    val durationSeconds: Long = 0L,
    val category: String = "General"
) {
    fun toDomain(isFavorite: Boolean = false): Track = Track(
        id = videoId,
        youtubeVideoId = videoId,
        title = title,
        artist = artist,
        channel = channel,
        thumbnailUrl = thumbnailUrl,
        duration = duration,
        durationSeconds = durationSeconds,
        category = category,
        isFavorite = isFavorite
    )

    companion object {
        fun fromTrack(track: Track, playedAt: Long = System.currentTimeMillis()): PlaybackHistoryEntity =
            PlaybackHistoryEntity(
                videoId = track.youtubeVideoId,
                title = track.title,
                artist = track.artist,
                channel = track.channel,
                thumbnailUrl = track.thumbnailUrl,
                playedAt = playedAt,
                duration = track.duration,
                durationSeconds = track.durationSeconds,
                category = track.category
            )
    }
}
