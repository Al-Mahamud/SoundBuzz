package com.example.musictube.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musictube.MusicTubeApplication
import com.example.musictube.data.repository.MusicRepository
import com.example.musictube.playback.PlaybackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

class SettingsViewModel(
    private val repository: MusicRepository = MusicTubeApplication.instance.repository,
    private val playbackManager: PlaybackManager = MusicTubeApplication.instance.playbackManager
) : ViewModel() {

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _autoplay = MutableStateFlow(true)
    val autoplay: StateFlow<Boolean> = _autoplay.asStateFlow()

    val isShuffle: StateFlow<Boolean> = MutableStateFlow(false).apply {
        viewModelScope.launch {
            playbackManager.playerState.collect {
                value = it.isShuffleEnabled
            }
        }
    }

    val currentApiKey: StateFlow<String> = repository.getYoutubeApiKeyFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _storageStatusMessage = MutableStateFlow<String?>(null)
    val storageStatusMessage: StateFlow<String?> = _storageStatusMessage.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun toggleAutoplay() {
        _autoplay.value = !_autoplay.value
    }

    fun toggleShuffle() {
        playbackManager.toggleShuffle()
    }

    fun toggleRepeat() {
        playbackManager.toggleRepeat()
    }

    fun updateApiKey(newKey: String) {
        viewModelScope.launch {
            repository.setYoutubeApiKey(newKey)
            _storageStatusMessage.value = "API Key saved successfully!"
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            _storageStatusMessage.value = "Cache cleared successfully."
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearPlaybackHistory()
            _storageStatusMessage.value = "Playback history cleared."
        }
    }

    fun clearStatusMessage() {
        _storageStatusMessage.value = null
    }
}
