package com.example.musictube.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.musictube.domain.model.Playlist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val thumbnail: String? = null
) {
    fun toDomain(trackCount: Int = 0): Playlist = Playlist(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        thumbnail = thumbnail,
        trackCount = trackCount
    )
}
