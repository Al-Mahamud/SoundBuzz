package com.example.musictube.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musictube.MusicTubeApplication
import com.example.musictube.data.repository.MusicRepository
import com.example.musictube.domain.model.AccentOption
import com.example.musictube.domain.model.ThemeMode
import com.example.musictube.domain.model.ThemeSettings
import com.example.musictube.playback.PlaybackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: MusicRepository = MusicTubeApplication.instance.repository,
    private val playbackManager: PlaybackManager = MusicTubeApplication.instance.playbackManager
) : ViewModel() {

    val themeSettings: StateFlow<ThemeSettings> = repository.getThemeSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeSettings())

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
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setAccent(accent: AccentOption) {
        viewModelScope.launch { repository.setThemeAccent(accent) }
    }

    fun setPureBlack(enabled: Boolean) {
        viewModelScope.launch { repository.setPureBlack(enabled) }
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
