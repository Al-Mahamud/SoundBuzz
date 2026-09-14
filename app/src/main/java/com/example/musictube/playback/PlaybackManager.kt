package com.example.musictube.playback

import com.example.musictube.data.repository.MusicRepository
import com.example.musictube.domain.model.PlayState
import com.example.musictube.domain.model.PlayerState
import com.example.musictube.domain.model.RepeatMode
import com.example.musictube.domain.model.Track
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import android.content.Context
import android.view.ViewGroup
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlaybackManager(
    private val repository: MusicRepository,
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(-1)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private var activeYouTubePlayer: YouTubePlayer? = null
    private var isPlayerReady = false

    fun attachYouTubePlayer(player: YouTubePlayer) {
        activeYouTubePlayer = player
        isPlayerReady = true
        com.example.musictube.utils.DiagnosticsLogger.logPlayer("attachYouTubePlayer", "Player attached to PlaybackManager")
        _playerState.value.currentTrack?.let { track ->
            if (_playerState.value.playState == PlayState.PLAYING || _playerState.value.playState == PlayState.BUFFERING) {
                com.example.musictube.utils.DiagnosticsLogger.logPlayer("attachYouTubePlayer -> loadVideo", track.youtubeVideoId)
                player.loadVideo(track.youtubeVideoId, _playerState.value.currentPositionSeconds)
            }
        }
    }

    fun detachYouTubePlayer() {
        activeYouTubePlayer = null
        isPlayerReady = false
        com.example.musictube.utils.DiagnosticsLogger.logPlayer("detachYouTubePlayer", "Player detached from PlaybackManager")
    }

    fun onPlayerStateChange(state: PlayerConstants.PlayerState) {
        com.example.musictube.utils.DiagnosticsLogger.logPlayer("onStateChange", state.name)
        when (state) {
            PlayerConstants.PlayerState.PLAYING -> {
                _playerState.update { it.copy(playState = PlayState.PLAYING, errorMessage = null) }
            }
            PlayerConstants.PlayerState.PAUSED -> {
                _playerState.update { it.copy(playState = PlayState.PAUSED) }
            }
            PlayerConstants.PlayerState.BUFFERING -> {
                _playerState.update { it.copy(playState = PlayState.BUFFERING) }
            }
            PlayerConstants.PlayerState.ENDED -> {
                handleTrackEnded()
            }
            else -> {}
        }
    }

    fun onCurrentSecond(second: Float) {
        _playerState.update { it.copy(currentPositionSeconds = second) }
    }

    fun onVideoDuration(duration: Float) {
        _playerState.update { it.copy(durationSeconds = duration) }
    }

    fun onPlayerError(error: PlayerConstants.PlayerError) {
        com.example.musictube.utils.DiagnosticsLogger.logPlayer("onError", "${error.name} (Code: $error)")
        val msg = when (error) {
            PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER ->
                "Playback restricted on this device. Skipping to next track..."
            PlayerConstants.PlayerError.VIDEO_NOT_FOUND ->
                "Video not found on YouTube. Skipping to next track..."
            else ->
                "Playback error: ${error.name}"
        }
        _playerState.update { it.copy(playState = PlayState.ERROR, errorMessage = msg) }
        scope.launch {
            kotlinx.coroutines.delay(2000)
            next()
        }
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        if (newQueue != null && newQueue.isNotEmpty()) {
            _queue.value = newQueue
            val index = newQueue.indexOfFirst { it.youtubeVideoId == track.youtubeVideoId }
            _currentQueueIndex.value = if (index >= 0) index else 0
        } else {
            // If track is not in existing queue, append it
            val currentList = _queue.value.toMutableList()
            var index = currentList.indexOfFirst { it.youtubeVideoId == track.youtubeVideoId }
            if (index == -1) {
                currentList.add(track)
                index = currentList.lastIndex
                _queue.value = currentList
            }
            _currentQueueIndex.value = index
        }

        _playerState.update {
            it.copy(
                currentTrack = track,
                playState = PlayState.BUFFERING,
                currentPositionSeconds = 0f,
                durationSeconds = if (track.durationSeconds > 0) track.durationSeconds.toFloat() else 0f,
                errorMessage = null
            )
        }

        com.example.musictube.utils.DiagnosticsLogger.logPlayer("playTrack", "${track.title} [${track.youtubeVideoId}]")
        val player = activeYouTubePlayer
        if (player != null && isPlayerReady) {
            com.example.musictube.utils.DiagnosticsLogger.logPlayer("playTrack", "player.loadVideo(${track.youtubeVideoId})")
            player.loadVideo(track.youtubeVideoId, 0f)
            _playerState.update { it.copy(playState = PlayState.PLAYING, errorMessage = null) }
        }

        // Record history
        scope.launch {
            repository.recordTrackPlayed(track)
        }
    }

    fun togglePlayPause() {
        val currentState = _playerState.value.playState
        val player = activeYouTubePlayer
        if (currentState == PlayState.PLAYING) {
            player?.pause()
            _playerState.update { it.copy(playState = PlayState.PAUSED) }
        } else if (currentState == PlayState.PAUSED || currentState == PlayState.IDLE) {
            val track = _playerState.value.currentTrack
            if (track != null) {
                player?.play()
                _playerState.update { it.copy(playState = PlayState.PLAYING) }
            }
        }
    }

    fun seekTo(seconds: Float) {
        _playerState.update { it.copy(currentPositionSeconds = seconds) }
        activeYouTubePlayer?.seekTo(seconds)
    }

    fun next() {
        val q = _queue.value
        if (q.isEmpty()) return
        val currentIndex = _currentQueueIndex.value
        val nextIndex = if (_playerState.value.isShuffleEnabled) {
            (q.indices - currentIndex).randomOrNull() ?: currentIndex
        } else {
            currentIndex + 1
        }

        if (nextIndex in q.indices) {
            _currentQueueIndex.value = nextIndex
            playTrack(q[nextIndex])
        } else if (_playerState.value.repeatMode == RepeatMode.ALL) {
            _currentQueueIndex.value = 0
            playTrack(q[0])
        }
    }

    fun previous() {
        // If played more than 3 seconds, restart current track
        if (_playerState.value.currentPositionSeconds > 3f) {
            seekTo(0f)
            return
        }

        val q = _queue.value
        if (q.isEmpty()) return
        val prevIndex = _currentQueueIndex.value - 1
        if (prevIndex in q.indices) {
            _currentQueueIndex.value = prevIndex
            playTrack(q[prevIndex])
        } else {
            seekTo(0f)
        }
    }

    fun toggleShuffle() {
        _playerState.update { it.copy(isShuffleEnabled = !it.isShuffleEnabled) }
    }

    fun toggleRepeat() {
        _playerState.update {
            val nextRepeat = when (it.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            it.copy(repeatMode = nextRepeat)
        }
    }

    fun toggleVideoVisibility() {
        _playerState.update { it.copy(isVideoVisible = !it.isVideoVisible) }
    }

    fun playNext(track: Track) {
        val q = _queue.value.toMutableList()
        val insertIndex = (_currentQueueIndex.value + 1).coerceIn(0, q.size)
        q.add(insertIndex, track)
        _queue.value = q
    }

    fun addToQueue(track: Track) {
        val q = _queue.value.toMutableList()
        q.add(track)
        _queue.value = q
    }

    fun removeFromQueue(index: Int) {
        val q = _queue.value.toMutableList()
        if (index in q.indices) {
            q.removeAt(index)
            _queue.value = q
            if (index < _currentQueueIndex.value) {
                _currentQueueIndex.value -= 1
            } else if (index == _currentQueueIndex.value) {
                if (q.isNotEmpty()) {
                    val newIndex = index.coerceAtMost(q.lastIndex)
                    _currentQueueIndex.value = newIndex
                    playTrack(q[newIndex])
                } else {
                    _currentQueueIndex.value = -1
                    _playerState.update { it.copy(currentTrack = null, playState = PlayState.IDLE) }
                }
            }
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentQueueIndex.value = -1
        _playerState.update { it.copy(currentTrack = null, playState = PlayState.IDLE) }
    }

    fun shuffleQueue() {
        val currentTrack = _playerState.value.currentTrack
        val q = _queue.value.toMutableList()
        if (currentTrack != null) {
            q.remove(currentTrack)
            q.shuffle()
            q.add(0, currentTrack)
            _queue.value = q
            _currentQueueIndex.value = 0
        } else {
            q.shuffle()
            _queue.value = q
        }
    }

    private fun handleTrackEnded() {
        when (_playerState.value.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0f)
                activeYouTubePlayer?.play()
            }
            RepeatMode.ALL -> {
                next()
            }
            RepeatMode.OFF -> {
                val nextIndex = _currentQueueIndex.value + 1
                if (nextIndex in _queue.value.indices) {
                    next()
                } else {
                    _playerState.update { it.copy(playState = PlayState.PAUSED) }
                }
            }
        }
    }
}
