package com.example.musictube.domain.usecase

import com.example.musictube.data.local.entity.FavoriteEntity
import com.example.musictube.data.local.entity.PlaybackHistoryEntity
import com.example.musictube.domain.model.Track

class RecommendationEngine {

    /**
     * Scores and ranks candidate tracks based on user behavior:
     * - Artist match: +5
     * - Genre match: +4
     * - Favorite artist: +5
     * - Recently played context: +2
     * - Same category: +3
     * - Popular / trending: +2
     * - Already played penalty: -1
     */
    fun rankTracks(
        candidates: List<Track>,
        history: List<PlaybackHistoryEntity>,
        favorites: List<FavoriteEntity>
    ): List<Track> {
        if (candidates.isEmpty()) return emptyList()

        val historyArtistCounts = mutableMapOf<String, Int>()
        val historyCategoryCounts = mutableMapOf<String, Int>()
        val playedVideoIds = mutableSetOf<String>()

        val recentHistory = history.take(5)
        val recentArtists = recentHistory.map { it.artist.lowercase() }.toSet()
        val recentCategories = recentHistory.map { it.category.lowercase() }.toSet()

        for (item in history) {
            playedVideoIds.add(item.videoId)
            val a = item.artist.lowercase()
            val c = item.category.lowercase()
            historyArtistCounts[a] = (historyArtistCounts[a] ?: 0) + 1
            historyCategoryCounts[c] = (historyCategoryCounts[c] ?: 0) + 1
        }

        val favoriteArtists = favorites.map { it.artist.lowercase() }.filter { it.isNotBlank() }.toSet()

        val scoredTracks = candidates.map { track ->
            var score = 0
            val artistLower = track.artist.lowercase()
            val categoryLower = track.category.lowercase()

            // Artist match: +5
            if (historyArtistCounts.containsKey(artistLower)) {
                score += 5
            }

            // Genre match: +4
            if (historyCategoryCounts.containsKey(categoryLower)) {
                score += 4
            }

            // Favorite artist: +5
            if (favoriteArtists.contains(artistLower)) {
                score += 5
            }

            // Recently played context: +2
            if (recentArtists.contains(artistLower) || recentCategories.contains(categoryLower)) {
                score += 2
            }

            // Same top category: +3
            val topCategory = historyCategoryCounts.maxByOrNull { it.value }?.key
            if (topCategory != null && categoryLower == topCategory) {
                score += 3
            }

            // Popular / trending: +2 (e.g. viewCount > 500M or default high viewCount)
            if (track.viewCount > 500_000_000L) {
                score += 2
            }

            // Already played: -1
            if (playedVideoIds.contains(track.youtubeVideoId)) {
                score -= 1
            }

            track to score
        }

        return scoredTracks
            .sortedByDescending { it.second }
            .map { it.first }
    }
}
