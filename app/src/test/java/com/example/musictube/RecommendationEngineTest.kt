package com.example.musictube

import com.example.musictube.data.local.entity.FavoriteEntity
import com.example.musictube.data.local.entity.PlaybackHistoryEntity
import com.example.musictube.domain.model.Track
import com.example.musictube.domain.usecase.RecommendationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecommendationEngineTest {

    private lateinit var recommendationEngine: RecommendationEngine

    @Before
    fun setUp() {
        recommendationEngine = RecommendationEngine()
    }

    @Test
    fun `test recommendation scoring ranks preferred artist and genre higher`() {
        val candidates = listOf(
            Track(
                id = "track1",
                youtubeVideoId = "v1",
                title = "Song A",
                artist = "The Weeknd",
                thumbnailUrl = "",
                category = "Pop",
                viewCount = 100_000_000L
            ),
            Track(
                id = "track2",
                youtubeVideoId = "v2",
                title = "Song B",
                artist = "Random Artist",
                thumbnailUrl = "",
                category = "Jazz",
                viewCount = 10_000_000L
            ),
            Track(
                id = "track3",
                youtubeVideoId = "v3",
                title = "Song C",
                artist = "The Weeknd",
                thumbnailUrl = "",
                category = "Pop",
                viewCount = 600_000_000L // +2 for trending
            )
        )

        val history = listOf(
            PlaybackHistoryEntity(
                videoId = "old_v",
                title = "Old Song",
                artist = "The Weeknd",
                category = "Pop",
                thumbnailUrl = ""
            )
        )

        val favorites = listOf(
            FavoriteEntity(
                videoId = "fav_v",
                artist = "The Weeknd"
            )
        )

        val ranked = recommendationEngine.rankTracks(candidates, history, favorites)

        // Song C has: Artist (+5), Genre (+4), Favorite Artist (+5), Recent (+2), Top Category (+3), Trending (+2) = 21
        // Song A has: Artist (+5), Genre (+4), Favorite Artist (+5), Recent (+2), Top Category (+3) = 19
        // Song B has: 0
        assertEquals(3, ranked.size)
        assertEquals("Song C", ranked[0].title)
        assertEquals("Song A", ranked[1].title)
        assertEquals("Song B", ranked[2].title)
    }

    @Test
    fun `test already played penalty lowers track rank`() {
        val candidates = listOf(
            Track(
                id = "track1",
                youtubeVideoId = "played_v",
                title = "Already Played Song",
                artist = "Dua Lipa",
                thumbnailUrl = "",
                category = "Pop"
            ),
            Track(
                id = "track2",
                youtubeVideoId = "fresh_v",
                title = "Fresh Song",
                artist = "Dua Lipa",
                thumbnailUrl = "",
                category = "Pop"
            )
        )

        val history = listOf(
            PlaybackHistoryEntity(
                videoId = "played_v",
                title = "Already Played Song",
                artist = "Dua Lipa",
                category = "Pop",
                thumbnailUrl = ""
            )
        )

        val ranked = recommendationEngine.rankTracks(candidates, history, emptyList())

        // Fresh song should have higher rank because played_v incurs -1 penalty
        assertEquals("Fresh Song", ranked.first().title)
    }

    @Test
    fun `test empty candidates returns empty list`() {
        val ranked = recommendationEngine.rankTracks(emptyList(), emptyList(), emptyList())
        assertTrue(ranked.isEmpty())
    }
}
