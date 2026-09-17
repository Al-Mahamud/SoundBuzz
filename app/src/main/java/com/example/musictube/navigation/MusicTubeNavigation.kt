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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
                        onNavigateToPlayer = {
                            isPlayerExpanded = true
                        }
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        onNavigateToArtist = { artistName ->
                            val encoded = URLEncoder.encode(artistName, "UTF-8")
                            navController.navigate("artist/$encoded")
                        },
                        onNavigateToPlayer = {
                            isPlayerExpanded = true
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
                            val encoded = URLEncoder.encode(artistName, "UTF-8")
                            navController.navigate("artist/$encoded")
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
                    isPlayerExpanded = false
                    val encoded = URLEncoder.encode(artistName, "UTF-8")
                    navController.navigate("artist/$encoded")
                }
            )
        }
    }
}
