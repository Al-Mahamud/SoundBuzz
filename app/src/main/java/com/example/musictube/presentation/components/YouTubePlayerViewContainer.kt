package com.example.musictube.presentation.components

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.musictube.MusicTubeApplication
import com.example.musictube.playback.PlaybackManager

@Composable
fun YouTubePlayerViewContainer(
    playbackManager: PlaybackManager = MusicTubeApplication.instance.playbackManager,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val playerView = playbackManager.getSharedPlayerView(context)
        lifecycleOwner.lifecycle.addObserver(playerView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(playerView)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val playerView = playbackManager.getSharedPlayerView(ctx)
            (playerView.parent as? ViewGroup)?.removeView(playerView)
            playerView
        }
    )
}
