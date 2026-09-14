package com.example.musictube.presentation.playlist

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

class PlaylistDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: MusicRepository = MusicTubeApplication.instance.repository,
    private val playbackManager: PlaybackManager = MusicTubeApplication.instance.playbackManager
) : ViewModel() {

    val playlistId: Long = checkNotNull(savedStateHandle["playlistId"]).toString().toLongOrNull() ?: 0L

    private val _playlist = MutableStateFlow<Playlist?>(null)
    val playlist: StateFlow<Playlist?> = _playlist.asStateFlow()

    val tracks: StateFlow<List<Track>> = repository.getPlaylistTracks(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadPlaylistInfo()
    }

    private fun loadPlaylistInfo() {
        viewModelScope.launch {
            repository.getPlaylists().collect { list ->
                _playlist.value = list.firstOrNull { it.id == playlistId }
            }
        }
    }

    fun playTrack(track: Track) {
        val currentTracks = tracks.value
        playbackManager.playTrack(track, currentTracks)
    }

    fun playAll() {
        val currentTracks = tracks.value
        if (currentTracks.isNotEmpty()) {
            playbackManager.playTrack(currentTracks.first(), currentTracks)
        }
    }

    fun shufflePlay() {
        val currentTracks = tracks.value
        if (currentTracks.isNotEmpty()) {
            val shuffled = currentTracks.shuffled()
            playbackManager.playTrack(shuffled.first(), shuffled)
        }
    }

    fun removeTrack(videoId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, videoId)
        }
    }

    fun renamePlaylist(newName: String) {
        viewModelScope.launch {
            repository.renamePlaylist(playlistId, newName)
            loadPlaylistInfo()
        }
    }

    fun deletePlaylist(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            onDeleted()
        }
    }
}
