package com.example.musictube.presentation.artist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musictube.MusicTubeApplication
import com.example.musictube.data.repository.MusicRepository
import com.example.musictube.domain.model.Artist
import com.example.musictube.domain.model.Playlist
import com.example.musictube.domain.model.Track
import com.example.musictube.playback.PlaybackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArtistDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: MusicRepository = MusicTubeApplication.instance.repository,
    private val playbackManager: PlaybackManager = MusicTubeApplication.instance.playbackManager
) : ViewModel() {

    val artistName: String = java.net.URLDecoder.decode(checkNotNull(savedStateHandle["artistName"]), "UTF-8")

    private val _artist = MutableStateFlow<Artist?>(null)
    val artist: StateFlow<Artist?> = _artist.asStateFlow()

    private val _relatedTracks = MutableStateFlow<List<Track>>(emptyList())
    val relatedTracks: StateFlow<List<Track>> = _relatedTracks.asStateFlow()

    val playlists: StateFlow<List<Playlist>> = repository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadArtistData()
    }

    private fun loadArtistData() {
        viewModelScope.launch {
            repository.getArtistDetails(artistName).collect { a ->
                _artist.value = a
            }
        }
        viewModelScope.launch {
            repository.searchTracks(artistName).collect { tracks ->
                _relatedTracks.value = tracks
            }
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
