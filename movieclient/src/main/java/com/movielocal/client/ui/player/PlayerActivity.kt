package com.movielocal.client.ui.player

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.movielocal.client.data.models.VideoProgress
import com.movielocal.client.data.repository.MovieRepository
import com.movielocal.client.ui.theme.MovieLocalTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayerActivity : ComponentActivity() {
    
    private var player: ExoPlayer? = null
    private val repository = MovieRepository()
    private var videoId: String = ""
    private var videoTitle: String = ""
    private var progressJob: kotlinx.coroutines.Job? = null
    private var isInPipMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        val videoUrl = intent.getStringExtra("VIDEO_URL") ?: ""
        videoId = intent.getStringExtra("VIDEO_ID") ?: ""
        videoTitle = intent.getStringExtra("VIDEO_TITLE") ?: ""
        
        val serverUrl = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getString("server_url", "") ?: ""
        
        if (serverUrl.isNotEmpty()) {
            repository.setServerUrl(serverUrl)
        }
        
        setContent {
            MovieLocalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    VideoPlayerScreen(
                        videoUrl = videoUrl,
                        videoId = videoId,
                        videoTitle = videoTitle,
                        repository = repository,
                        onBackPressed = { 
                            saveCurrentProgress()
                            finish() 
                        },
                        onPlayerReady = { exoPlayer ->
                            player = exoPlayer
                            startProgressTracking()
                        },
                        onEnterPip = {
                            enterPipMode()
                        }
                    )
                }
            }
        }
    }
    
    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val rational = Rational(16, 9)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(rational)
                .build()
            enterPictureInPictureMode(params)
        }
    }
    
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }
    
    private fun startProgressTracking() {
        progressJob = lifecycleScope.launch {
            while (true) {
                delay(10000) // Save every 10 seconds
                saveCurrentProgress()
            }
        }
    }
    
    private fun saveCurrentProgress() {
        player?.let { p ->
            if (p.duration > 0 && videoId.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        val progress = VideoProgress(
                            videoId = videoId,
                            position = p.currentPosition,
                            duration = p.duration
                        )
                        repository.saveProgress(videoId, progress)
                    } catch (e: Exception) {
                        // Silent fail
                    }
                }
            }
        }
    }
    
    override fun onStop() {
        super.onStop()
        saveCurrentProgress()
        player?.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        progressJob?.cancel()
        saveCurrentProgress()
        player?.release()
        player = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    videoUrl: String,
    videoId: String,
    videoTitle: String,
    repository: MovieRepository,
    onBackPressed: () -> Unit,
    onPlayerReady: (ExoPlayer) -> Unit,
    onEnterPip: () -> Unit
) {
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }
    var showResumeDialog by remember { mutableStateOf(false) }
    var savedProgress by remember { mutableStateOf<VideoProgress?>(null) }
    var playerInitialized by remember { mutableStateOf(false) }
    var aspectRatioMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var showAspectRatioMenu by remember { mutableStateOf(false) }
    var playerViewRef by remember { mutableStateOf<StyledPlayerView?>(null) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = false
            onPlayerReady(this)
        }
    }
    
    LaunchedEffect(videoId) {
        if (videoId.isNotEmpty()) {
            try {
                val result = repository.getProgress(videoId)
                if (result.isSuccess) {
                    val progress = result.getOrNull()
                    if (progress != null && progress.position > 5000) {
                        savedProgress = progress
                        showResumeDialog = true
                    } else {
                        exoPlayer.playWhenReady = true
                        playerInitialized = true
                    }
                } else {
                    exoPlayer.playWhenReady = true
                    playerInitialized = true
                }
            } catch (e: Exception) {
                exoPlayer.playWhenReady = true
                playerInitialized = true
            }
        } else {
            exoPlayer.playWhenReady = true
            playerInitialized = true
        }
    }
    
    if (showResumeDialog && savedProgress != null) {
        ResumeDialog(
            progress = savedProgress!!,
            videoTitle = videoTitle,
            onResume = {
                exoPlayer.seekTo(savedProgress!!.position)
                exoPlayer.playWhenReady = true
                showResumeDialog = false
                playerInitialized = true
            },
            onStartFromBeginning = {
                exoPlayer.seekTo(0)
                exoPlayer.playWhenReady = true
                showResumeDialog = false
                playerInitialized = true
            }
        )
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { showControls = !showControls }
    ) {
        AndroidView(
            factory = { ctx ->
                StyledPlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = aspectRatioMode
                    playerViewRef = this
                }
            },
            update = { view ->
                view.resizeMode = aspectRatioMode
            },
            modifier = Modifier.fillMaxSize()
        )
        
        if (showControls) {
            TopAppBar(
                title = { 
                    Text(
                        videoTitle, 
                        color = Color.White,
                        maxLines = 1
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAspectRatioMenu = !showAspectRatioMenu }) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Proporção de Tela",
                            tint = Color.White
                        )
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        IconButton(onClick = onEnterPip) {
                            Icon(
                                imageVector = Icons.Default.PictureInPicture,
                                contentDescription = "Picture in Picture",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                modifier = Modifier.align(Alignment.TopCenter)
            )
            
            if (showAspectRatioMenu) {
                AspectRatioMenu(
                    currentMode = aspectRatioMode,
                    onSelectMode = { mode ->
                        aspectRatioMode = mode
                        playerViewRef?.resizeMode = mode
                        showAspectRatioMenu = false
                    },
                    onDismiss = { showAspectRatioMenu = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 64.dp, end = 8.dp)
                )
            }
            
            PlayerControls(
                player = exoPlayer,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun AspectRatioMenu(
    currentMode: Int,
    onSelectMode: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.9f)
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Proporção de Tela",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                modifier = Modifier.padding(8.dp)
            )
            
            AspectRatioMenuItem(
                text = "Ajustar",
                icon = Icons.Default.FitScreen,
                isSelected = currentMode == AspectRatioFrameLayout.RESIZE_MODE_FIT,
                onClick = { onSelectMode(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
            )
            
            AspectRatioMenuItem(
                text = "Preencher",
                icon = Icons.Default.Fullscreen,
                isSelected = currentMode == AspectRatioFrameLayout.RESIZE_MODE_FILL,
                onClick = { onSelectMode(AspectRatioFrameLayout.RESIZE_MODE_FILL) }
            )
            
            AspectRatioMenuItem(
                text = "Zoom",
                icon = Icons.Default.ZoomOutMap,
                isSelected = currentMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                onClick = { onSelectMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM) }
            )
            
            AspectRatioMenuItem(
                text = "Esticar",
                icon = Icons.Default.OpenInFull,
                isSelected = currentMode == AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH,
                onClick = { onSelectMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH) }
            )
        }
    }
}

@Composable
fun AspectRatioMenuItem(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
        )
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun PlayerControls(
    player: ExoPlayer,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    
    LaunchedEffect(player) {
        while (true) {
            isPlaying = player.isPlaying
            currentPosition = player.currentPosition
            duration = player.duration
            delay(500)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(16.dp)
    ) {
        Slider(
            value = if (duration > 0) currentPosition.toFloat() else 0f,
            onValueChange = { player.seekTo(it.toLong()) },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime(currentPosition),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { player.seekTo(player.currentPosition - 10000) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Voltar 10s",
                        tint = Color.White
                    )
                }
                
                IconButton(
                    onClick = {
                        if (isPlaying) player.pause() else player.play()
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                IconButton(
                    onClick = { player.seekTo(player.currentPosition + 10000) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Avançar 10s",
                        tint = Color.White
                    )
                }
            }
            
            Text(
                text = formatTime(duration),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

fun formatTime(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun ResumeDialog(
    progress: VideoProgress,
    videoTitle: String,
    onResume: () -> Unit,
    onStartFromBeginning: () -> Unit
) {
    val progressPercent = ((progress.position.toFloat() / progress.duration.toFloat()) * 100).toInt()
    val progressMinutes = (progress.position / 60000).toInt()
    val progressSeconds = ((progress.position % 60000) / 1000).toInt()
    
    AlertDialog(
        onDismissRequest = onStartFromBeginning,
        icon = {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text("Continuar Assistindo?") },
        text = {
            Column {
                Text(
                    text = videoTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Você assistiu $progressPercent% deste vídeo.")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Última posição: ${progressMinutes}m ${progressSeconds}s")
            }
        },
        confirmButton = {
            Button(onClick = onResume) {
                Text("Continuar")
            }
        },
        dismissButton = {
            TextButton(onClick = onStartFromBeginning) {
                Text("Começar do Início")
            }
        }
    )
}
