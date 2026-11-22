package com.example.musictube.playback

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.musictube.data.local.dao.TrackDao
import com.example.musictube.data.remote.model.DefaultMusicCatalog
import com.example.musictube.domain.model.Track
import com.example.musictube.utils.DiagnosticsLogger
import com.example.musictube.utils.YouTubeLinkParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Opens videos handed to SoundBuzz from outside the app: "Share → SoundBuzz" in the YouTube /
 * YouTube Music apps (ACTION_SEND) and YouTube links opened with SoundBuzz (ACTION_VIEW).
 */
class SharedLinkHandler(
    private val context: Context,
    private val playbackManager: PlaybackManager,
    private val trackDao: TrackDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    /** Set when a shared video starts playing; the UI expands the full player and clears it. */
    private val _openPlayerRequest = MutableStateFlow(false)
    val openPlayerRequest: StateFlow<Boolean> = _openPlayerRequest.asStateFlow()

    fun consumeOpenPlayerRequest() {
        _openPlayerRequest.value = false
    }

    /** Returns true when the intent carried something this handler took care of. */
    fun handle(intent: Intent?): Boolean {
        val text = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> return false
        }
        val shared = YouTubeLinkParser.parse(text)
        if (shared == null) {
            DiagnosticsLogger.logPlayer("SharedLink", "No YouTube video in shared content: $text")
            toast("That doesn't look like a YouTube video link")
            return true
        }

        // Same video already loaded (e.g. shared twice): just bring the player up
        if (playbackManager.playerState.value.currentTrack?.youtubeVideoId == shared.videoId) {
            if (shared.startSeconds > 0) playbackManager.seekTo(shared.startSeconds.toFloat())
            if (!playbackManager.isPlaybackActive) playbackManager.play()
            _openPlayerRequest.value = true
            return true
        }

        toast("Opening in SoundBuzz…")
        job?.cancel()
        job = scope.launch {
            try {
                val track = knownTrack(shared.videoId) ?: AudioStreamResolver.fetchTrack(shared.videoId)
                DiagnosticsLogger.logPlayer("SharedLink", "Playing shared ${track.title} [${track.youtubeVideoId}] from ${shared.startSeconds}s")
                playbackManager.playTrack(track, startSeconds = shared.startSeconds.toFloat())
                _openPlayerRequest.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DiagnosticsLogger.e("SharedLink", "Couldn't open shared video ${shared.videoId}", e)
                toast("Couldn't open that video. Check your connection and try again.")
            }
        }
        return true
    }

    /** Skips the network lookup for videos the app already has details for. */
    private suspend fun knownTrack(videoId: String): Track? =
        DefaultMusicCatalog.catalogTracks.firstOrNull { it.youtubeVideoId == videoId }
            ?: trackDao.getTrackById(videoId)?.toDomain()
                ?.takeIf { it.title.isNotBlank() && it.durationSeconds > 0 }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
