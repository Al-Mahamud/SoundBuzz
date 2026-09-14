package com.example.musictube.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musictube.MusicTubeApplication
import com.example.musictube.domain.model.Track
import com.example.musictube.playback.PlaybackManager
import com.example.musictube.utils.DiagnosticsLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsSheet(
    onDismiss: () -> Unit,
    playbackManager: PlaybackManager = MusicTubeApplication.instance.playbackManager
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val playerState by playbackManager.playerState.collectAsState()
    val apiLogs by DiagnosticsLogger.apiLogs.collectAsState()
    val playerLogs by DiagnosticsLogger.playerLogs.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Player Status", "API Responses (${apiLogs.size})", "Player Events (${playerLogs.size})")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Diagnostics & Live Response",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(onClick = {
                        val fullLog = buildString {
                            appendLine("=== SOUNDBUZZ DIAGNOSTICS LOG ===")
                            appendLine("Player State: ${playerState.playState}")
                            appendLine("Current Track: ${playerState.currentTrack?.title} [${playerState.currentTrack?.youtubeVideoId}]")
                            appendLine("Position: ${playerState.currentPositionSeconds}s / ${playerState.durationSeconds}s")
                            appendLine("Error Message: ${playerState.errorMessage}")
                            appendLine("\n--- RECENT API CALLS ---")
                            apiLogs.forEach {
                                appendLine("[${it.timestamp}] ${it.method} ${it.summary} -> ${it.url}")
                                appendLine("Response: ${it.rawResponse.take(500)}")
                                appendLine("---")
                            }
                            appendLine("\n--- PLAYER EVENTS ---")
                            playerLogs.forEach {
                                appendLine("[${it.timestamp}] ${it.event}: ${it.details}")
                            }
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("SoundBuzz Diagnostics", fullLog))
                        Toast.makeText(context, "Diagnostics copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = { DiagnosticsLogger.clear() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Test Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val testTrack = Track(
                            id = "dQw4w9WgXcQ",
                            youtubeVideoId = "dQw4w9WgXcQ",
                            title = "Never Gonna Give You Up (Test Video)",
                            artist = "Rick Astley",
                            channel = "Rick Astley",
                            thumbnailUrl = "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
                            duration = "3:32",
                            durationSeconds = 212,
                            category = "Test"
                        )
                        playbackManager.playTrack(testTrack)
                        Toast.makeText(context, "Playing test track...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Play Video", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTabIndex) {
                0 -> PlayerStatusView(playerState)
                1 -> ApiLogsView(apiLogs)
                2 -> PlayerLogsView(playerLogs)
            }
        }
    }
}

@Composable
private fun PlayerStatusView(playerState: com.example.musictube.domain.model.PlayerState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "CURRENT PLAYBACK STATUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                StatusRow("Play State", playerState.playState.name, if (playerState.isPlaying) Color(0xFF00C853) else MaterialTheme.colorScheme.onSurface)
                StatusRow("Current Track", playerState.currentTrack?.title ?: "None")
                StatusRow("Artist", playerState.currentTrack?.artist ?: "None")
                StatusRow("Video ID", playerState.currentTrack?.youtubeVideoId ?: "None")
                StatusRow("Position", "${playerState.currentPositionSeconds.toInt()}s / ${playerState.durationSeconds.toInt()}s")
                StatusRow("Video Visible", "${playerState.isVideoVisible}")
                if (!playerState.errorMessage.isNullOrBlank()) {
                    StatusRow("Error", playerState.errorMessage ?: "", MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tip: If videoId is valid but State stays BUFFERING, YouTube may be restricted on your network or this video forbids third-party embeds. Tap 'Test Play Video' above to verify your phone's YouTube player.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ApiLogsView(logs: List<com.example.musictube.utils.ApiLogEntry>) {
    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No API calls recorded yet. Try searching for a song in the Search tab.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (log.isSuccess) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${log.method} (${log.statusCode})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (log.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = log.timestamp,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = log.url,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Scrollable response box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = log.rawResponse.take(1500),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .horizontalScroll(rememberScrollState()),
                                maxLines = 8
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerLogsView(logs: List<com.example.musictube.utils.PlayerLogEntry>) {
    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No player events recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(logs) { log ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = log.event,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (log.details.isNotBlank()) {
                            Text(
                                text = log.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Text(
                        text = log.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
    }
}
