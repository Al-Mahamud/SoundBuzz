package com.example.musictube.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musictube.MusicTubeApplication
import com.example.musictube.data.repository.MusicRepository
import com.example.musictube.domain.model.Playlist
import com.example.musictube.domain.model.Track
import com.example.musictube.playback.PlaybackManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<Track> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: MusicRepository = MusicTubeApplication.instance.repository,
    private val playbackManager: PlaybackManager = MusicTubeApplication.instance.playbackManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    val recentSearches: StateFlow<List<String>> = repository.getRecentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults: StateFlow<List<Track>> = _query
        .debounce(400)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) {
                _isLoading.value = false
                flowOf(emptyList())
            } else {
                _isLoading.value = true
                _error.value = null
                repository.addSearchQuery(q)
                repository.searchTracks(q)
                    .catch { e ->
                        _error.value = e.localizedMessage ?: "Search failed"
                        _isLoading.value = false
                        emit(emptyList())
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.getPlaylists().collect {
                _playlists.value = it
            }
        }
        viewModelScope.launch {
            searchResults.collect {
                _isLoading.value = false
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun retrySearch() {
        val current = _query.value
        _query.value = ""
        _query.value = current
    }

    fun clearSearch() {
        _query.value = ""
    }

    fun deleteRecentSearch(q: String) {
        viewModelScope.launch {
            repository.deleteSearchQuery(q)
        }
    }

    fun clearAllRecentSearches() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    fun playTrack(track: Track, list: List<Track>) {
        playbackManager.playTrack(track, list)
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

    fun createPlaylist(name: String, track: Track?) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            if (track != null) {
                repository.addTrackToPlaylist(id, track)
            }
        }
    }
}
