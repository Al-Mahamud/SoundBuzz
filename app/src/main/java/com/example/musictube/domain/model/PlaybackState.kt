package com.example.musictube.domain.model

enum class PlayState {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class PlayerState(
    val currentTrack: Track? = null,
    val playState: PlayState = PlayState.IDLE,
    val currentPositionSeconds: Float = 0f,
    val durationSeconds: Float = 0f,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isVideoVisible: Boolean = true,
    val errorMessage: String? = null
) {
    val isPlaying: Boolean
        get() = playState == PlayState.PLAYING

    val progress: Float
        get() = if (durationSeconds > 0f) (currentPositionSeconds / durationSeconds).coerceIn(0f, 1f) else 0f
}
