package com.example.musictube.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val videoId: String,
    val title: String = "",
    val artist: String = "",
    val thumbnailUrl: String = "",
    val duration: String = "",
    val addedAt: Long = System.currentTimeMillis()
)
