package com.example.musictube.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.musictube.MusicTubeApplication
import com.example.musictube.playback.PlaybackManager
import com.example.musictube.utils.DiagnosticsLogger
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
fun YouTubePlayerViewContainer(
    videoId: String,
    modifier: Modifier = Modifier,
    playbackManager: PlaybackManager = MusicTubeApplication.instance.playbackManager
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var activePlayer by remember { mutableStateOf<YouTubePlayer?>(null) }
    var loadedVideoId by remember { mutableStateOf<String?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            YouTubePlayerView(ctx).apply {
                enableAutomaticInitialization = false
                lifecycleOwner.lifecycle.addObserver(this)

                val options = IFramePlayerOptions.Builder(ctx)
                    .controls(1)
                    .rel(0)
                    .build()

                initialize(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        activePlayer = youTubePlayer
                        loadedVideoId = videoId
                        playbackManager.attachYouTubePlayer(youTubePlayer)
                        DiagnosticsLogger.logPlayer("onReady", "Player ready! Auto-loading: $videoId")
                        if (videoId.isNotBlank()) {
                            youTubePlayer.loadVideo(videoId, 0f)
                        }
                    }

                    override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                        playbackManager.onPlayerStateChange(state)
                    }

                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        playbackManager.onCurrentSecond(second)
                    }

                    override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                        playbackManager.onVideoDuration(duration)
                    }

                    override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                        playbackManager.onPlayerError(error)
                    }
                }, options)
            }
        },
        update = { _ ->
            if (activePlayer != null && loadedVideoId != videoId && videoId.isNotBlank()) {
                loadedVideoId = videoId
                DiagnosticsLogger.logPlayer("update", "Video changed, loading: $videoId")
                activePlayer?.loadVideo(videoId, 0f)
            }
        },
        onRelease = { playerView ->
            DiagnosticsLogger.logPlayer("onRelease", "Releasing YouTubePlayerView")
            lifecycleOwner.lifecycle.removeObserver(playerView)
            playbackManager.detachYouTubePlayer()
            playerView.release()
        }
    )
}
