package com.example.musictube.presentation.category

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musictube.domain.model.Track
import com.example.musictube.presentation.components.AddToPlaylistDialog
import com.example.musictube.presentation.components.TrackItemCard
import com.example.musictube.presentation.components.TrackItemRow
import com.example.musictube.presentation.components.TrackOptionsMenuBottomSheet
import com.example.musictube.presentation.home.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    viewModel: CategoryDetailViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToArtist: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedTrackForMenu by remember { mutableStateOf<Track?>(null) }
    var selectedTrackForPlaylist by remember { mutableStateOf<Track?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.categoryName) },
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
            // Category Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${uiState.categoryName} Hub",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            // Popular Section
            if (uiState.popular.isNotEmpty()) {
                item {
                    SectionHeader(title = "Popular in ${uiState.categoryName}")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.popular) { track ->
                            TrackItemCard(
                                track = track,
                                onTrackClick = {
                                    viewModel.playTrack(track, uiState.popular)
                                    onNavigateToPlayer(track.youtubeVideoId)
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Trending Section
            if (uiState.trending.isNotEmpty()) {
                item {
                    SectionHeader(title = "Trending Now")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.trending) { track ->
                            TrackItemCard(
                                track = track,
                                onTrackClick = {
                                    viewModel.playTrack(track, uiState.trending)
                                    onNavigateToPlayer(track.youtubeVideoId)
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Latest Section
            if (uiState.latest.isNotEmpty()) {
                item {
                    SectionHeader(title = "Latest Songs")
                }
                items(uiState.latest) { track ->
                    TrackItemRow(
                        track = track,
                        onTrackClick = {
                            viewModel.playTrack(track, uiState.latest)
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
            onViewArtist = { artistName -> onNavigateToArtist(artistName) }
        )
    }

    // Add to Playlist Dialog
    selectedTrackForPlaylist?.let { track ->
        AddToPlaylistDialog(
            track = track,
            playlists = uiState.playlists,
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
