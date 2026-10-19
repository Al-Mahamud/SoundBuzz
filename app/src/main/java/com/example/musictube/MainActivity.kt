package com.example.musictube

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.example.musictube.domain.model.ThemeSettings
import com.example.musictube.navigation.MusicTubeApp
import com.example.musictube.presentation.theme.MusicTubeTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            // Saved in the preferences table, so every screen sees the same theme immediately
            val repository = MusicTubeApplication.instance.repository
            val themeFlow = remember { repository.getThemeSettingsFlow() }
            val themeSettings by themeFlow.collectAsState(initial = ThemeSettings())

            MusicTubeTheme(settings = themeSettings) {
                MusicTubeApp()
            }
        }
    }
}
