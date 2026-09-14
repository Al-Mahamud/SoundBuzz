package com.example.musictube.presentation.artist

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.musictube.domain.model.Track
import com.example.musictube.presentation.components.AddToPlaylistDialog
import com.example.musictube.presentation.components.TrackItemRow
import com.example.musictube.presentation.components.TrackOptionsMenuBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    viewModel: ArtistDetailViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit
) {
    val artist by viewModel.artist.collectAsState()
    val relatedTracks by viewModel.relatedTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var selectedTrackForMenu by remember { mutableStateOf<Track?>(null) }
    var selectedTrackForPlaylist by remember { mutableStateOf<Track?>(null) }

    val popularTracks = artist?.popularTracks ?: emptyList()
    val allArtistTracks = (popularTracks + relatedTracks).distinctBy { it.youtubeVideoId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(artist?.name ?: viewModel.artistName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Artist Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = artist?.imageUrl,
                        contentDescription = artist?.name,
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = artist?.name ?: viewModel.artistName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (!artist?.subscriberCount.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${artist?.subscriberCount} subscribers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!artist?.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = artist?.description.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Play & Shuffle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (allArtistTracks.isNotEmpty()) {
                                    viewModel.playAll(allArtistTracks)
                                    onNavigateToPlayer(allArtistTracks.first().youtubeVideoId)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All")
                        }

                        OutlinedButton(
                            onClick = {
                                if (allArtistTracks.isNotEmpty()) {
                                    viewModel.shufflePlay(allArtistTracks)
                                    onNavigateToPlayer(allArtistTracks.shuffled().first().youtubeVideoId)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle")
                        }
                    }
                }
            }

            // Popular Songs
            item {
                Text(
                    text = "Popular Songs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(allArtistTracks) { track ->
                TrackItemRow(
                    track = track,
                    onTrackClick = {
                        viewModel.playTrack(track, allArtistTracks)
                        onNavigateToPlayer(track.youtubeVideoId)
                    },
                    onMoreOptionsClick = {
                        selectedTrackForMenu = track
                    }
                )
            }
        }
    }

    // Options Bottom Sheet
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
            onViewArtist = { }
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
                viewModel.createPlaylist(name, track)
                selectedTrackForPlaylist = null
            }
        )
    }
}
