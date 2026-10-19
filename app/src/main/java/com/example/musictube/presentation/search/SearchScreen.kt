package com.example.musictube.presentation.search

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.example.musictube.domain.model.Artist
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musictube.domain.model.Track
import com.example.musictube.presentation.components.AddToPlaylistDialog
import com.example.musictube.presentation.components.EmptyStateView
import com.example.musictube.presentation.components.ErrorRetryView
import com.example.musictube.presentation.components.SegmentOption
import com.example.musictube.presentation.components.SegmentedControl
import com.example.musictube.presentation.home.CategoryCardItem
import com.example.musictube.presentation.components.SkeletonRowItem
import com.example.musictube.presentation.components.TrackItemRow
import com.example.musictube.presentation.components.TrackOptionsMenuBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    onNavigateToArtist: (String) -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToPlayer: (String) -> Unit
) {
    val query by viewModel.query.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val artistResults by viewModel.artistResults.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var selectedTrackForMenu by remember { mutableStateOf<Track?>(null) }
    var selectedTrackForPlaylist by remember { mutableStateOf<Track?>(null) }

    val colors = MaterialTheme.colorScheme
    val filters = SearchFilter.entries

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineLarge,
            color = colors.onBackground,
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 20.dp)
                .semantics { heading() }
        )

        // Bright, inverted search field so it reads as the screen's main action
        TextField(
            value = query,
            onValueChange = { viewModel.onQueryChange(it) },
            placeholder = { Text("Songs, artists, moods…", style = MaterialTheme.typography.bodyLarge) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearSearch() }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colors.inverseSurface,
                unfocusedContainerColor = colors.inverseSurface,
                focusedTextColor = colors.inverseOnSurface,
                unfocusedTextColor = colors.inverseOnSurface,
                focusedLeadingIconColor = colors.inverseOnSurface,
                unfocusedLeadingIconColor = colors.inverseOnSurface,
                focusedTrailingIconColor = colors.inverseOnSurface,
                unfocusedTrailingIconColor = colors.inverseOnSurface,
                focusedPlaceholderColor = colors.inverseOnSurface.copy(alpha = 0.6f),
                unfocusedPlaceholderColor = colors.inverseOnSurface.copy(alpha = 0.6f),
                cursorColor = colors.primary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp)
                .semantics { contentDescription = "Search songs, artists or moods" }
        )

        SegmentedControl(
            options = filters.map { SegmentOption(it.label) },
            selectedIndex = filters.indexOf(selectedFilter),
            onSelect = { viewModel.setFilter(filters[it]) },
            fillWidth = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                isLoading -> {
                    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
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
                query.isNotBlank() && searchResults.isEmpty() && artistResults.isEmpty() && !isLoading -> {
                    EmptyStateView(
                        message = "No music found. Try searching with another name."
                    )
                }
                query.isBlank() -> {
                    // Recent searches + browse grid
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (recentSearches.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, end = 8.dp, top = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "RECENT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { viewModel.clearAllRecentSearches() }) {
                                        Text("Clear")
                                    }
                                }
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(recentSearches) { recent ->
                                        RecentSearchChip(
                                            text = recent,
                                            onClick = { viewModel.onQueryChange(recent) },
                                            onRemove = { viewModel.deleteRecentSearch(recent) }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Browse moods & genres",
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.onBackground,
                                modifier = Modifier
                                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 14.dp)
                                    .semantics { heading() }
                            )
                        }

                        items(viewModel.browseCategories.chunked(2)) { pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                pair.forEach { category ->
                                    CategoryCardItem(
                                        category = category,
                                        onClick = { onNavigateToCategory(category.name) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                else -> {
                    // Search results & artists
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (selectedFilter == SearchFilter.ARTISTS) {
                            items(artistResults) { artist ->
                                ArtistItemRow(
                                    artist = artist,
                                    onClick = { onNavigateToArtist(artist.name) }
                                )
                            }
                        } else {
                            if (artistResults.isNotEmpty() && selectedFilter == SearchFilter.ALL) {
                                item { ResultsHeader("Artists") }
                                items(artistResults) { artist ->
                                    ArtistItemRow(
                                        artist = artist,
                                        onClick = { onNavigateToArtist(artist.name) }
                                    )
                                }
                                if (searchResults.isNotEmpty()) {
                                    item { ResultsHeader("Songs & videos") }
                                }
                            }
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

@Composable
fun ArtistItemRow(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = artist.imageUrl,
            contentDescription = artist.name,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist.subscriberCount ?: "Artist",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ResultsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp)
            .semantics { heading() }
    )
}

@Composable
private fun RecentSearchChip(
    text: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, CircleShape)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = colors.onSurface, maxLines = 1)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove)
                .semantics { contentDescription = "Remove $text" },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
