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
import com.movielocal.client.ui.screens.BottomNavigationBar
import com.movielocal.client.ui.screens.ServerFoundDialog
import com.movielocal.client.ui.screens.ConnectionDialog
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
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    var showConnectionDialog by remember { mutableStateOf(connectionState.serverUrl.isEmpty()) }
    var showServerFoundDialog by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf("home") }
    var selectedMovie by remember { mutableStateOf<com.movielocal.client.data.models.Movie?>(null) }
    var selectedSeries by remember { mutableStateOf<com.movielocal.client.data.models.Series?>(null) }
    
    LaunchedEffect(Unit) {
        if (connectionState.serverUrl.isEmpty()) {
            showConnectionDialog = true
            viewModel.discoverServer()
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
                    onOpenSettings = { showConnectionDialog = true }
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
