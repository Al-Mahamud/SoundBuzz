package com.example.musictube.data.repository

import com.example.musictube.BuildConfig
import com.example.musictube.data.local.database.MusicTubeDatabase
import com.example.musictube.data.local.entity.FavoriteEntity
import com.example.musictube.data.local.entity.PlaybackHistoryEntity
import com.example.musictube.data.local.entity.PlaylistEntity
import com.example.musictube.data.local.entity.PlaylistTrackEntity
import com.example.musictube.data.local.entity.TrackEntity
import com.example.musictube.data.local.entity.UserPreferenceEntity
import com.example.musictube.data.remote.api.NetworkClient
import com.example.musictube.data.remote.innertube.InnerTubeClient
import com.example.musictube.data.remote.model.DefaultMusicCatalog
import com.example.musictube.domain.model.Artist
import com.example.musictube.domain.model.Category
import com.example.musictube.domain.model.CategoryPlaylist
import com.example.musictube.domain.model.LyricLine
import com.example.musictube.domain.model.Playlist
import com.example.musictube.domain.model.Track
import com.example.musictube.domain.usecase.RecommendationEngine
import com.example.musictube.utils.DiagnosticsLogger
import com.example.musictube.utils.DurationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(
    private val database: MusicTubeDatabase,
    private val recommendationEngine: RecommendationEngine = RecommendationEngine()
) {
    private val trackDao = database.trackDao()
    private val playlistDao = database.playlistDao()
    private val favoriteDao = database.favoriteDao()
    private val playbackHistoryDao = database.playbackHistoryDao()
    private val searchHistoryDao = database.searchHistoryDao()
    private val userPreferenceDao = database.userPreferenceDao()

    private val PREF_API_KEY = "youtube_api_key"

    fun getCategories(): Flow<List<Category>> = flow {
        var list = InnerTubeClient.browseMoodsAndGenres()
        if (list.isEmpty()) {
            list = DefaultMusicCatalog.categories
        }
        emit(list)
    }.flowOn(Dispatchers.IO)

    fun getDefaultCategories(): List<Category> = DefaultMusicCatalog.categories

    fun getPopularArtists(): List<Artist> = DefaultMusicCatalog.popularArtists

    fun getYoutubeApiKeyFlow(): Flow<String> {
        return userPreferenceDao.getPreferenceFlow(PREF_API_KEY).map { pref ->
            pref?.value?.ifBlank { BuildConfig.DEFAULT_YOUTUBE_API_KEY } ?: BuildConfig.DEFAULT_YOUTUBE_API_KEY
        }
    }

    suspend fun setYoutubeApiKey(apiKey: String) {
        withContext(Dispatchers.IO) {
            userPreferenceDao.setPreference(UserPreferenceEntity(PREF_API_KEY, apiKey.trim()))
        }
    }

    private suspend fun getActiveApiKey(): String {
        val storedKey = userPreferenceDao.getPreferenceValue(PREF_API_KEY)
        return if (!storedKey.isNullOrBlank()) {
            storedKey.trim()
        } else {
            BuildConfig.DEFAULT_YOUTUBE_API_KEY.trim()
        }
    }

    fun getTrendingTracks(): Flow<List<Track>> = flow {
        var tracks: List<Track> = emptyList()

        // 1. Primary: InnerTube API
        try {
            tracks = InnerTubeClient.getTrendingTracks()
            if (tracks.isNotEmpty()) {
                trackDao.insertTracks(tracks.map { TrackEntity.fromDomain(it) })
            }
        } catch (e: Exception) {
            // fallback
        }

        // 2. Secondary fallback: YouTube Data API v3
        if (tracks.isEmpty()) {
            val apiKey = getActiveApiKey()
            if (apiKey.isNotBlank()) {
                try {
                    val response = NetworkClient.apiService.getPopularVideos(apiKey = apiKey)
                    val items = response.items.orEmpty()
                    if (items.isNotEmpty()) {
                        tracks = items.mapNotNull { item ->
                            val videoId = item.id ?: return@mapNotNull null
                            val snippet = item.snippet ?: return@mapNotNull null
                            val title = snippet.title ?: "Untitled"
                            val artist = snippet.channelTitle ?: "Unknown Artist"
                            val thumb = snippet.thumbnails?.bestUrl() ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                            val durationSec = DurationUtils.parseIsoDurationToSeconds(item.contentDetails?.duration)
                            val durationStr = DurationUtils.formatSecondsToTime(durationSec)
                            val views = item.statistics?.viewCount?.toLongOrNull() ?: 0L

                            Track(
                                id = videoId,
                                youtubeVideoId = videoId,
                                title = title,
                                artist = artist,
                                channel = artist,
                                thumbnailUrl = thumb,
                                duration = durationStr,
                                durationSeconds = durationSec,
                                category = "Trending",
                                viewCount = views,
                                publishedAt = snippet.publishedAt.orEmpty()
                            )
                        }
                        trackDao.insertTracks(tracks.map { TrackEntity.fromDomain(it) })
                    }
                } catch (e: Exception) {
                    // Fallback to local catalog
                }
            }
        }

        if (tracks.isEmpty()) {
            tracks = DefaultMusicCatalog.catalogTracks.take(8)
        }

        val favIds = favoriteDao.getAllFavoriteVideoIdsDirect().toSet()
        emit(tracks.map { it.copy(isFavorite = favIds.contains(it.youtubeVideoId)) })
    }.flowOn(Dispatchers.IO)

    fun getPopularTracks(): Flow<List<Track>> = flow {
        var tracks: List<Track> = emptyList()
        try {
            tracks = InnerTubeClient.getPopularTracks()
            if (tracks.isNotEmpty()) {
                trackDao.insertTracks(tracks.map { TrackEntity.fromDomain(it) })
            }
        } catch (e: Exception) {
            // fallback
        }

        if (tracks.isEmpty()) {
            tracks = DefaultMusicCatalog.catalogTracks.sortedByDescending { it.viewCount }.take(10)
        }

        val favIds = favoriteDao.getAllFavoriteVideoIdsDirect().toSet()
        emit(tracks.map { it.copy(isFavorite = favIds.contains(it.youtubeVideoId)) })
    }.flowOn(Dispatchers.IO)

    fun getNewReleases(): Flow<List<Track>> = flow {
        var tracks: List<Track> = emptyList()
        try {
            tracks = InnerTubeClient.getNewReleases()
            if (tracks.isNotEmpty()) {
                trackDao.insertTracks(tracks.map { TrackEntity.fromDomain(it) })
            }
        } catch (e: Exception) {
            // fallback
        }

        if (tracks.isEmpty()) {
            tracks = DefaultMusicCatalog.catalogTracks.shuffled().take(8)
        }

        val favIds = favoriteDao.getAllFavoriteVideoIdsDirect().toSet()
        emit(tracks.map { it.copy(isFavorite = favIds.contains(it.youtubeVideoId)) })
    }.flowOn(Dispatchers.IO)

    fun getCategoryTracks(categoryName: String): Flow<List<Track>> = flow {
        var tracks: List<Track> = emptyList()

        // 1. Primary: InnerTube API
        try {
            tracks = InnerTubeClient.getCategoryTracks(categoryName)
            if (tracks.isNotEmpty()) {
                trackDao.insertTracks(tracks.map { TrackEntity.fromDomain(it) })
            }
        } catch (e: Exception) {
            // fallback
        }

        // 2. Secondary: YouTube Data API v3
        if (tracks.isEmpty()) {
            val apiKey = getActiveApiKey()
            if (apiKey.isNotBlank()) {
                try {
                    val query = "$categoryName music"
                    val searchResponse = NetworkClient.apiService.search(query = query, apiKey = apiKey)
                    val videoIds = searchResponse.items?.mapNotNull { it.id?.videoId }?.joinToString(",")
                    if (!videoIds.isNullOrBlank()) {
                        val detailsResponse = NetworkClient.apiService.getVideoDetails(videoIds = videoIds, apiKey = apiKey)
                        val items = detailsResponse.items.orEmpty()
                        if (items.isNotEmpty()) {
                            tracks = items.mapNotNull { item ->
                                val id = item.id ?: return@mapNotNull null
                                val snippet = item.snippet ?: return@mapNotNull null
                                val durationSec = DurationUtils.parseIsoDurationToSeconds(item.contentDetails?.duration)
                                Track(
                                    id = id,
                                    youtubeVideoId = id,
                                    title = snippet.title ?: "Untitled",
                                    artist = snippet.channelTitle ?: "Unknown Artist",
                                    channel = snippet.channelTitle ?: "Unknown Artist",
                                    thumbnailUrl = snippet.thumbnails?.bestUrl() ?: "https://img.youtube.com/vi/$id/hqdefault.jpg",
                                    duration = DurationUtils.formatSecondsToTime(durationSec),
                                    durationSeconds = durationSec,
                                    category = categoryName,
                                    viewCount = item.statistics?.viewCount?.toLongOrNull() ?: 0L,
                                    publishedAt = snippet.publishedAt.orEmpty()
                                )
                            }
                            trackDao.insertTracks(tracks.map { TrackEntity.fromDomain(it) })
                        }
                    }
                } catch (e: Exception) {
                    // Fallback
                }
            }
        }

        if (tracks.isEmpty()) {
            val matched = DefaultMusicCatalog.catalogTracks.filter {
                it.category.equals(categoryName, ignoreCase = true)
            }
            tracks = if (matched.isNotEmpty()) matched else DefaultMusicCatalog.catalogTracks.shuffled().take(6)
        }

        val favIds = favoriteDao.getAllFavoriteVideoIdsDirect().toSet()
        emit(tracks.map { it.copy(isFavorite = favIds.contains(it.youtubeVideoId)) })
    }.flowOn(Dispatchers.IO)

    fun getCategoryPlaylists(): Flow<List<CategoryPlaylist>> = flow {
        val categoriesConfig = listOf(
            Triple("Workout", "High-Energy Workout Beats", "Power up your session with pulse-pounding workout hits"),
            Triple("Chill", "Midnight Lo-Fi & Chill", "Soothing beats and ambient melodies to relax and unwind"),
            Triple("Party", "Ultimate Party Bangers", "Dance floor anthems and chart-topping party hits"),
            Triple("Focus", "Deep Focus & Study Flow", "Calm instrumentals for productivity and deep work"),
            Triple("Pop", "Global Pop Anthems", "Today's hottest pop hits and trending viral anthems"),
            Triple("Rock", "Rock Legends & Anthems", "High-voltage riffs and timeless rock classics"),
            Triple("Romance", "Romantic Evening Serenade", "Sweet acoustic and emotional love ballads"),
            Triple("Bangla", "Bangla & Regional Vibes", "Soulful regional hits and popular Bengali melodies")
        )

        // 1. Initial immediate emission using local catalog so UI renders with zero lag
        val initialPlaylists = categoriesConfig.map { (catName, title, desc) ->
            val matched = DefaultMusicCatalog.catalogTracks.filter {
                it.category.equals(catName, ignoreCase = true)
            }.ifEmpty { DefaultMusicCatalog.catalogTracks.shuffled().take(6) }

            CategoryPlaylist(
                id = "cat_pl_${catName.lowercase()}",
                title = title,
                category = catName,
                description = desc,
                thumbnailUrl = matched.firstOrNull()?.thumbnailUrl ?: "https://img.youtube.com/vi/4NRXx6U8ABQ/hqdefault.jpg",
                trackCount = matched.size,
                tracks = matched
            )
        }
        emit(initialPlaylists)

        // 2. Refresh asynchronously with live InnerTube category tracks
        try {
            val livePlaylists = categoriesConfig.map { (catName, title, desc) ->
                var tracks: List<Track> = emptyList()
                try {
                    tracks = InnerTubeClient.getCategoryTracks(catName).take(12)
                } catch (_: Exception) {}

                if (tracks.isEmpty()) {
                    tracks = DefaultMusicCatalog.catalogTracks.filter {
                        it.category.equals(catName, ignoreCase = true)
                    }.ifEmpty { DefaultMusicCatalog.catalogTracks.shuffled().take(6) }
                }

                CategoryPlaylist(
                    id = "cat_pl_${catName.lowercase()}",
                    title = title,
                    category = catName,
                    description = desc,
                    thumbnailUrl = tracks.firstOrNull()?.thumbnailUrl ?: "https://img.youtube.com/vi/4NRXx6U8ABQ/hqdefault.jpg",
                    trackCount = tracks.size,
                    tracks = tracks
                )
            }
            emit(livePlaylists)
        } catch (e: Exception) {
            DiagnosticsLogger.w("Repository", "InnerTube category playlists refresh note: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    fun searchTracks(query: String, searchMode: String = "ALL"): Flow<List<Track>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }

        var tracks: List<Track> = emptyList()

        // 1. Primary: InnerTube API (Unlimited, keyless, real-time YouTube music search)
        try {
            tracks = when (searchMode.uppercase()) {
                "AUDIO" -> InnerTubeClient.searchAudio(query)
                "VIDEO" -> InnerTubeClient.searchVideo(query)
                else -> InnerTubeClient.search(query)
            }
            if (tracks.isNotEmpty()) {
                trackDao.insertTracks(tracks.map { TrackEntity.fromDomain(it) })
            }
        } catch (e: Exception) {
            // Fallback to Data API if available
        }

        // 2. Secondary fallback: YouTube Data API v3 (if configured by user)
        if (tracks.isEmpty()) {
            val apiKey = getActiveApiKey()
            if (apiKey.isNotBlank()) {
                try {
                    val searchResponse = NetworkClient.apiService.search(query = query, apiKey = apiKey)
                    val videoIds = searchResponse.items?.mapNotNull { it.id?.videoId }?.joinToString(",")
                    if (!videoIds.isNullOrBlank()) {
                        val detailsResponse = NetworkClient.apiService.getVideoDetails(videoIds = videoIds, apiKey = apiKey)
                        val items = detailsResponse.items.orEmpty()
                        if (items.isNotEmpty()) {
                            tracks = items.mapNotNull { item ->
                                val id = item.id ?: return@mapNotNull null
                                val snippet = item.snippet ?: return@mapNotNull null
                                val durationSec = DurationUtils.parseIsoDurationToSeconds(item.contentDetails?.duration)
                                Track(
                                    id = id,
                                    youtubeVideoId = id,
                                    title = snippet.title ?: "Untitled",
                                    artist = snippet.channelTitle ?: "Unknown Artist",
                                    channel = snippet.channelTitle ?: "Unknown Artist",
                                    thumbnailUrl = snippet.thumbnails?.bestUrl() ?: "https://img.youtube.com/vi/$id/hqdefault.jpg",
                                    duration = DurationUtils.formatSecondsToTime(durationSec),
                                    durationSeconds = durationSec,
                                    category = "Search",
                                    viewCount = item.statistics?.viewCount?.toLongOrNull() ?: 0L,
                                    publishedAt = snippet.publishedAt.orEmpty()
                                )
                            }
                            trackDao.insertTracks(tracks.map { TrackEntity.fromDomain(it) })
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to local catalog
                }
            }
        }

        // 3. Offline fallback
        if (tracks.isEmpty()) {
            val q = query.lowercase()
            tracks = DefaultMusicCatalog.catalogTracks.filter {
                it.title.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q) ||
                        it.category.lowercase().contains(q)
            }
        }

        val favIds = favoriteDao.getAllFavoriteVideoIdsDirect().toSet()
        emit(tracks.map { it.copy(isFavorite = favIds.contains(it.youtubeVideoId)) })
    }.flowOn(Dispatchers.IO)

    fun searchArtists(query: String): Flow<List<Artist>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }

        var artists: List<Artist> = emptyList()
        try {
            artists = InnerTubeClient.searchArtists(query)
        } catch (e: Exception) {
            // fallback
        }

        if (artists.isEmpty()) {
            artists = DefaultMusicCatalog.popularArtists.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
        emit(artists)
    }.flowOn(Dispatchers.IO)

    fun getLyrics(videoId: String): Flow<List<LyricLine>> = flow {
        val lyrics = InnerTubeClient.getLyrics(videoId)
        emit(lyrics)
    }.flowOn(Dispatchers.IO)

    fun getRecommendedTracks(): Flow<List<Track>> = combine(
        playbackHistoryDao.getHistory(),
        favoriteDao.getAllFavorites()
    ) { history, favorites ->
        val candidates = DefaultMusicCatalog.catalogTracks
        val ranked = recommendationEngine.rankTracks(candidates, history, favorites)
        val favIds = favorites.map { it.videoId }.toSet()
        ranked.map { it.copy(isFavorite = favIds.contains(it.youtubeVideoId)) }
    }.flowOn(Dispatchers.IO)

    fun getArtistDetails(artistName: String): Flow<Artist?> = flow {
        var artist: Artist? = null
        try {
            artist = InnerTubeClient.getArtistDetails(artistName)
        } catch (e: Exception) {
            // fallback
        }

        if (artist == null) {
            val matchedArtist = DefaultMusicCatalog.popularArtists.firstOrNull {
                it.name.equals(artistName, ignoreCase = true) || it.id.equals(artistName, ignoreCase = true)
            } ?: Artist(
                id = artistName.lowercase().replace(" ", "_"),
                name = artistName,
                imageUrl = "https://img.youtube.com/vi/4NRXx6U8ABQ/hqdefault.jpg",
                description = "Popular artist on SoundBuzz."
            )

            val artistTracks = DefaultMusicCatalog.catalogTracks.filter {
                it.artist.contains(artistName, ignoreCase = true)
            }
            artist = matchedArtist.copy(popularTracks = artistTracks)
        }

        emit(artist)
    }.flowOn(Dispatchers.IO)

    fun getRelatedTracks(videoId: String): Flow<List<Track>> = flow {
        val favIds = favoriteDao.getAllFavoriteVideoIdsDirect().toSet()
        val currentTrack = DefaultMusicCatalog.catalogTracks.firstOrNull { it.youtubeVideoId == videoId }
        val related = if (currentTrack != null) {
            DefaultMusicCatalog.catalogTracks.filter {
                it.youtubeVideoId != videoId &&
                        (it.category.equals(currentTrack.category, ignoreCase = true) ||
                                it.artist.equals(currentTrack.artist, ignoreCase = true))
            }
        } else {
            DefaultMusicCatalog.catalogTracks.shuffled().take(6)
        }
        emit(related.map { it.copy(isFavorite = favIds.contains(it.youtubeVideoId)) })
    }.flowOn(Dispatchers.IO)

    // History
    fun getRecentlyPlayed(): Flow<List<Track>> {
        return combine(
            playbackHistoryDao.getHistory(),
            favoriteDao.getAllFavoriteVideoIds()
        ) { historyList, favIds ->
            val favSet = favIds.toSet()
            historyList.map { it.toDomain(isFavorite = favSet.contains(it.videoId)) }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun recordTrackPlayed(track: Track) {
        withContext(Dispatchers.IO) {
            playbackHistoryDao.recordPlay(PlaybackHistoryEntity.fromTrack(track))
            trackDao.insertTrack(TrackEntity.fromDomain(track))
        }
    }

    suspend fun clearPlaybackHistory() {
        withContext(Dispatchers.IO) {
            playbackHistoryDao.clearHistory()
        }
    }

    // Favorites
    fun getFavorites(): Flow<List<Track>> {
        return favoriteDao.getAllFavorites().map { list ->
            list.map { fav ->
                val cached = trackDao.getTrackById(fav.videoId)
                if (cached != null) {
                    cached.toDomain(isFavorite = true)
                } else {
                    Track(
                        id = fav.videoId,
                        youtubeVideoId = fav.videoId,
                        title = fav.title.ifBlank { "Favorite Song" },
                        artist = fav.artist.ifBlank { "Unknown Artist" },
                        channel = fav.artist.ifBlank { "Unknown Artist" },
                        thumbnailUrl = fav.thumbnailUrl.ifBlank { "https://img.youtube.com/vi/${fav.videoId}/hqdefault.jpg" },
                        duration = fav.duration,
                        isFavorite = true
                    )
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    fun isFavorite(videoId: String): Flow<Boolean> = favoriteDao.isFavorite(videoId).flowOn(Dispatchers.IO)

    suspend fun toggleFavorite(track: Track) {
        withContext(Dispatchers.IO) {
            val isFav = favoriteDao.isFavoriteDirect(track.youtubeVideoId)
            if (isFav) {
                favoriteDao.deleteFavorite(track.youtubeVideoId)
            } else {
                favoriteDao.insertFavorite(
                    FavoriteEntity(
                        videoId = track.youtubeVideoId,
                        title = track.title,
                        artist = track.artist,
                        thumbnailUrl = track.thumbnailUrl,
                        duration = track.duration,
                        addedAt = System.currentTimeMillis()
                    )
                )
                trackDao.insertTrack(TrackEntity.fromDomain(track))
            }
        }
    }

    // Playlists
    fun getPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getPlaylistsWithCount().map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(
            PlaylistEntity(
                name = name.trim(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        val existing = playlistDao.getPlaylistById(playlistId)
        if (existing != null) {
            playlistDao.updatePlaylist(existing.copy(name = newName.trim(), updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylistById(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, track: Track) = withContext(Dispatchers.IO) {
        val count = playlistDao.getPlaylistTrackCountDirect(playlistId)
        playlistDao.insertPlaylistTrack(
            PlaylistTrackEntity(
                playlistId = playlistId,
                videoId = track.youtubeVideoId,
                title = track.title,
                artist = track.artist,
                thumbnailUrl = track.thumbnailUrl,
                position = count,
                addedAt = System.currentTimeMillis()
            )
        )
        // Also update playlist thumbnail and updatedAt
        val playlist = playlistDao.getPlaylistById(playlistId)
        if (playlist != null) {
            playlistDao.updatePlaylist(
                playlist.copy(
                    thumbnail = playlist.thumbnail ?: track.thumbnailUrl,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        trackDao.insertTrack(TrackEntity.fromDomain(track))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, videoId: String) = withContext(Dispatchers.IO) {
        playlistDao.removePlaylistTrack(playlistId, videoId)
    }

    fun getPlaylistTracks(playlistId: Long): Flow<List<Track>> {
        return combine(
            playlistDao.getPlaylistTracks(playlistId),
            favoriteDao.getAllFavoriteVideoIds()
        ) { ptList, favIds ->
            val favSet = favIds.toSet()
            ptList.map { pt ->
                Track(
                    id = pt.videoId,
                    youtubeVideoId = pt.videoId,
                    title = pt.title,
                    artist = pt.artist,
                    channel = pt.artist,
                    thumbnailUrl = pt.thumbnailUrl,
                    isFavorite = favSet.contains(pt.videoId)
                )
            }
        }
    }

    // Search History
    fun getRecentSearches(): Flow<List<String>> {
        return searchHistoryDao.getRecentSearches().map { list -> list.map { it.query } }
    }

    suspend fun addSearchQuery(query: String) = withContext(Dispatchers.IO) {
        if (query.isNotBlank()) {
            searchHistoryDao.addSearch(query.trim())
        }
    }

    suspend fun deleteSearchQuery(query: String) = withContext(Dispatchers.IO) {
        searchHistoryDao.deleteSearchByQuery(query.trim())
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        searchHistoryDao.clearAllSearches()
    }

    // Clear Cache
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        trackDao.deleteTracksOlderThan(System.currentTimeMillis())
    }
}
