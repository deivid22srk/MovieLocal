package com.movielocal.client

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.movielocal.client.ui.player.PlayerActivity
import com.movielocal.client.ui.screens.HomeScreen
import com.movielocal.client.ui.screens.SearchScreen
import com.movielocal.client.ui.screens.ProfileScreen
import com.movielocal.client.ui.screens.ProfileSelectionScreen
import com.movielocal.client.ui.screens.ProfileManagementScreen
import com.movielocal.client.ui.screens.BottomNavigationBar
import com.movielocal.client.ui.screens.ServerFoundDialog
import com.movielocal.client.ui.screens.ConnectionDialog
import com.movielocal.client.data.ProfileManager
import androidx.compose.ui.platform.LocalContext
import com.movielocal.client.ui.theme.MovieLocalTheme
import com.movielocal.client.ui.viewmodel.MovieViewModel

class MainActivity : ComponentActivity() {
    
    private val gson = Gson()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MovieLocalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MovieApp(
                        onPlayMovie = { videoUrl, videoId, videoTitle ->
                            startActivity(Intent(this, PlayerActivity::class.java).apply {
                                putExtra("VIDEO_URL", videoUrl)
                                putExtra("VIDEO_ID", videoId)
                                putExtra("VIDEO_TITLE", videoTitle)
                            })
                        },
                        onPlaySeries = { series, season, episode ->
                            val seasonData = series.seasons.find { it.seasonNumber == season }
                            val episodeData = seasonData?.episodes?.find { it.episodeNumber == episode }
                            
                            if (episodeData != null) {
                                startActivity(Intent(this, PlayerActivity::class.java).apply {
                                    putExtra("VIDEO_URL", episodeData.videoUrl)
                                    putExtra("VIDEO_ID", episodeData.id)
                                    putExtra("VIDEO_TITLE", "${series.title} - S${season}E${episode}: ${episodeData.title}")
                                    putExtra("IS_SERIES", true)
                                    putExtra("SERIES_DATA", gson.toJson(series))
                                    putExtra("CURRENT_SEASON", season)
                                    putExtra("CURRENT_EPISODE", episode)
                                })
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MovieApp(
    viewModel: MovieViewModel = viewModel(),
    onPlayMovie: (String, String, String) -> Unit,
    onPlaySeries: (com.movielocal.client.data.models.Series, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val profileManager = remember { ProfileManager(context) }
    
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    var showConnectionDialog by remember { mutableStateOf(connectionState.serverUrl.isEmpty()) }
    var showServerFoundDialog by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf("home") }
    var showProfileSelection by remember { mutableStateOf(false) }
    var showProfileManagement by remember { mutableStateOf(false) }
    var selectedMovie by remember { mutableStateOf<com.movielocal.client.data.models.Movie?>(null) }
    var selectedSeries by remember { mutableStateOf<com.movielocal.client.data.models.Series?>(null) }
    
    LaunchedEffect(connectionState.serverUrl) {
        if (connectionState.serverUrl.isEmpty()) {
            showConnectionDialog = true
            showProfileSelection = false
            viewModel.discoverServer()
        } else if (!profileManager.hasProfile() && !showConnectionDialog) {
            showProfileSelection = true
        }
    }
    
    LaunchedEffect(connectionState.discoveredServerIp) {
        if (connectionState.discoveredServerIp != null && connectionState.serverUrl.isEmpty()) {
            showServerFoundDialog = true
        }
    }
    
    if (showServerFoundDialog && connectionState.discoveredServerIp != null) {
        ServerFoundDialog(
            serverIp = connectionState.discoveredServerIp!!,
            onAccept = {
                viewModel.setServerUrl("${connectionState.discoveredServerIp}:8080")
                showServerFoundDialog = false
                showConnectionDialog = false
                viewModel.clearDiscoveredServer()
                if (!profileManager.hasProfile()) {
                    showProfileSelection = true
                }
            },
            onDecline = {
                showServerFoundDialog = false
                viewModel.clearDiscoveredServer()
            }
        )
    }
    
    if (showConnectionDialog) {
        ConnectionDialog(
            currentServerUrl = connectionState.serverUrl,
            isDiscovering = connectionState.isDiscovering,
            onSetServerUrl = { url ->
                viewModel.setServerUrl(url)
                showConnectionDialog = false
                if (!profileManager.hasProfile()) {
                    showProfileSelection = true
                }
            },
            onDismiss = {
                if (connectionState.serverUrl.isNotEmpty()) {
                    showConnectionDialog = false
                }
            },
            onDiscover = {
                viewModel.discoverServer()
            }
        )
    }
    
    if (showProfileSelection && !showConnectionDialog && !showServerFoundDialog && connectionState.serverUrl.isNotEmpty()) {
        ProfileSelectionScreen(
            onProfileSelected = { profile ->
                profileManager.saveCurrentProfile(profile)
                showProfileSelection = false
            },
            onManageProfiles = {
                showProfileManagement = true
            }
        )
    } else if (showProfileManagement && !showConnectionDialog && !showServerFoundDialog && connectionState.serverUrl.isNotEmpty()) {
        ProfileManagementScreen(
            onBack = {
                showProfileManagement = false
                if (!profileManager.hasProfile()) {
                    showProfileSelection = true
                }
            }
        )
    } else {
    
    Scaffold(
        bottomBar = {
            if (currentScreen != "detail") {
                BottomNavigationBar(
                    currentScreen = currentScreen,
                    onNavigate = { screen -> currentScreen = screen }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.then(if (currentScreen == "detail") Modifier else Modifier.padding(paddingValues))) {
            when (currentScreen) {
                "home" -> HomeScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.loadContent() },
                    onMovieClick = { movie ->
                        selectedMovie = movie
                        selectedSeries = null
                        currentScreen = "detail"
                    },
                    onSeriesClick = { series ->
                        selectedSeries = series
                        selectedMovie = null
                        currentScreen = "detail"
                    },
                    onOpenSettings = { showConnectionDialog = true }
                )
                "search" -> SearchScreen(
                    uiState = uiState,
                    onSearch = { query -> viewModel.updateSearchQuery(query) },
                    onMovieClick = { movie ->
                        selectedMovie = movie
                        selectedSeries = null
                        currentScreen = "detail"
                    },
                    onSeriesClick = { series ->
                        selectedSeries = series
                        selectedMovie = null
                        currentScreen = "detail"
                    }
                )
                "profile" -> ProfileScreen(
                    serverUrl = connectionState.serverUrl,
                    onOpenSettings = { showConnectionDialog = true },
                    onSwitchProfile = {
                        profileManager.clearCurrentProfile()
                        showProfileSelection = true
                    }
                )
                "detail" -> com.movielocal.client.ui.screens.DetailScreen(
                    movie = selectedMovie,
                    series = selectedSeries,
                    onPlayMovie = onPlayMovie,
                    onPlaySeries = onPlaySeries,
                    onBack = { currentScreen = "home" }
                )
            }
        }
    }
    }
}
