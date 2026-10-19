package com.example.musictube.presentation.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.musictube.domain.model.PlayState
import com.example.musictube.domain.model.RepeatMode
import com.example.musictube.presentation.components.AddToPlaylistDialog
import com.example.musictube.presentation.components.SegmentOption
import com.example.musictube.presentation.components.SegmentedControl
import com.example.musictube.presentation.components.WaveformSeekBar
import com.example.musictube.presentation.components.YouTubePlayerViewContainer
import com.example.musictube.utils.DurationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToArtist: (String) -> Unit
) {
    val playerState by viewModel.playerState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()

    val track = playerState.currentTrack

    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showQueueBottomSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showDiagnosticsSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }

    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f
    // Tint the whole screen with the artwork's main color
    val artworkColor = rememberArtworkColor(track?.thumbnailUrl)
    val backgroundColor by animateColorAsState(
        targetValue = artworkColor?.let { lerp(colors.background, it, if (isDark) 0.30f else 0.14f) } ?: colors.background,
        animationSpec = tween(durationMillis = 600),
        label = "playerBackground"
    )
    val raisedColor = lerp(backgroundColor, colors.onBackground, if (isDark) 0.08f else 0.05f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (track == null) {
            Text(
                text = "No track currently playing",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimize player",
                    containerColor = raisedColor,
                    onClick = onNavigateBack
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PLAYING FROM",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = track.category.ifBlank { "Music" },
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box {
                    RoundIconButton(
                        icon = Icons.Default.MoreHoriz,
                        contentDescription = "More options",
                        containerColor = raisedColor,
                        onClick = { showMoreMenu = true }
                    )
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add to playlist") },
                            leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                            onClick = { showMoreMenu = false; showAddToPlaylistDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Synced lyrics") },
                            leadingIcon = { Icon(Icons.Default.Subtitles, contentDescription = null) },
                            onClick = { showMoreMenu = false; showLyricsSheet = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Go to artist") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            onClick = { showMoreMenu = false; onNavigateToArtist(track.artist) }
                        )
                        DropdownMenuItem(
                            text = { Text("Diagnostics") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = { showMoreMenu = false; showDiagnosticsSheet = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Stop and close") },
                            leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                viewModel.stopAndDismiss()
                                onNavigateBack()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Artwork, or the YouTube player in video mode. Shrinks on short screens.
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .aspectRatio(if (playerState.isVideoVisible) 16f / 9f else 1f)
                    .shadow(28.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                // Audio mode streams audio only (no video data), so the YouTube view only exists in video mode
                if (playerState.isVideoVisible) {
                    YouTubePlayerViewContainer(
                        videoId = track.youtubeVideoId,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = track.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val audioLabel = playerState.audioFormat
                ?.takeIf { it.contains("kbps") }
                ?.let { "Audio · ${it.substringAfterLast("· ")}" }
                ?: "Audio"
            SegmentedControl(
                options = listOf(
                    SegmentOption(audioLabel, Icons.Default.Headphones),
                    SegmentOption("Video", Icons.Default.SmartDisplay)
                ),
                selectedIndex = if (playerState.isVideoVisible) 1 else 0,
                onSelect = { index ->
                    if ((index == 1) != playerState.isVideoVisible) viewModel.toggleVideoVisibility()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Title, artist and favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onNavigateToArtist(track.artist) }
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                RoundIconButton(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    containerColor = raisedColor,
                    tint = if (isFavorite) colors.primary else colors.onSurface,
                    size = 48.dp,
                    onClick = { viewModel.toggleFavorite() }
                )
            }

            if (!playerState.errorMessage.isNullOrBlank()) {
                Text(
                    text = playerState.errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onErrorContainer,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.errorContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Waveform seek bar
            val duration = playerState.durationSeconds.coerceAtLeast(1f)
            val shownPosition = scrubFraction?.let { it * duration } ?: playerState.currentPositionSeconds
            WaveformSeekBar(
                progress = shownPosition / duration,
                seed = track.youtubeVideoId,
                onSeek = { fraction -> viewModel.seekTo(fraction * duration) },
                onScrub = { scrubFraction = it }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = DurationUtils.formatSecondsToTime(shownPosition),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
                Text(
                    text = DurationUtils.formatSecondsToTime(duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = if (playerState.isShuffleEnabled) "Shuffle on" else "Shuffle off",
                        tint = if (playerState.isShuffleEnabled) colors.primary else colors.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.previous() }, modifier = Modifier.size(56.dp)) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = colors.onSurface,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(18.dp, CircleShape)
                        .clip(CircleShape)
                        .background(colors.primary)
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    if (playerState.playState == PlayState.BUFFERING) {
                        CircularProgressIndicator(
                            color = colors.onPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                IconButton(onClick = { viewModel.next() }, modifier = Modifier.size(56.dp)) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = colors.onSurface,
                        modifier = Modifier.size(38.dp)
                    )
                }
                IconButton(onClick = { viewModel.toggleRepeat() }) {
                    Icon(
                        imageVector = if (playerState.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = when (playerState.repeatMode) {
                            RepeatMode.OFF -> "Repeat off"
                            RepeatMode.ALL -> "Repeat all"
                            RepeatMode.ONE -> "Repeat one"
                        },
                        tint = if (playerState.repeatMode != RepeatMode.OFF) colors.primary else colors.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lyrics peek + queue entry
            val activeLyricIndex = lyrics.indexOfFirst { it.isActiveAt(playerState.currentPositionSeconds) }
            val currentLine = lyrics.getOrNull(activeLyricIndex)?.text
            val nextLine = lyrics.getOrNull(activeLyricIndex + 1)?.text
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(raisedColor)
                    .clickable { showLyricsSheet = true }
                    .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LYRICS",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showQueueBottomSheet = true }) {
                        Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Up next · ${(queue.size - currentQueueIndex - 1).coerceAtLeast(0)}")
                    }
                }
                Text(
                    text = when {
                        lyrics.isEmpty() -> "No synced lyrics for this track"
                        currentLine != null -> currentLine
                        else -> lyrics.first().text
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = if (lyrics.isEmpty()) colors.onSurfaceVariant else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 12.dp)
                )
                if (nextLine != null) {
                    Text(
                        text = nextLine,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp, end = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Queue Bottom Sheet
    if (showQueueBottomSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showQueueBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Queue",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        TextButton(onClick = { viewModel.shuffleQueue() }) {
                            Text("Shuffle")
                        }
                        TextButton(onClick = { viewModel.clearQueue() }) {
                            Text("Clear")
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    itemsIndexed(queue) { index, qTrack ->
                        val isCurrent = index == currentQueueIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                .clickable {
                                    viewModel.playFromQueue(index)
                                }
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = qTrack.thumbnailUrl,
                                contentDescription = qTrack.title,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = qTrack.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = qTrack.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = { viewModel.removeFromQueue(index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add to Playlist Dialog
    if (showAddToPlaylistDialog && track != null) {
        AddToPlaylistDialog(
            track = track,
            playlists = playlists,
            onDismiss = { showAddToPlaylistDialog = false },
            onPlaylistSelected = { playlistId ->
                viewModel.addTrackToPlaylist(playlistId)
                showAddToPlaylistDialog = false
            },
            onCreateNewPlaylist = { name ->
                viewModel.createPlaylist(name)
                showAddToPlaylistDialog = false
            }
        )
    }

    // Diagnostics & API Response Bottom Sheet
    if (showDiagnosticsSheet) {
        com.example.musictube.presentation.components.DiagnosticsSheet(
            onDismiss = { showDiagnosticsSheet = false }
        )
    }

    // Live Synced Lyrics Bottom Sheet
    if (showLyricsSheet && track != null) {
        val sheetState = rememberModalBottomSheetState()
        val listState = rememberLazyListState()

        val activeIndex = lyrics.indexOfFirst { it.isActiveAt(playerState.currentPositionSeconds) }
        LaunchedEffect(activeIndex) {
            if (activeIndex >= 0) {
                listState.animateScrollToItem(activeIndex)
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showLyricsSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Synced Lyrics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { showLyricsSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (lyrics.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No live synced lyrics found for this track.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    ) {
                        itemsIndexed(lyrics) { _, line ->
                            val isActive = line.isActiveAt(playerState.currentPositionSeconds)
                            Text(
                                text = line.text,
                                style = if (isActive) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.seekTo(line.startMs / 1000f)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 44.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

/** Main color of the track artwork (vibrant first), extracted off the main thread. */
@Composable
private fun rememberArtworkColor(url: String?): Color? {
    val context = LocalContext.current
    var color by remember(url) { mutableStateOf<Color?>(null) }
    LaunchedEffect(url) {
        if (url.isNullOrBlank()) return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(128)
            .allowHardware(false)
            .build()
        val result = context.imageLoader.execute(request) as? SuccessResult ?: return@LaunchedEffect
        val palette = withContext(Dispatchers.Default) {
            Palette.from(result.drawable.toBitmap()).generate()
        }
        val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch
        color = swatch?.let { Color(it.rgb) }
    }
    return color
}
