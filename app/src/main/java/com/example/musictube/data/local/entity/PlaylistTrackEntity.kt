package com.example.musictube.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "videoId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlistId"]), Index(value = ["videoId"])]
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)
