package com.example.musictube.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.core.content.IntentCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.musictube.MainActivity
import com.example.musictube.MusicTubeApplication
import com.example.musictube.R
import com.example.musictube.utils.DiagnosticsLogger

/**
 * Hosts the MediaSession. Android routes headset/earbud buttons, lock screen, notification,
 * Bluetooth car and watch controls to it; Media3 builds the media notification and keeps the
 * service in the foreground while something is playing.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var headsetClicks: HeadsetClickHandler? = null

    override fun onCreate() {
        super.onCreate()
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playbackManager = MusicTubeApplication.instance.playbackManager
        val clicks = HeadsetClickHandler(Handler(mainLooper), onClicks = { count ->
            when (count) {
                1 -> playbackManager.togglePlayPause()
                2 -> playbackManager.next()
                else -> playbackManager.previous()
            }
        })
        headsetClicks = clicks

        val player = SessionPlayer(mainLooper, playbackManager)
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onMediaButtonEvent(
                    session: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo,
                    intent: Intent
                ): Boolean {
                    val event = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                        ?: return false
                    // Play/pause and headset-hook presses are counted; other keys use Media3's default handling
                    return clicks.onKeyEvent(event)
                }
            })
            .build()

        // Nothing in the app binds a MediaController, so register the session ourselves; this is what
        // makes Media3 post the media notification and keep the service in the foreground
        mediaSession?.let { addSession(it) }

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setChannelName(R.string.playback_channel_name)
                .build()
                .apply { setSmallIcon(R.drawable.ic_stat_soundbuzz) }
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        headsetClicks?.release()
        headsetClicks = null
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "soundbuzz_playback_channel"

        fun start(context: Context) {
            try {
                context.startService(Intent(context, PlaybackService::class.java))
            } catch (e: IllegalStateException) {
                // Background start not allowed; the session is already running if playback was started from it
                DiagnosticsLogger.logPlayer("PlaybackService", "Could not start service: ${e.message}")
            }
        }
    }
}
