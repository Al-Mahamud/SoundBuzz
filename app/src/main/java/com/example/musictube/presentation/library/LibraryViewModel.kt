package com.example.musictube.presentation.library

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

class LibraryViewModel(
    private val repository: MusicRepository = MusicTubeApplication.instance.repository,
    private val playbackManager: PlaybackManager = MusicTubeApplication.instance.playbackManager
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0) // 0: Playlists, 1: Favorites, 2: History
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    val playlists: StateFlow<List<Playlist>> = repository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Track>> = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<Track>> = repository.getRecentlyPlayed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun playTrack(track: Track, queue: List<Track>) {
        playbackManager.playTrack(track, queue)
    }

    fun playAll(tracks: List<Track>) {
        if (tracks.isNotEmpty()) {
            playbackManager.playTrack(tracks.first(), tracks)
        }
    }

    fun shufflePlay(tracks: List<Track>) {
        if (tracks.isNotEmpty()) {
            val shuffled = tracks.shuffled()
            playbackManager.playTrack(shuffled.first(), shuffled)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearPlaybackHistory()
        }
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
}
