package com.example.musictube.presentation.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musictube.domain.model.Playlist
import com.example.musictube.domain.model.Track
import com.example.musictube.presentation.components.AddToPlaylistDialog
import com.example.musictube.presentation.components.ArtworkImage
import com.example.musictube.presentation.components.PillChip
import com.example.musictube.presentation.components.CreatePlaylistDialog
import com.example.musictube.presentation.components.EmptyStateView
import com.example.musictube.presentation.components.TrackItemRow
import com.example.musictube.presentation.components.TrackOptionsMenuBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel(),
    onNavigateToPlaylist: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToPlayer: (String) -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val history by viewModel.recentlyPlayed.collectAsState()

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var selectedTrackForMenu by remember { mutableStateOf<Track?>(null) }
    var selectedTrackForPlaylist by remember { mutableStateOf<Track?>(null) }

    val colors = MaterialTheme.colorScheme
    val tabs = listOf("Playlists", "Favorites", "History")

    val playAll: (List<Track>) -> Unit = { tracks ->
        if (tracks.isNotEmpty()) {
            viewModel.playAll(tracks)
            onNavigateToPlayer(tracks.first().youtubeVideoId)
        }
    }
    val shufflePlay: (List<Track>) -> Unit = { tracks ->
        if (tracks.isNotEmpty()) {
            viewModel.shufflePlay(tracks)
            onNavigateToPlayer(tracks.first().youtubeVideoId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(1.dp, colors.outlineVariant, CircleShape)
                    .clickable { showCreatePlaylistDialog = true }
                    .semantics { contentDescription = "Create playlist" },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = colors.onSurface)
            }
        }

        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                PillChip(
                    label = title,
                    selected = selectedTab == index,
                    onClick = { viewModel.setSelectedTab(index) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            LikedSongsCard(
                                count = favorites.size,
                                onOpen = { viewModel.setSelectedTab(1) },
                                onPlayAll = { playAll(favorites) },
                                onShuffle = { shufflePlay(favorites) }
                            )
                        }
                        item {
                            Text(
                                text = "Playlists",
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.onBackground,
                                modifier = Modifier
                                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 6.dp)
                                    .semantics { heading() }
                            )
                        }
                        if (playlists.isEmpty()) {
                            item {
                                Text(
                                    text = "No playlists yet. Tap + to create your first one.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                            }
                        } else {
                            items(playlists) { playlist ->
                                PlaylistItemRow(
                                    playlist = playlist,
                                    onClick = { onNavigateToPlaylist(playlist.id) },
                                    onDelete = { viewModel.deletePlaylist(playlist.id) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (favorites.isEmpty()) {
                        EmptyStateView(
                            message = "No favorite songs yet. Tap the heart on any song to add it here!"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            item {
                                PlayShuffleButtons(
                                    onPlayAll = { playAll(favorites) },
                                    onShuffle = { shufflePlay(favorites) },
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                            }
                            items(favorites) { track ->
                                TrackItemRow(
                                    track = track,
                                    onTrackClick = {
                                        viewModel.playTrack(track, favorites)
                                        onNavigateToPlayer(track.youtubeVideoId)
                                    },
                                    onMoreOptionsClick = {
                                        selectedTrackForMenu = track
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    if (history.isEmpty()) {
                        EmptyStateView(
                            message = "Your playback history is empty. Start exploring and playing music!"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${history.size} recently played",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { viewModel.clearHistory() }) {
                                        Text("Clear history")
                                    }
                                }
                            }
                            items(history) { track ->
                                TrackItemRow(
                                    track = track,
                                    onTrackClick = {
                                        viewModel.playTrack(track, history)
                                        onNavigateToPlayer(track.youtubeVideoId)
                                    },
                                    onMoreOptionsClick = {
                                        selectedTrackForMenu = track
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                showCreatePlaylistDialog = false
                viewModel.createPlaylist(name)
            }
        )
    }

    // Track Options Menu
    selectedTrackForMenu?.let { track ->
        TrackOptionsMenuBottomSheet(
            track = track,
            onDismiss = { selectedTrackForMenu = null },
            onPlay = {
                viewModel.playTrack(track, listOf(track))
                onNavigateToPlayer(track.youtubeVideoId)
            },
            onPlayNext = { },
            onAddToQueue = { },
            onAddToPlaylist = {
                selectedTrackForPlaylist = track
            },
            onToggleFavorite = { viewModel.toggleFavorite(track) },
            onViewArtist = { artistName -> onNavigateToArtist(artistName) }
        )
    }

    // Add to Playlist Dialog
    selectedTrackForPlaylist?.let { track ->
        AddToPlaylistDialog(
            track = track,
            playlists = playlists,
            onDismiss = { selectedTrackForPlaylist = null },
            onPlaylistSelected = { playlistId ->
                viewModel.addTrackToPlaylist(playlistId, track)
                selectedTrackForPlaylist = null
            },
            onCreateNewPlaylist = { name ->
                viewModel.createPlaylist(name)
                selectedTrackForPlaylist = null
            }
        )
    }
}

@Composable
private fun LikedSongsCard(
    count: Int,
    onOpen: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, shape)
            .clickable(onClick = onOpen)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = "Liked songs", style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
                Text(
                    text = if (count == 0) "Heart songs to collect them here" else "$count songs you've hearted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }
        if (count > 0) {
            PlayShuffleButtons(onPlayAll = onPlayAll, onShuffle = onShuffle)
        }
    }
}

@Composable
private fun PlayShuffleButtons(
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onPlayAll,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Play all")
        }
        OutlinedButton(
            onClick = onShuffle,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
        ) {
            Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Shuffle")
        }
    }
}

@Composable
fun PlaylistItemRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!playlist.thumbnail.isNullOrBlank()) {
            ArtworkImage(
                model = playlist.thumbnail,
                contentDescription = playlist.name,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(14.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Playlist · ${playlist.trackCount} tracks",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete playlist ${playlist.name}",
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
