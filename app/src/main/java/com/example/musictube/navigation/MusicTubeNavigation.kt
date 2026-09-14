package com.example.musictube.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.example.musictube.presentation.components.YouTubePlayerViewContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

    val isFullScreenPlayer = currentDestination?.startsWith("player/") == true

    val bottomTabs = listOf(
        Screen.Home,
        Screen.Search,
        Screen.Library,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            if (!isFullScreenPlayer) {
                Column {
                    // Persistent Mini Player above bottom navigation
                    AnimatedVisibility(
                        visible = playerState.currentTrack != null,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        MiniPlayer(
                            playerState = playerState,
                            onExpandClick = {
                                playerState.currentTrack?.let { track ->
                                    navController.navigate("player/${track.youtubeVideoId}")
                                }
                            },
                            onPlayPauseClick = { playbackManager.togglePlayPause() },
                            onNextClick = { playbackManager.next() }
                        )
                    }

                    // 4-Tab Bottom Navigation Bar
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        bottomTabs.forEach { screen ->
                            val isSelected = currentDestination == screen.route
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.label
                                    )
                                },
                                label = { Text(screen.label) },
                                selected = isSelected,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                onClick = {
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
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Persistent background YouTube Player to keep playback active across the app
            if (!isFullScreenPlayer || !playerState.isVideoVisible) {
                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .alpha(0.001f)
                ) {
                    YouTubePlayerViewContainer()
                }
            }

            NavHost(
                navController = navController,
                startDestination = Screen.Home.route
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToArtist = { artistName ->
                            val encoded = URLEncoder.encode(artistName, "UTF-8")
                            navController.navigate("artist/$encoded")
                        },
                        onNavigateToCategory = { categoryName ->
                            val encoded = URLEncoder.encode(categoryName, "UTF-8")
                            navController.navigate("category/$encoded")
                        },
                        onNavigateToPlayer = { videoId ->
                            navController.navigate("player/$videoId")
                        }
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        onNavigateToArtist = { artistName ->
                            val encoded = URLEncoder.encode(artistName, "UTF-8")
                            navController.navigate("artist/$encoded")
                        },
                        onNavigateToPlayer = { videoId ->
                            navController.navigate("player/$videoId")
                        }
                    )
                }

                composable(Screen.Library.route) {
                    LibraryScreen(
                        onNavigateToPlaylist = { playlistId ->
                            navController.navigate("playlist/$playlistId")
                        },
                        onNavigateToArtist = { artistName ->
                            val encoded = URLEncoder.encode(artistName, "UTF-8")
                            navController.navigate("artist/$encoded")
                        },
                        onNavigateToPlayer = { videoId ->
                            navController.navigate("player/$videoId")
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen()
                }

                composable(
                    route = "player/{videoId}",
                    arguments = listOf(navArgument("videoId") { type = NavType.StringType })
                ) {
                    PlayerScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToArtist = { artistName ->
                            val encoded = URLEncoder.encode(artistName, "UTF-8")
                            navController.navigate("artist/$encoded")
                        }
                    )
                }

                composable(
                    route = "playlist/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                ) {
                    PlaylistDetailScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPlayer = { videoId ->
                            navController.navigate("player/$videoId")
                        }
                    )
                }

                composable(
                    route = "artist/{artistName}",
                    arguments = listOf(navArgument("artistName") { type = NavType.StringType })
                ) {
                    ArtistDetailScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPlayer = { videoId ->
                            navController.navigate("player/$videoId")
                        }
                    )
                }

                composable(
                    route = "category/{categoryName}",
                    arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
                ) {
                    CategoryDetailScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPlayer = { videoId ->
                            navController.navigate("player/$videoId")
                        },
                        onNavigateToArtist = { artistName ->
                            val encoded = URLEncoder.encode(artistName, "UTF-8")
                            navController.navigate("artist/$encoded")
                        }
                    )
                }
            }
        }
    }
}
