package com.example.musictube.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.musictube.MusicTubeApplication
import com.example.musictube.presentation.artist.ArtistDetailScreen
import com.example.musictube.presentation.category.CategoryDetailScreen
import com.example.musictube.presentation.components.MiniPlayer
import com.example.musictube.presentation.home.HomeScreen
import com.example.musictube.presentation.library.LibraryScreen
import com.example.musictube.presentation.player.PlayerScreen
import com.example.musictube.presentation.playlist.PlaylistDetailScreen
import com.example.musictube.presentation.search.SearchScreen
import com.example.musictube.presentation.settings.SettingsScreen
import java.net.URLEncoder

sealed class Screen(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Search : Screen("search", "Search", Icons.Filled.Search, Icons.Outlined.Search)
    data object Library : Screen("library", "Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun MusicTubeApp() {
    val navController = rememberNavController()
    val playbackManager = MusicTubeApplication.instance.playbackManager
    val playerState by playbackManager.playerState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }

    // Intercept back button when player is full screen
    BackHandler(enabled = isPlayerExpanded) {
        isPlayerExpanded = false
    }

    val bottomTabs = listOf(
        Screen.Home,
        Screen.Search,
        Screen.Library,
        Screen.Settings
    )

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val playerOffsetY by animateDpAsState(
        targetValue = if (isPlayerExpanded) 0.dp else (screenHeight + 150.dp),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "playerSheetOffset"
    )

    Scaffold(
        bottomBar = {
            if (!isPlayerExpanded) {
                Column {
                    // Persistent Mini Player above bottom navigation
                    AnimatedVisibility(
                        visible = playerState.currentTrack != null,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        MiniPlayer(
                            playerState = playerState,
                            onExpandClick = { isPlayerExpanded = true },
                            onPlayPauseClick = { playbackManager.togglePlayPause() },
                            onNextClick = { playbackManager.next() },
                            onCloseClick = { playbackManager.stopAndDismiss() }
                        )
                    }

                    SoundBuzzBottomBar(
                        tabs = bottomTabs,
                        currentRoute = currentDestination,
                        onTabClick = { screen ->
                            if (currentDestination != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Screens below must not re-apply the status/navigation bar insets handled here
                .consumeWindowInsets(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(160)) }
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToArtist = { artistName ->
                            try {
                                val encoded = com.example.musictube.utils.NavUtils.encodeArg(artistName)
                                navController.navigate("artist/$encoded")
                            } catch (e: Exception) {
                                com.example.musictube.utils.DiagnosticsLogger.e("Navigation", "Error navigating to artist: $artistName", e)
                            }
                        },
                        onNavigateToCategory = { categoryName ->
                            try {
                                val encoded = com.example.musictube.utils.NavUtils.encodeArg(categoryName)
                                navController.navigate("category/$encoded")
                            } catch (e: Exception) {
                                com.example.musictube.utils.DiagnosticsLogger.e("Navigation", "Error navigating to category: $categoryName", e)
                            }
                        },
                        onNavigateToPlayer = {
                            isPlayerExpanded = true
                        }
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        onNavigateToCategory = { categoryName ->
                            try {
                                val encoded = com.example.musictube.utils.NavUtils.encodeArg(categoryName)
                                navController.navigate("category/$encoded")
                            } catch (e: Exception) {
                                com.example.musictube.utils.DiagnosticsLogger.e("Navigation", "Error navigating to category: $categoryName", e)
                            }
                        },
                        onNavigateToArtist = { artistName ->
                            try {
                                val encoded = com.example.musictube.utils.NavUtils.encodeArg(artistName)
                                navController.navigate("artist/$encoded")
                            } catch (e: Exception) {
                                com.example.musictube.utils.DiagnosticsLogger.e("Navigation", "Error navigating to artist: $artistName", e)
                            }
                        },
                        onNavigateToPlayer = {
                            isPlayerExpanded = true
                        }
                    )
                }

                composable(Screen.Library.route) {
                    LibraryScreen(
                        onNavigateToPlaylist = { playlistId ->
                            try {
                                navController.navigate("playlist/$playlistId")
                            } catch (e: Exception) {
                                com.example.musictube.utils.DiagnosticsLogger.e("Navigation", "Error navigating to playlist: $playlistId", e)
                            }
                        },
                        onNavigateToArtist = { artistName ->
                            try {
                                val encoded = com.example.musictube.utils.NavUtils.encodeArg(artistName)
                                navController.navigate("artist/$encoded")
                            } catch (e: Exception) {
                                com.example.musictube.utils.DiagnosticsLogger.e("Navigation", "Error navigating to artist: $artistName", e)
                            }
                        },
                        onNavigateToPlayer = {
                            isPlayerExpanded = true
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen()
                }

                composable(
                    route = "playlist/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                ) {
                    PlaylistDetailScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPlayer = {
                            isPlayerExpanded = true
                        }
                    )
                }

                composable(
                    route = "artist/{artistName}",
                    arguments = listOf(navArgument("artistName") { type = NavType.StringType })
                ) {
                    ArtistDetailScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPlayer = {
                            isPlayerExpanded = true
                        }
                    )
                }

                composable(
                    route = "category/{categoryName}",
                    arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
                ) {
                    CategoryDetailScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPlayer = {
                            isPlayerExpanded = true
                        },
                        onNavigateToArtist = { artistName ->
                            try {
                                val encoded = com.example.musictube.utils.NavUtils.encodeArg(artistName)
                                navController.navigate("artist/$encoded")
                            } catch (e: Exception) {
                                com.example.musictube.utils.DiagnosticsLogger.e("Navigation", "Error navigating to artist: $artistName", e)
                            }
                        }
                    )
                }
            }
        }
    }

    // Persistent Expandable Full Screen Player
    // Lives at the app root level so YouTubePlayerView is never released during screen navigation!
    if (playerState.currentTrack != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = playerOffsetY)
                .alpha(if (isPlayerExpanded) 1f else 0.001f)
                .zIndex(if (isPlayerExpanded) 10f else -1f)
        ) {
            PlayerScreen(
                onNavigateBack = { isPlayerExpanded = false },
                onNavigateToArtist = { artistName ->
                    try {
                        isPlayerExpanded = false
                        val encoded = com.example.musictube.utils.NavUtils.encodeArg(artistName)
                        navController.navigate("artist/$encoded")
                    } catch (e: Exception) {
                        com.example.musictube.utils.DiagnosticsLogger.e("Navigation", "Error navigating to artist: $artistName", e)
                    }
                }
            )
        }
    }
}

@Composable
private fun SoundBuzzBottomBar(
    tabs: List<Screen>,
    currentRoute: String?,
    onTabClick: (Screen) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.background(colors.background)) {
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(68.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { screen ->
                val isSelected = currentRoute == screen.route
                val tint by animateColorAsState(
                    if (isSelected) colors.primary else colors.onSurfaceVariant,
                    label = "navTint"
                )
                val indicator by animateColorAsState(
                    if (isSelected) colors.primaryContainer else Color.Transparent,
                    label = "navIndicator"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab
                        ) { onTabClick(screen) }
                        .semantics { selected = isSelected },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 56.dp, height = 30.dp)
                            .clip(CircleShape)
                            .background(indicator),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = screen.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = tint
                    )
                }
            }
        }
    }
}
