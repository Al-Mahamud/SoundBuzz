package com.example.musictube.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.example.musictube.domain.model.AccentOption
import com.example.musictube.domain.model.ThemeMode
import com.example.musictube.domain.model.ThemeSettings
import com.example.musictube.presentation.components.SegmentOption
import com.example.musictube.presentation.components.SegmentedControl
import com.example.musictube.presentation.theme.OnAccentDark
import com.example.musictube.presentation.theme.isAppInDarkTheme
import com.example.musictube.presentation.theme.shades
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.musictube.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val themeSettings by viewModel.themeSettings.collectAsState()
    val autoplay by viewModel.autoplay.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val currentApiKey by viewModel.currentApiKey.collectAsState()
    val statusMessage by viewModel.storageStatusMessage.collectAsState()

    var apiKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showDiagnosticsSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .semantics { heading() }
            )

            // 1. Appearance Section
            SettingsSectionHeader(title = "Appearance", icon = Icons.Default.Palette)
            AppearanceCard(
                settings = themeSettings,
                onModeChange = viewModel::setThemeMode,
                onAccentChange = viewModel::setAccent,
                onPureBlackChange = viewModel::setPureBlack
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Playback Section
            SettingsSectionHeader(title = "Playback", icon = Icons.Default.PlayCircleOutline)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Autoplay", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Keep playing similar music automatically", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = autoplay,
                            onCheckedChange = { viewModel.toggleAutoplay() }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Shuffle Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Randomize upcoming track order", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isShuffle,
                            onCheckedChange = { viewModel.toggleShuffle() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. YouTube API Key Configuration
            SettingsSectionHeader(title = "YouTube Data API v3 Key", icon = Icons.Default.Key)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Enter your personal YouTube Data API v3 key to enable full live YouTube search and charts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showDiagnosticsSheet = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View API Logs")
                        }
                        Button(
                            onClick = { viewModel.updateApiKey(apiKeyInput) }
                        ) {
                            Text("Save Key")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. App Diagnostics & Logs Section
            SettingsSectionHeader(title = "App Diagnostics & Logs", icon = Icons.Default.BugReport)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Inspect real-time general logs, uncaught exceptions, system diagnostics, and player status.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val generalLogs by com.example.musictube.utils.DiagnosticsLogger.generalLogs.collectAsState()
                        val crashCount = generalLogs.count { it.level == com.example.musictube.utils.LogLevel.CRASH || it.level == com.example.musictube.utils.LogLevel.ERROR }

                        Column {
                            Text(
                                text = "${generalLogs.size} logs captured",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (crashCount > 0) {
                                Text(
                                    text = "$crashCount error/crash recorded",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Button(
                            onClick = { showDiagnosticsSheet = true },
                            colors = if (crashCount > 0) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View App Logs")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Storage Section
            SettingsSectionHeader(title = "Storage", icon = Icons.Default.CleaningServices)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClearCacheDialog = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Clear Cache", style = MaterialTheme.typography.bodyLarge)
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClearHistoryDialog = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Clear History", style = MaterialTheme.typography.bodyLarge)
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. About Section
            SettingsSectionHeader(title = "About", icon = Icons.Default.Info)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_mark),
                                contentDescription = "SoundBuzz logo",
                                modifier = Modifier.size(width = 44.dp, height = 26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("SoundBuzz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Fast & Modern YouTube Streaming", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("App Version", style = MaterialTheme.typography.bodyLarge)
                        Text("1.0.0 (Release)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLicensesDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Open-Source Licenses", style = MaterialTheme.typography.bodyLarge)
                        Text("View", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Audio mode streams audio-only via NewPipeExtractor to save data; video mode uses the embedded YouTube player.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Clear Cache Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Cache") },
            text = { Text("Are you sure you want to clear cached track metadata?") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCacheDialog = false
                        viewModel.clearCache()
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear History Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Playback History") },
            text = { Text("Are you sure you want to clear your playback history?") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearHistoryDialog = false
                        viewModel.clearHistory()
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Open Source Licenses Dialog
    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text("Open-Source Libraries") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("• Jetpack Compose & Material 3 (Google Apache 2.0)", style = MaterialTheme.typography.bodySmall)
                    Text("• AndroidX Room Database (Google Apache 2.0)", style = MaterialTheme.typography.bodySmall)
                    Text("• Android-YouTube-Player (Pierfrancesco Soffritti MIT)", style = MaterialTheme.typography.bodySmall)
                    Text("• Coil Compose (Coil Contributors Apache 2.0)", style = MaterialTheme.typography.bodySmall)
                    Text("• Retrofit & OkHttp (Square Apache 2.0)", style = MaterialTheme.typography.bodySmall)
                    Text("• AndroidX Media3 ExoPlayer (Google Apache 2.0)", style = MaterialTheme.typography.bodySmall)
                    Text("• NewPipe Extractor (TeamNewPipe GPL-3.0)", style = MaterialTheme.typography.bodySmall)
                    Text("• Manrope & Space Grotesk fonts (SIL Open Font License)", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showDiagnosticsSheet) {
        com.example.musictube.presentation.components.DiagnosticsSheet(
            onDismiss = { showDiagnosticsSheet = false }
        )
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppearanceCard(
    settings: ThemeSettings,
    onModeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentOption) -> Unit,
    onPureBlackChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDark = isAppInDarkTheme(settings)
    val modes = listOf(ThemeMode.SYSTEM, ThemeMode.DARK, ThemeMode.LIGHT)

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Theme", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Spacer(modifier = Modifier.height(10.dp))
            SegmentedControl(
                options = listOf(
                    SegmentOption("System", Icons.Default.BrightnessAuto),
                    SegmentOption("Dark", Icons.Default.DarkMode),
                    SegmentOption("Light", Icons.Default.LightMode)
                ),
                selectedIndex = modes.indexOf(settings.mode),
                onSelect = { onModeChange(modes[it]) },
                fillWidth = true,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = colors.outlineVariant)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Accent color",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(settings.accent.label, style = MaterialTheme.typography.bodyMedium, color = colors.primary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AccentOption.entries.forEach { option ->
                    val swatch = if (isDark) option.shades.onDark else option.shades.onLight
                    val selected = option == settings.accent
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .border(2.dp, if (selected) colors.onSurface else Color.Transparent, CircleShape)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .clickable(role = Role.RadioButton) { onAccentChange(option) }
                            .semantics {
                                contentDescription = "${option.label} accent"
                                this.selected = selected
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (isDark) OnAccentDark else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = colors.outlineVariant)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Pure black",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDark) colors.onSurface else colors.onSurfaceVariant
                    )
                    Text(
                        text = if (isDark) "Pitch-black backgrounds, saves battery on OLED screens" else "Available in dark theme",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.pureBlack && isDark,
                    onCheckedChange = onPureBlackChange,
                    enabled = isDark
                )
            }
        }
    }
}
