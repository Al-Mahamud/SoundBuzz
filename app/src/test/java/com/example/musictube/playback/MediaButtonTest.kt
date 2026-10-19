package com.example.musictube.playback

import android.content.Intent
import android.os.Looper
import android.view.KeyEvent
import com.example.musictube.MusicTubeApplication
import com.example.musictube.data.remote.model.DefaultMusicCatalog
import com.example.musictube.domain.model.PlayState
import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaButtonTest {

    /** Sends real MEDIA_BUTTON intents through the MediaSessionService, like a headset or earbuds would. */
    @Test
    fun headsetButtonsControlPlayback() {
        val manager = MusicTubeApplication.instance.playbackManager
        val tracks = DefaultMusicCatalog.catalogTracks.take(3)
        // Video mode without an attached YouTube view: deterministic, no network
        manager.toggleVideoVisibility()
        manager.playTrack(tracks[0], tracks)
        manager.play()
        idle()

        // What the lock screen / notification / Bluetooth display sees
        val sessionPlayer = SessionPlayer(Looper.getMainLooper(), manager)
        idle()
        assertEquals(tracks[0].title, sessionPlayer.mediaMetadata.title.toString())
        assertEquals(tracks[0].artist, sessionPlayer.mediaMetadata.artist.toString())
        assertTrue(sessionPlayer.isPlaying || sessionPlayer.playWhenReady)
        assertTrue(sessionPlayer.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT))
        assertTrue(sessionPlayer.hasNextMediaItem())
        assertEquals(3, sessionPlayer.mediaItemCount)
        sessionPlayer.release()

        val service = Robolectric.buildService(PlaybackService::class.java).create()
        idle()

        fun press(keyCode: Int) {
            val down = Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            service.withIntent(down).startCommand(0, 1)
            val up = Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, keyCode))
            service.withIntent(up).startCommand(0, 2)
            idle()
        }

        press(KeyEvent.KEYCODE_MEDIA_NEXT)
        assertEquals(1, manager.currentQueueIndex.value)

        press(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        assertEquals(0, manager.currentQueueIndex.value)

        manager.play(); idle()
        press(KeyEvent.KEYCODE_MEDIA_PAUSE)
        assertEquals(PlayState.PAUSED, manager.playerState.value.playState)

        press(KeyEvent.KEYCODE_MEDIA_PLAY)
        assertEquals(PlayState.PLAYING, manager.playerState.value.playState)

        press(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        assertEquals(PlayState.PAUSED, manager.playerState.value.playState)

        press(KeyEvent.KEYCODE_HEADSETHOOK)
        assertEquals(PlayState.PLAYING, manager.playerState.value.playState)

        // Double press of a single-button headset
        val before = manager.currentQueueIndex.value
        fun quick(keyCode: Int) {
            service.withIntent(Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))).startCommand(0, 3)
            service.withIntent(Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, keyCode))).startCommand(0, 4)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
        }
        quick(KeyEvent.KEYCODE_HEADSETHOOK); quick(KeyEvent.KEYCODE_HEADSETHOOK); idle()
        assertEquals(before + 1, manager.currentQueueIndex.value)

        quick(KeyEvent.KEYCODE_HEADSETHOOK); quick(KeyEvent.KEYCODE_HEADSETHOOK); quick(KeyEvent.KEYCODE_HEADSETHOOK); idle()
        assertEquals(before, manager.currentQueueIndex.value)

        service.destroy()
    }

    private fun idle() {
        // Lets Media3's double-tap window for play/pause and headset-hook keys elapse
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1000))
    }
}
