package com.example.musictube.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musictube.MusicTubeApplication
import com.example.musictube.data.remote.model.DefaultMusicCatalog
import com.example.musictube.data.repository.MusicRepository
import com.example.musictube.domain.model.Artist
import com.example.musictube.domain.model.Category
import com.example.musictube.domain.model.Playlist
import com.example.musictube.domain.model.Track
import com.example.musictube.playback.PlaybackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val recentlyPlayed: List<Track> = emptyList(),
    val recommendations: List<Track> = DefaultMusicCatalog.catalogTracks.take(8),
    val trending: List<Track> = DefaultMusicCatalog.catalogTracks.take(8),
    val popular: List<Track> = DefaultMusicCatalog.catalogTracks.sortedByDescending { it.viewCount }.take(10),
    val newReleases: List<Track> = DefaultMusicCatalog.catalogTracks.reversed().take(8),
    val popularArtists: List<Artist> = DefaultMusicCatalog.popularArtists,
    val categories: List<Category> = DefaultMusicCatalog.categories,
    val regionalMusic: List<Track> = DefaultMusicCatalog.catalogTracks.filter { it.category.equals("Bangla", ignoreCase = true) || it.category.equals("Bollywood", ignoreCase = true) },
    val hitSongs: List<Track> = DefaultMusicCatalog.catalogTracks.sortedByDescending { it.viewCount }.take(6),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val repository: MusicRepository = MusicTubeApplication.instance.repository,
    private val playbackManager: PlaybackManager = MusicTubeApplication.instance.playbackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        // Collect recently played
        viewModelScope.launch {
            repository.getRecentlyPlayed()
                .catch { /* ignore error to prevent crash */ }
                .collect { list ->
                    _uiState.update { it.copy(recentlyPlayed = list) }
                }
        }

        // Collect recommendations
        viewModelScope.launch {
            repository.getRecommendedTracks()
                .catch { /* ignore */ }
                .collect { list ->
                    if (list.isNotEmpty()) {
                        _uiState.update { it.copy(recommendations = list) }
                    }
                }
        }

        // Collect playlists
        viewModelScope.launch {
            repository.getPlaylists()
                .catch { /* ignore */ }
                .collect { list ->
                    _uiState.update { it.copy(playlists = list) }
                }
        }

        // Collect trending
        viewModelScope.launch {
            repository.getTrendingTracks()
                .catch { /* ignore */ }
                .collect { list ->
                    if (list.isNotEmpty()) {
                        _uiState.update { it.copy(trending = list) }
                    }
                }
        }

        // Collect popular
        viewModelScope.launch {
            repository.getPopularTracks()
                .catch { /* ignore */ }
                .collect { list ->
                    if (list.isNotEmpty()) {
                        _uiState.update { it.copy(popular = list, hitSongs = list.take(6)) }
                    }
                }
        }

        // Collect new releases
        viewModelScope.launch {
            repository.getNewReleases()
                .catch { /* ignore */ }
                .collect { list ->
                    if (list.isNotEmpty()) {
                        _uiState.update { it.copy(newReleases = list) }
                    }
                }
        }

        // Collect regional
        viewModelScope.launch {
            repository.getCategoryTracks("Bangla")
                .catch { /* ignore */ }
                .collect { list ->
                    if (list.isNotEmpty()) {
                        _uiState.update { it.copy(regionalMusic = list) }
                    }
                }
        }
    }

    fun playTrack(track: Track, queue: List<Track>) {
        playbackManager.playTrack(track, queue)
    }

    fun playNext(track: Track) {
        playbackManager.playNext(track)
    }

    fun addToQueue(track: Track) {
        playbackManager.addToQueue(track)
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            repository.toggleFavorite(track)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
        }
    }

    fun createPlaylist(name: String, initialTrack: Track? = null) {
        viewModelScope.launch {
            val playlistId = repository.createPlaylist(name)
            if (initialTrack != null) {
                repository.addTrackToPlaylist(playlistId, initialTrack)
            }
        }
    }
}
