package com.example.musictube.presentation.search

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musictube.domain.model.Track
import com.example.musictube.presentation.components.AddToPlaylistDialog
import com.example.musictube.presentation.components.EmptyStateView
import com.example.musictube.presentation.components.ErrorRetryView
import com.example.musictube.presentation.components.SkeletonRowItem
import com.example.musictube.presentation.components.TrackItemRow
import com.example.musictube.presentation.components.TrackOptionsMenuBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    onNavigateToArtist: (String) -> Unit,
    onNavigateToPlayer: (String) -> Unit
) {
    val query by viewModel.query.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var selectedTrackForMenu by remember { mutableStateOf<Track?>(null) }
    var selectedTrackForPlaylist by remember { mutableStateOf<Track?>(null) }

    val quickKeywords = listOf("Pop", "Rock", "Lo-Fi", "EDM", "Bangla", "Bollywood", "Hip-Hop", "Arijit Singh", "The Weeknd")

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input Field
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    placeholder = { Text("Search music, artists, albums...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearSearch() }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Quick Category Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(quickKeywords) { keyword ->
                    FilterChip(
                        selected = query.equals(keyword, ignoreCase = true),
                        onClick = { viewModel.onQueryChange(keyword) },
                        label = { Text(keyword) }
                    )
                }
            }

            // Body Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    isLoading -> {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            items(8) {
                                SkeletonRowItem()
                            }
                        }
                    }
                    error != null && searchResults.isEmpty() -> {
                        ErrorRetryView(
                            message = error ?: "Search error",
                            onRetry = { viewModel.retrySearch() }
                        )
                    }
                    query.isNotBlank() && searchResults.isEmpty() && !isLoading -> {
                        EmptyStateView(
                            message = "No music found. Try searching with another name."
                        )
                    }
                    query.isBlank() -> {
                        // Display Recent Searches
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            if (recentSearches.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recent Searches",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(onClick = { viewModel.clearAllRecentSearches() }) {
                                        Text("Clear All")
                                    }
                                }

                                LazyColumn(
                                    contentPadding = PaddingValues(bottom = 90.dp)
                                ) {
                                    items(recentSearches) { recent ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.onQueryChange(recent) }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = recent,
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { viewModel.deleteRecentSearch(recent) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                EmptyStateView(
                                    message = "Search for your favorite songs, artists, and playlists"
                                )
                            }
                        }
                    }
                    else -> {
                        // Display Search Results
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            items(searchResults) { track ->
                                TrackItemRow(
                                    track = track,
                                    onTrackClick = {
                                        viewModel.playTrack(track, searchResults)
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

    // Track Options Menu
    selectedTrackForMenu?.let { track ->
        TrackOptionsMenuBottomSheet(
            track = track,
            onDismiss = { selectedTrackForMenu = null },
            onPlay = {
                viewModel.playTrack(track, listOf(track))
                onNavigateToPlayer(track.youtubeVideoId)
            },
            onPlayNext = { viewModel.playNext(track) },
            onAddToQueue = { viewModel.addToQueue(track) },
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
                viewModel.createPlaylist(name, track)
                selectedTrackForPlaylist = null
            }
        )
    }
}
