package com.example.musictube.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musictube.MusicTubeApplication
import com.example.musictube.data.repository.MusicRepository
import com.example.musictube.domain.model.Playlist
import com.example.musictube.domain.model.PlayerState
import com.example.musictube.domain.model.Track
import com.example.musictube.playback.PlaybackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val repository: MusicRepository = MusicTubeApplication.instance.repository,
    private val playbackManager: PlaybackManager = MusicTubeApplication.instance.playbackManager
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = playbackManager.playerState
    val queue: StateFlow<List<Track>> = playbackManager.queue
    val currentQueueIndex: StateFlow<Int> = playbackManager.currentQueueIndex

    val playlists: StateFlow<List<Playlist>> = repository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isFavorite: StateFlow<Boolean> = playerState
        .flatMapLatest { state ->
            val videoId = state.currentTrack?.youtubeVideoId
            if (videoId != null) {
                repository.isFavorite(videoId)
            } else {
                flowOf(false)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }

    fun seekTo(seconds: Float) {
        playbackManager.seekTo(seconds)
    }

    fun next() {
        playbackManager.next()
    }

    fun previous() {
        playbackManager.previous()
    }

    fun toggleShuffle() {
        playbackManager.toggleShuffle()
    }

    fun toggleRepeat() {
        playbackManager.toggleRepeat()
    }

    fun toggleVideoVisibility() {
        playbackManager.toggleVideoVisibility()
    }

    fun toggleFavorite() {
        val track = playerState.value.currentTrack ?: return
        viewModelScope.launch {
            repository.toggleFavorite(track)
        }
    }

    fun playFromQueue(index: Int) {
        val q = queue.value
        if (index in q.indices) {
            playbackManager.playTrack(q[index])
        }
    }

    fun removeFromQueue(index: Int) {
        playbackManager.removeFromQueue(index)
    }

    fun clearQueue() {
        playbackManager.clearQueue()
    }

    fun shuffleQueue() {
        playbackManager.shuffleQueue()
    }

    fun addTrackToPlaylist(playlistId: Long) {
        val track = playerState.value.currentTrack ?: return
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
        }
    }

    fun createPlaylist(name: String) {
        val track = playerState.value.currentTrack ?: return
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            repository.addTrackToPlaylist(id, track)
        }
    }
}
