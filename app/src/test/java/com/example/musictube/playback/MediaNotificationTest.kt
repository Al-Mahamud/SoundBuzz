package com.example.musictube.playback

import android.app.Notification
import android.app.NotificationManager
import android.os.Looper
import com.example.musictube.MusicTubeApplication
import com.example.musictube.data.remote.model.DefaultMusicCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaNotificationTest {

    /** The live media notification (also drives lock screen / quick settings controls) follows playback. */
    @Test
    fun mediaNotificationFollowsPlayback() {
        val notifications = shadowOf(RuntimeEnvironment.getApplication().getSystemService(NotificationManager::class.java))
        val manager = MusicTubeApplication.instance.playbackManager
        val tracks = DefaultMusicCatalog.catalogTracks.take(3)
        // Video mode without an attached YouTube view: deterministic, no network
        manager.toggleVideoVisibility()
        manager.playTrack(tracks[0], tracks)
        manager.play()
        idle()

        val service = Robolectric.buildService(PlaybackService::class.java).create().startCommand(0, 1)
        idle()

        assertNotNull("service must run in the foreground while playing", shadowOf(service.get()).lastForegroundNotification)
        var notification = notifications.allNotifications.single()
        assertEquals(Notification.MediaStyle::class.java.name, notification.extras.getString(Notification.EXTRA_TEMPLATE))
        assertTrue(notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION))
        assertEquals(tracks[0].title, notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals(listOf("Seek to previous item", "Pause", "Seek to next item"), notification.actions.map { it.title.toString() })

        manager.next()
        manager.play()
        idle()
        notification = notifications.allNotifications.single()
        assertEquals(tracks[1].title, notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals(tracks[1].artist, notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())

        manager.pause()
        idle()
        notification = notifications.allNotifications.single()
        assertEquals("Play", notification.actions[1].title.toString())

        service.destroy()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1500))
}
