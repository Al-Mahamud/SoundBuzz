package com.example.musictube.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.musictube.data.local.entity.PlaylistEntity
import com.example.musictube.data.local.entity.PlaylistTrackEntity
import com.example.musictube.domain.model.Playlist
import kotlinx.coroutines.flow.Flow

data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val thumbnail: String?,
    val trackCount: Int
) {
    fun toDomain(): Playlist = Playlist(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        thumbnail = thumbnail,
        trackCount = trackCount
    )
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("""
        SELECT p.id, p.name, p.createdAt, p.updatedAt, p.thumbnail, COUNT(pt.videoId) as trackCount 
        FROM playlists p 
        LEFT JOIN playlist_tracks pt ON p.id = pt.playlistId 
        GROUP BY p.id 
        ORDER BY p.updatedAt DESC
    """)
    fun getPlaylistsWithCount(): Flow<List<PlaylistWithCount>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(playlistTrack: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removePlaylistTrack(playlistId: Long, videoId: String)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC, addedAt ASC")
    fun getPlaylistTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    fun getPlaylistTrackCount(playlistId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getPlaylistTrackCountDirect(playlistId: Long): Int

    @Transaction
    suspend fun updateTrackPositions(tracks: List<PlaylistTrackEntity>) {
        for (track in tracks) {
            insertPlaylistTrack(track)
        }
    }
}
