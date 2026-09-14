package com.example.musictube.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.musictube.data.local.dao.FavoriteDao
import com.example.musictube.data.local.dao.PlaybackHistoryDao
import com.example.musictube.data.local.dao.PlaylistDao
import com.example.musictube.data.local.dao.SearchHistoryDao
import com.example.musictube.data.local.dao.TrackDao
import com.example.musictube.data.local.dao.UserPreferenceDao
import com.example.musictube.data.local.entity.FavoriteEntity
import com.example.musictube.data.local.entity.PlaybackHistoryEntity
import com.example.musictube.data.local.entity.PlaylistEntity
import com.example.musictube.data.local.entity.PlaylistTrackEntity
import com.example.musictube.data.local.entity.SearchHistoryEntity
import com.example.musictube.data.local.entity.TrackEntity
import com.example.musictube.data.local.entity.UserPreferenceEntity

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        FavoriteEntity::class,
        PlaybackHistoryEntity::class,
        SearchHistoryEntity::class,
        UserPreferenceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MusicTubeDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun userPreferenceDao(): UserPreferenceDao

    companion object {
        @Volatile
        private var INSTANCE: MusicTubeDatabase? = null

        fun getInstance(context: Context): MusicTubeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicTubeDatabase::class.java,
                    "musictube_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
