package com.example.musictube.presentation.library

import androidx.compose.foundation.background
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.musictube.domain.model.Playlist
import com.example.musictube.domain.model.Track
import com.example.musictube.presentation.components.AddToPlaylistDialog
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

    val tabs = listOf("Playlists", "Favorites", "Recently Played")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Your Library",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showCreatePlaylistDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Create Playlist")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.setSelectedTab(index) },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
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
                        // Playlists Tab
                        if (playlists.isEmpty()) {
                            EmptyStateView(
                                message = "You don't have any playlists yet. Tap the '+' button below to create one!"
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
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
                        // Favorites Tab
                        if (favorites.isEmpty()) {
                            EmptyStateView(
                                message = "No favorite songs yet. Tap the heart icon on any song to add it here!"
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.playAll(favorites)
                                            onNavigateToPlayer(favorites.first().youtubeVideoId)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Play All")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.shufflePlay(favorites)
                                            onNavigateToPlayer(favorites.shuffled().first().youtubeVideoId)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.Shuffle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Shuffle")
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 90.dp)
                                ) {
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
                    }
                    2 -> {
                        // Recently Played Tab
                        if (history.isEmpty()) {
                            EmptyStateView(
                                message = "Your playback history is empty. Start exploring and playing music!"
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${history.size} recently played",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(onClick = { viewModel.clearHistory() }) {
                                        Text("Clear History")
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 90.dp)
                                ) {
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
fun PlaylistItemRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!playlist.thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = playlist.thumbnail,
                    contentDescription = playlist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${playlist.trackCount} tracks",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Playlist",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
