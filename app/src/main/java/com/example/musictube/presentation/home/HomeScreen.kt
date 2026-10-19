package com.example.musictube.presentation.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musictube.R
import com.example.musictube.domain.model.Artist
import com.example.musictube.domain.model.Category
import com.example.musictube.domain.model.CategoryPlaylist
import com.example.musictube.domain.model.Track
import com.example.musictube.presentation.components.AddToPlaylistDialog
import com.example.musictube.presentation.components.ArtworkImage
import com.example.musictube.presentation.components.ErrorRetryView
import com.example.musictube.presentation.components.PillChip
import com.example.musictube.presentation.components.SkeletonCardItem
import com.example.musictube.presentation.components.TrackItemCard
import com.example.musictube.presentation.components.TrackItemRow
import com.example.musictube.presentation.components.TrackOptionsMenuBottomSheet
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToArtist: (String) -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToPlayer: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedTrackForMenu by remember { mutableStateOf<Track?>(null) }
    var selectedTrackForPlaylist by remember { mutableStateOf<Track?>(null) }

    val play: (Track, List<Track>) -> Unit = { track, queue ->
        viewModel.playTrack(track, queue)
        onNavigateToPlayer(track.youtubeVideoId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> {
                HomeLoadingSkeleton()
            }
            uiState.error != null && uiState.trending.isEmpty() -> {
                ErrorRetryView(
                    message = uiState.error ?: "Error loading home screen",
                    onRetry = { viewModel.loadHomeData() }
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
                ) {
                    item { HomeHeader() }

                    item {
                        MoodChipsRow(
                            categories = uiState.categories,
                            onCategoryClick = onNavigateToCategory
                        )
                    }

                    // Hero: continue the last song, or the top recommendation for new listeners
                    val continueTrack = uiState.recentlyPlayed.firstOrNull()
                    val heroTrack = continueTrack ?: uiState.recommendations.firstOrNull()
                    if (heroTrack != null) {
                        item {
                            HeroTrackCard(
                                track = heroTrack,
                                label = if (continueTrack != null) "CONTINUE LISTENING" else "TOP PICK FOR YOU",
                                onPlay = {
                                    play(heroTrack, if (continueTrack != null) uiState.recentlyPlayed else uiState.recommendations)
                                }
                            )
                        }
                    }

                    if (uiState.recommendations.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Made for you")
                            TrackCardsRow(uiState.recommendations) { play(it, uiState.recommendations) }
                        }
                    }

                    if (uiState.trending.isNotEmpty()) {
                        item { SectionHeader(title = "Trending now") }
                        itemsIndexed(uiState.trending.take(5), key = { _, t -> "trending_${t.youtubeVideoId}" }) { index, track ->
                            TrackItemRow(
                                track = track,
                                onTrackClick = { play(track, uiState.trending) },
                                onMoreOptionsClick = { selectedTrackForMenu = track },
                                leading = {
                                    Text(
                                        text = "%02d".format(index + 1),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.width(30.dp)
                                    )
                                }
                            )
                        }
                    }

                    if (uiState.recentlyPlayed.size > 1) {
                        item {
                            SectionHeader(title = "Recently played")
                            TrackCardsRow(uiState.recentlyPlayed) { play(it, uiState.recentlyPlayed) }
                        }
                    }

                    if (uiState.categoryPlaylists.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Featured playlists")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(uiState.categoryPlaylists) { playlist ->
                                    CategoryPlaylistCard(
                                        playlist = playlist,
                                        onClick = { onNavigateToCategory(playlist.category) },
                                        onPlayClick = {
                                            viewModel.playCategoryPlaylist(playlist)
                                            if (playlist.tracks.isNotEmpty()) {
                                                onNavigateToPlayer(playlist.tracks.first().youtubeVideoId)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.popularArtists.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Popular artists")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.popularArtists) { artist ->
                                    ArtistCircularItem(
                                        artist = artist,
                                        onClick = { onNavigateToArtist(artist.name) }
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.newReleases.isNotEmpty()) {
                        item {
                            SectionHeader(title = "New releases")
                            TrackCardsRow(uiState.newReleases) { play(it, uiState.newReleases) }
                        }
                    }

                    if (uiState.hitSongs.isNotEmpty()) {
                        item {
                            SectionHeader(title = "All-time hits")
                            TrackCardsRow(uiState.hitSongs) { play(it, uiState.hitSongs) }
                        }
                    }

                    if (uiState.regionalMusic.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Regional hits & vibes")
                            TrackCardsRow(uiState.regionalMusic) { play(it, uiState.regionalMusic) }
                        }
                    }
                }
            }
        }
    }

    // Options Menu Bottom Sheet
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

@Composable
private fun HomeHeader() {
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.height(44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_mark),
                contentDescription = null,
                modifier = Modifier.size(width = 44.dp, height = 26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = buildAnnotatedString {
                    append("Sound")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("Buzz") }
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = greeting,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "What's the vibe today?",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() }
        )
    }
}

@Composable
private fun MoodChipsRow(
    categories: List<Category>,
    onCategoryClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)
    ) {
        item {
            PillChip(label = "All", selected = true, onClick = {})
        }
        items(categories.take(12), key = { it.id }) { category ->
            PillChip(
                label = category.name,
                selected = false,
                onClick = { onCategoryClick(category.name) }
            )
        }
    }
}

@Composable
private fun HeroTrackCard(
    track: Track,
    label: String,
    onPlay: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, top = 20.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, shape)
            .clickable(onClick = onPlay)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ArtworkImage(
            model = track.thumbnailUrl,
            contentDescription = track.title,
            modifier = Modifier.size(104.dp),
            shape = RoundedCornerShape(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.primary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play ${track.title}",
                tint = colors.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun TrackCardsRow(tracks: List<Track>, onTrackClick: (Track) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(tracks) { track ->
            TrackItemCard(track = track, onTrackClick = { onTrackClick(track) })
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 28.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun ArtistCircularItem(
    artist: Artist,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), CircleShape)
                .padding(5.dp)
        ) {
            ArtworkImage(
                model = artist.imageUrl,
                contentDescription = artist.name,
                modifier = Modifier.matchParentSize(),
                shape = CircleShape
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Genre tile: deepened category color with a bright circle, white label. */
@Composable
fun CategoryCardItem(
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val base = Color(category.primaryColor)
    Box(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(lerp(base, Color.Black, 0.45f))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .align(Alignment.TopEnd)
                .offset(x = 18.dp, y = (-22).dp)
                .clip(CircleShape)
                .background(base)
        )
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}

@Composable
fun HomeLoadingSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(4) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(22.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.height(14.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(4) {
                        SkeletonCardItem()
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryPlaylistCard(
    playlist: CategoryPlaylist,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(RoundedCornerShape(20.dp))
        ) {
            ArtworkImage(
                model = playlist.thumbnailUrl,
                contentDescription = playlist.title,
                modifier = Modifier.matchParentSize(),
                shape = RoundedCornerShape(20.dp)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
            )
            Text(
                text = playlist.category.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onPrimary,
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.primary)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
                    .clickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play ${playlist.title}",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = playlist.title,
            style = MaterialTheme.typography.titleSmall,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${playlist.trackCount} tracks · ${playlist.description}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
