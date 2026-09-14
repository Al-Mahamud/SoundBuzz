package com.example.musictube.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.musictube.data.local.entity.PlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT 50")
    fun getHistory(): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT 50")
    suspend fun getHistoryDirect(): List<PlaybackHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHistory(entry: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE videoId = :videoId")
    suspend fun deleteHistoryItem(videoId: String)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    @Query("DELETE FROM playback_history WHERE videoId NOT IN (SELECT videoId FROM playback_history ORDER BY playedAt DESC LIMIT 50)")
    suspend fun trimHistory()

    @Transaction
    suspend fun recordPlay(entry: PlaybackHistoryEntity) {
        insertOrUpdateHistory(entry)
        trimHistory()
    }
}
