package com.example.musictube

import com.example.musictube.data.local.entity.FavoriteEntity
import com.example.musictube.data.local.entity.PlaybackHistoryEntity
import com.example.musictube.data.local.entity.PlaylistEntity
import com.example.musictube.data.local.entity.TrackEntity
import com.example.musictube.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityMappingTest {

    @Test
    fun `test Track domain to entity and entity to domain conversion`() {
        val original = Track(
            id = "vid_123",
            youtubeVideoId = "vid_123",
            title = "Blinding Lights",
            artist = "The Weeknd",
            channel = "The Weeknd",
            thumbnailUrl = "https://example.com/thumb.jpg",
            duration = "3:20",
            durationSeconds = 200,
            category = "Pop",
            viewCount = 1000L,
            isFavorite = true
        )

        val entity = TrackEntity.fromDomain(original)
        assertEquals("vid_123", entity.id)
        assertEquals("Blinding Lights", entity.title)
        assertEquals("The Weeknd", entity.artist)

        val restored = entity.toDomain(isFavorite = true)
        assertEquals(original.id, restored.id)
        assertEquals(original.title, restored.title)
        assertTrue(restored.isFavorite)
    }

    @Test
    fun `test PlaybackHistoryEntity creation from track`() {
        val track = Track(
            id = "v999",
            youtubeVideoId = "v999",
            title = "Test Song",
            artist = "Test Artist",
            thumbnailUrl = "thumb",
            duration = "2:30",
            durationSeconds = 150
        )

        val history = PlaybackHistoryEntity.fromTrack(track)
        assertEquals("v999", history.videoId)
        assertEquals("Test Song", history.title)
        assertEquals("Test Artist", history.artist)
    }

    @Test
    fun `test PlaylistEntity track count assignment`() {
        val entity = PlaylistEntity(
            id = 42,
            name = "My Favorites",
            thumbnail = "thumb"
        )
        val domain = entity.toDomain(trackCount = 15)
        assertEquals(42L, domain.id)
        assertEquals("My Favorites", domain.name)
        assertEquals(15, domain.trackCount)
    }
}
