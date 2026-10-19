package com.example.musictube.playback

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.musictube.data.repository.MusicRepository
import com.example.musictube.domain.model.PlayState
import com.example.musictube.domain.model.PlayerState
import com.example.musictube.domain.model.RepeatMode
import com.example.musictube.domain.model.Track
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Two playback engines:
 * - Audio mode (default): audio-only stream resolved by [AudioStreamResolver], played by ExoPlayer.
 * - Video mode ("Watch Video"): the embedded YouTube IFrame player.
 * Only one engine is active at a time; toggling hands the position over to the other one.
 */
@OptIn(UnstableApi::class)
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

    private val isAudioMode: Boolean
        get() = !_playerState.value.isVideoVisible

    // Set when audio extraction failed and we fell back to the YouTube player for the current track
    private var autoSwitchedToVideo = false
    private var resolveJob: Job? = null
    private var progressJob: Job? = null
    private var retriedVideoId: String? = null

    private val audioListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (!isAudioMode) return
            when (playbackState) {
                Player.STATE_BUFFERING -> if (audioPlayer.playWhenReady) {
                    _playerState.update { it.copy(playState = PlayState.BUFFERING) }
                }
                Player.STATE_READY -> {
                    val durationMs = audioPlayer.duration
                    _playerState.update {
                        it.copy(
                            playState = if (audioPlayer.playWhenReady) PlayState.PLAYING else PlayState.PAUSED,
                            durationSeconds = if (durationMs != C.TIME_UNSET) durationMs / 1000f else it.durationSeconds,
                            errorMessage = null
                        )
                    }
                }
                Player.STATE_ENDED -> handleTrackEnded()
                else -> {}
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isAudioMode) return
            if (isPlaying) {
                _playerState.update { it.copy(playState = PlayState.PLAYING) }
                startProgressUpdates()
            } else {
                stopProgressUpdates()
                _playerState.update { it.copy(currentPositionSeconds = audioPlayer.currentPosition / 1000f) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (!isAudioMode) return
            com.example.musictube.utils.DiagnosticsLogger.logPlayer("audio onError", "${error.errorCodeName}: ${error.message}")
            val track = _playerState.value.currentTrack ?: return
            if (retriedVideoId != track.youtubeVideoId) {
                // Stream links expire or get rejected; resolve a fresh one once before giving up
                retriedVideoId = track.youtubeVideoId
                startAudio(track, audioPlayer.currentPosition / 1000f, playWhenReady = true, forceRefresh = true)
            } else {
                fallbackToVideo()
            }
        }
    }

    private val audioPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(YouTubeAudioDataSource.factory()))
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()
        .apply { addListener(audioListener) }

    private fun startAudio(track: Track, startSeconds: Float, playWhenReady: Boolean, forceRefresh: Boolean = false) {
        resolveJob?.cancel()
        stopProgressUpdates()
        audioPlayer.stop()
        resolveJob = scope.launch {
            try {
                val audio = AudioStreamResolver.resolve(track.youtubeVideoId, forceRefresh)
                if (!isAudioMode || _playerState.value.currentTrack?.youtubeVideoId != track.youtubeVideoId) return@launch
                audioPlayer.setMediaItem(MediaItem.fromUri(audio.url), (startSeconds * 1000).toLong())
                audioPlayer.playWhenReady = playWhenReady
                audioPlayer.prepare()
                _playerState.update { it.copy(audioFormat = audio.label) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                com.example.musictube.utils.DiagnosticsLogger.logPlayer("startAudio", "Audio resolve failed for ${track.youtubeVideoId}: ${e.message}")
                if (isAudioMode && _playerState.value.currentTrack?.youtubeVideoId == track.youtubeVideoId) {
                    fallbackToVideo()
                }
            }
        }
    }

    private fun stopAudio() {
        resolveJob?.cancel()
        stopProgressUpdates()
        audioPlayer.stop()
        audioPlayer.clearMediaItems()
    }

    /** Plays the current track through the YouTube player when no audio-only stream works. */
    private fun fallbackToVideo() {
        autoSwitchedToVideo = true
        _playerState.update {
            it.copy(
                isVideoVisible = true,
                audioFormat = null,
                playState = PlayState.BUFFERING,
                errorMessage = "Audio-only stream unavailable. Playing through the YouTube player instead."
            )
        }
        stopAudio()
        val track = _playerState.value.currentTrack ?: return
        activeYouTubePlayer?.loadVideo(track.youtubeVideoId, _playerState.value.currentPositionSeconds)
    }

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                _playerState.update { it.copy(currentPositionSeconds = audioPlayer.currentPosition / 1000f) }
                delay(500)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    fun attachYouTubePlayer(player: YouTubePlayer) {
        activeYouTubePlayer = player
        isPlayerReady = true
        com.example.musictube.utils.DiagnosticsLogger.logPlayer("attachYouTubePlayer", "Player attached to PlaybackManager")
        if (isAudioMode) return
        _playerState.value.currentTrack?.let { track ->
            val position = _playerState.value.currentPositionSeconds
            if (_playerState.value.playState == PlayState.PLAYING || _playerState.value.playState == PlayState.BUFFERING) {
                com.example.musictube.utils.DiagnosticsLogger.logPlayer("attachYouTubePlayer -> loadVideo", track.youtubeVideoId)
                player.loadVideo(track.youtubeVideoId, position)
            } else {
                player.cueVideo(track.youtubeVideoId, position)
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
        if (isAudioMode) return
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
        if (isAudioMode) return
        _playerState.update { it.copy(currentPositionSeconds = second) }
    }

    fun onVideoDuration(duration: Float) {
        if (isAudioMode) return
        _playerState.update { it.copy(durationSeconds = duration) }
    }

    fun onPlayerError(error: PlayerConstants.PlayerError) {
        com.example.musictube.utils.DiagnosticsLogger.logPlayer("onError", "${error.name} (Code: $error)")
        if (isAudioMode) return
        val shouldSkip = when (error) {
            PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER -> true
            PlayerConstants.PlayerError.VIDEO_NOT_FOUND -> true
            else -> false
        }
        val msg = when (error) {
            PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER ->
                "Video embedding restricted by uploader. Skipping to next track..."
            PlayerConstants.PlayerError.VIDEO_NOT_FOUND ->
                "Video not found on YouTube. Skipping to next track..."
            else ->
                "Playback error: ${error.name}. Tap play to retry."
        }
        _playerState.update { it.copy(playState = PlayState.ERROR, errorMessage = msg) }
        if (shouldSkip) {
            scope.launch {
                kotlinx.coroutines.delay(2000)
                next()
            }
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

        // A fallback to the YouTube player only applies to the track whose audio failed
        val revertToAudio = autoSwitchedToVideo
        autoSwitchedToVideo = false
        retriedVideoId = null

        _playerState.update {
            it.copy(
                currentTrack = track,
                playState = PlayState.BUFFERING,
                currentPositionSeconds = 0f,
                durationSeconds = if (track.durationSeconds > 0) track.durationSeconds.toFloat() else 0f,
                isVideoVisible = if (revertToAudio) false else it.isVideoVisible,
                audioFormat = null,
                errorMessage = null
            )
        }

        com.example.musictube.utils.DiagnosticsLogger.logPlayer("playTrack", "${track.title} [${track.youtubeVideoId}] audioMode=$isAudioMode")
        if (isAudioMode) {
            activeYouTubePlayer?.pause()
            startAudio(track, 0f, playWhenReady = true)
        } else {
            stopAudio()
            val player = activeYouTubePlayer
            if (player != null && isPlayerReady) {
                com.example.musictube.utils.DiagnosticsLogger.logPlayer("playTrack", "player.loadVideo(${track.youtubeVideoId})")
                player.loadVideo(track.youtubeVideoId, 0f)
                _playerState.update { it.copy(playState = PlayState.PLAYING, errorMessage = null) }
            }
        }

        // Start persistent foreground media service
        PlaybackService.startService(context)

        // Record history
        scope.launch {
            repository.recordTrackPlayed(track)
        }
    }

    fun togglePlayPause() {
        val currentState = _playerState.value.playState
        val player = activeYouTubePlayer
        if (currentState == PlayState.PLAYING) {
            if (isAudioMode) audioPlayer.pause() else player?.pause()
            _playerState.update { it.copy(playState = PlayState.PAUSED) }
        } else if (currentState == PlayState.PAUSED || currentState == PlayState.IDLE) {
            val track = _playerState.value.currentTrack
            if (track != null) {
                if (isAudioMode) {
                    if (audioPlayer.mediaItemCount == 0 || audioPlayer.playbackState == Player.STATE_IDLE) {
                        startAudio(track, _playerState.value.currentPositionSeconds, playWhenReady = true)
                    } else {
                        if (audioPlayer.playbackState == Player.STATE_ENDED) audioPlayer.seekTo(0)
                        audioPlayer.play()
                    }
                } else {
                    player?.play()
                }
                _playerState.update { it.copy(playState = PlayState.PLAYING) }
            }
        }
    }

    fun seekTo(seconds: Float) {
        _playerState.update { it.copy(currentPositionSeconds = seconds) }
        if (isAudioMode) {
            audioPlayer.seekTo((seconds * 1000).toLong())
        } else {
            activeYouTubePlayer?.seekTo(seconds)
        }
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
        val state = _playerState.value
        val track = state.currentTrack
        val wasPlaying = state.playState == PlayState.PLAYING || state.playState == PlayState.BUFFERING
        autoSwitchedToVideo = false

        if (state.isVideoVisible) {
            // Video -> audio: the YouTube view leaves composition, ExoPlayer resumes from the same spot
            activeYouTubePlayer?.pause()
            _playerState.update { it.copy(isVideoVisible = false, errorMessage = null) }
            if (track != null) startAudio(track, state.currentPositionSeconds, playWhenReady = wasPlaying)
        } else {
            // Audio -> video: the YouTube view is composed and attachYouTubePlayer() resumes playback
            _playerState.update { it.copy(isVideoVisible = true, audioFormat = null, errorMessage = null) }
            stopAudio()
            if (track != null) {
                activeYouTubePlayer?.let { player ->
                    if (wasPlaying) {
                        player.loadVideo(track.youtubeVideoId, state.currentPositionSeconds)
                    } else {
                        player.cueVideo(track.youtubeVideoId, state.currentPositionSeconds)
                    }
                }
            }
        }
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
                    stopAudio()
                    _playerState.update { it.copy(currentTrack = null, playState = PlayState.IDLE) }
                }
            }
        }
    }

    fun clearQueue() {
        stopAudio()
        _queue.value = emptyList()
        _currentQueueIndex.value = -1
        _playerState.update { it.copy(currentTrack = null, playState = PlayState.IDLE) }
        PlaybackService.stopService(context)
    }

    fun stopAndDismiss() {
        try {
            activeYouTubePlayer?.pause()
        } catch (e: Exception) {
            // ignore
        }
        stopAudio()
        // Keep the user's audio/video choice for the next session (but not an automatic fallback)
        val keepVideoMode = _playerState.value.isVideoVisible && !autoSwitchedToVideo
        autoSwitchedToVideo = false
        _queue.value = emptyList()
        _currentQueueIndex.value = -1
        _playerState.update { PlayerState(isVideoVisible = keepVideoMode) }
        PlaybackService.stopService(context)
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
                if (isAudioMode) audioPlayer.play() else activeYouTubePlayer?.play()
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
