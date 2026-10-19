package com.example.musictube.playback

import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.example.musictube.domain.model.PlayState
import com.example.musictube.domain.model.PlayerState
import com.example.musictube.domain.model.RepeatMode
import com.example.musictube.domain.model.Track
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Presents [PlaybackManager] to Android as a single Media3 [Player], whichever engine is active
 * (ExoPlayer audio stream or the YouTube video player). The MediaSession built on it receives
 * headset / Bluetooth buttons, lock screen, notification, car and watch controls.
 */
@OptIn(UnstableApi::class)
class SessionPlayer(
    looper: Looper,
    private val manager: PlaybackManager
) : SimpleBasePlayer(looper) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Last state handed to Media3, used to skip updates that only move the position forward
    private var lastSnapshot: Any? = null
    private var lastPositionMs = 0L
    private var lastPositionAtMs = 0L
    private var lastWasPlaying = false

    init {
        scope.launch {
            combine(manager.playerState, manager.queue, manager.currentQueueIndex) { state, queue, index ->
                Triple(state, queue, index)
            }.collect { (state, queue, index) ->
                val snapshot = Triple(state.copy(currentPositionSeconds = 0f), queue, index)
                if (snapshot != lastSnapshot || positionJumped(state)) {
                    lastSnapshot = snapshot
                    invalidateState()
                }
            }
        }
    }

    /** True when the position moved in a way controllers can't extrapolate (e.g. a seek in the app). */
    private fun positionJumped(state: PlayerState): Boolean {
        val elapsed = if (lastWasPlaying) SystemClock.elapsedRealtime() - lastPositionAtMs else 0L
        val expectedMs = lastPositionMs + elapsed
        return abs(state.currentPositionSeconds * 1000 - expectedMs) > POSITION_JUMP_TOLERANCE_MS
    }

    override fun getState(): State {
        val playerState = manager.playerState.value
        val track = playerState.currentTrack
            ?: return State.Builder()
                .setAvailableCommands(AVAILABLE_COMMANDS)
                .setPlaybackState(Player.STATE_IDLE)
                .setPlaylist(emptyList())
                .build()

        // Expose the whole queue so next/previous are known to be available
        val queue = manager.queue.value
        val queueIndex = manager.currentQueueIndex.value
        val inQueue = queueIndex in queue.indices && queue[queueIndex].youtubeVideoId == track.youtubeVideoId
        val items = if (inQueue) queue else listOf(track)
        val currentIndex = if (inQueue) queueIndex else 0

        val playing = playerState.playState == PlayState.PLAYING
        val buffering = playerState.playState == PlayState.BUFFERING
        val positionMs = (playerState.currentPositionSeconds * 1000).toLong().coerceAtLeast(0L)
        lastPositionMs = positionMs
        lastPositionAtMs = SystemClock.elapsedRealtime()
        lastWasPlaying = playing

        return State.Builder()
            .setAvailableCommands(AVAILABLE_COMMANDS)
            .setPlayWhenReady(playing || buffering, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(
                when (playerState.playState) {
                    PlayState.BUFFERING -> Player.STATE_BUFFERING
                    PlayState.ENDED -> Player.STATE_ENDED
                    else -> Player.STATE_READY
                }
            )
            .setPlaylist(
                items.mapIndexed { i, item ->
                    val durationSeconds = if (i == currentIndex) playerState.durationSeconds else item.durationSeconds.toFloat()
                    mediaItemData(i, item, durationSeconds)
                }
            )
            .setCurrentMediaItemIndex(currentIndex)
            .setContentPositionMs(
                if (playing) PositionSupplier.getExtrapolating(positionMs, 1f) else PositionSupplier.getConstant(positionMs)
            )
            .setShuffleModeEnabled(playerState.isShuffleEnabled)
            // PlaybackManager picks the next track itself when shuffling, so "next" must always be allowed then
            .setRepeatMode(
                when {
                    playerState.isShuffleEnabled -> Player.REPEAT_MODE_ALL
                    playerState.repeatMode == RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                    playerState.repeatMode == RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
            )
            .build()
    }

    private fun mediaItemData(index: Int, track: Track, durationSeconds: Float): MediaItemData {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setArtworkUri(track.thumbnailUrl.takeIf { it.isNotBlank() }?.let(Uri::parse))
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .build()
        return MediaItemData.Builder("$index:${track.youtubeVideoId}")
            .setMediaItem(
                MediaItem.Builder()
                    .setMediaId(track.youtubeVideoId)
                    .setMediaMetadata(metadata)
                    .build()
            )
            .setDurationUs(if (durationSeconds > 0f) (durationSeconds * 1_000_000).toLong() else C.TIME_UNSET)
            .setIsSeekable(true)
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) manager.play() else manager.pause()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        val position = positionMs.coerceAtLeast(0L) / 1000f
        val isCurrentItem = mediaItemIndex == currentMediaItemIndex
        when (seekCommand) {
            Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> manager.next()
            // Media3 already decided between "restart this song" (same item) and "previous song"
            Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM ->
                if (isCurrentItem) manager.seekTo(position) else manager.previous()
            Player.COMMAND_SEEK_TO_MEDIA_ITEM, Player.COMMAND_SEEK_TO_DEFAULT_POSITION ->
                if (isCurrentItem) manager.seekTo(position) else manager.playFromQueue(mediaItemIndex)
            else -> manager.seekTo(position)
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        manager.stopAndDismiss()
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> {
        scope.cancel()
        return Futures.immediateVoidFuture()
    }

    private companion object {
        const val POSITION_JUMP_TOLERANCE_MS = 1500f

        val AVAILABLE_COMMANDS: Player.Commands = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_STOP,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_TIMELINE,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_RELEASE
            )
            .build()
    }
}
