package com.example.musictube.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.musictube.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 30")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearchByQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAllSearches()

    @Query("DELETE FROM search_history WHERE id NOT IN (SELECT id FROM search_history ORDER BY timestamp DESC LIMIT 30)")
    suspend fun trimSearches()

    @Transaction
    suspend fun addSearch(query: String) {
        deleteSearchByQuery(query)
        insertSearch(SearchHistoryEntity(query = query, timestamp = System.currentTimeMillis()))
        trimSearches()
    }
}
