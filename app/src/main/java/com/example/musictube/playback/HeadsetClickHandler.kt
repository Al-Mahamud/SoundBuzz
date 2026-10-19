package com.example.musictube.playback

import android.os.Handler
import android.view.KeyEvent

/**
 * Multi-press support for single-button headsets (wired earphones send the same key every press):
 * 1 press = play/pause, 2 = next, 3 = previous. Earbuds that send dedicated next/previous keys
 * are not affected.
 */
class HeadsetClickHandler(
    private val handler: Handler,
    private val onClicks: (count: Int) -> Unit,
    private val timeoutMs: Long = MULTI_PRESS_TIMEOUT_MS
) {
    private var clicks = 0

    private val dispatch = Runnable {
        val count = clicks
        clicks = 0
        if (count > 0) onClicks(count)
    }

    /** Returns true when the key was consumed. */
    fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HEADSETHOOK && event.keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            return false
        }
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            clicks++
            handler.removeCallbacks(dispatch)
            if (clicks >= 3) dispatch.run() else handler.postDelayed(dispatch, timeoutMs)
        }
        return true
    }

    fun release() {
        handler.removeCallbacks(dispatch)
        clicks = 0
    }

    companion object {
        const val MULTI_PRESS_TIMEOUT_MS = 400L
    }
}
