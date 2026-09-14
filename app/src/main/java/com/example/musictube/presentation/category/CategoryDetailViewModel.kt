package com.example.musictube.presentation.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musictube.MusicTubeApplication
import com.example.musictube.data.repository.MusicRepository
import com.example.musictube.domain.model.Playlist
import com.example.musictube.domain.model.Track
import com.example.musictube.playback.PlaybackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryDetailUiState(
    val categoryName: String = "",
    val popular: List<Track> = emptyList(),
    val trending: List<Track> = emptyList(),
    val latest: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CategoryDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: MusicRepository = MusicTubeApplication.instance.repository,
    private val playbackManager: PlaybackManager = MusicTubeApplication.instance.playbackManager
) : ViewModel() {

    val categoryName: String = java.net.URLDecoder.decode(checkNotNull(savedStateHandle["categoryName"]), "UTF-8")

    private val _uiState = MutableStateFlow(CategoryDetailUiState(categoryName = categoryName, isLoading = true))
    val uiState: StateFlow<CategoryDetailUiState> = _uiState.asStateFlow()

    init {
        loadCategoryTracks()
        loadPlaylists()
    }

    private fun loadCategoryTracks() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                repository.getCategoryTracks(categoryName).collect { tracks ->
                    _uiState.value = _uiState.value.copy(
                        popular = tracks.sortedByDescending { it.viewCount },
                        trending = tracks.shuffled(),
                        latest = tracks.reversed(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Failed to load category"
                )
            }
        }
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            repository.getPlaylists().collect { pl ->
                _uiState.value = _uiState.value.copy(playlists = pl)
            }
        }
    }

    fun playTrack(track: Track, queue: List<Track>) {
        playbackManager.playTrack(track, queue)
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

    fun createPlaylist(name: String, track: Track?) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            if (track != null) {
                repository.addTrackToPlaylist(id, track)
            }
        }
    }
}
