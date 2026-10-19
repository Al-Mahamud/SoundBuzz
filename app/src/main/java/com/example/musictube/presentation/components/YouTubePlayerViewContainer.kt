package com.example.musictube.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
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

    val lifecycleObserver = remember {
        object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                playbackManager.detachYouTubePlayer()
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            YouTubePlayerView(ctx).apply {
                enableAutomaticInitialization = false
                enableBackgroundPlayback(true)
                lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

                val options = IFramePlayerOptions.Builder(ctx)
                    .controls(1)
                    .rel(0)
                    .build()

                initialize(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        DiagnosticsLogger.logPlayer("onReady", "Player ready for: $videoId")
                        // PlaybackManager loads the current track at the current position
                        playbackManager.attachYouTubePlayer(youTubePlayer)
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
        onRelease = { playerView ->
            DiagnosticsLogger.logPlayer("onRelease", "Releasing YouTubePlayerView")
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            playbackManager.detachYouTubePlayer()
            playerView.release()
        }
    )
}
