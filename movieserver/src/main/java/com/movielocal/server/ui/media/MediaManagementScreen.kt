package com.movielocal.server.ui.media

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.movielocal.server.data.EpisodeData
import com.movielocal.server.data.MediaDatabase
import com.movielocal.server.data.MediaItem
import com.movielocal.server.data.MediaType
import com.movielocal.server.data.SeasonData
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaManagementScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { MediaDatabase(context) }
    var mediaItems by remember { mutableStateOf(database.getAllMediaItems()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerenciar Mídia") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Adicionar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Filmes") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Séries") }
                )
            }
            
            val filteredItems = if (selectedTab == 0) {
                mediaItems.filter { it.type == MediaType.MOVIE }
            } else {
                mediaItems.filter { it.type == MediaType.SERIES }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredItems) { item ->
                    MediaItemCard(
                        item = item,
                        onClick = { selectedItem = item },
                        onDelete = {
                            database.deleteMediaItem(item.id)
                            mediaItems = database.getAllMediaItems()
                        }
                    )
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddMediaDialogEnhanced(
            onDismiss = { showAddDialog = false },
            onSave = { item ->
                database.saveMediaItem(item)
                mediaItems = database.getAllMediaItems()
                showAddDialog = false
            }
        )
    }
    
    if (selectedItem != null && selectedItem!!.type == MediaType.SERIES) {
        SeriesDetailsScreen(
            series = selectedItem!!,
            onDismiss = { selectedItem = null },
            onUpdate = { updated ->
                database.saveMediaItem(updated)
                mediaItems = database.getAllMediaItems()
                selectedItem = updated
            }
        )
    }
}

@Composable
fun MediaItemCard(
    item: MediaItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (item.coverPath != null) {
                Image(
                    painter = rememberAsyncImagePainter(item.coverPath),
                    contentDescription = item.title,
                    modifier = Modifier
                        .width(80.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.type == MediaType.MOVIE) 
                            Icons.Default.Movie 
                        else 
                            Icons.Default.Tv,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = "${item.year} • ${item.genre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFB400)
                    )
                    Text(
                        text = item.rating.toString(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                if (item.type == MediaType.SERIES && item.seasons != null) {
                    Text(
                        text = "${item.seasons.size} temp. • ${item.seasons.sumOf { it.episodes.size }} ep.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMediaDialog(
    onDismiss: () -> Unit,
    onSave: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    val database = remember { MediaDatabase(context) }
    
    var mediaType by remember { mutableStateOf(MediaType.MOVIE) }
    var title by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2024") }
    var genre by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("8.0") }
    var description by remember { mutableStateOf("") }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var detectedDuration by remember { mutableStateOf<Int?>(null) }
    
    val coverLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> coverUri = uri }
    
    val videoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> 
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            videoUri = uri
            
            detectedDuration = database.getVideoDuration(uri)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Mídia") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = mediaType == MediaType.MOVIE,
                            onClick = { mediaType = MediaType.MOVIE },
                            label = { Text("Filme") }
                        )
                        FilterChip(
                            selected = mediaType == MediaType.SERIES,
                            onClick = { mediaType = MediaType.SERIES },
                            label = { Text("Série") }
                        )
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = year,
                            onValueChange = { year = it },
                            label = { Text("Ano") },
                            modifier = Modifier.weight(1f)
                        )
                        
                        OutlinedTextField(
                            value = rating,
                            onValueChange = { rating = it },
                            label = { Text("Nota") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = genre,
                        onValueChange = { genre = it },
                        label = { Text("Gênero") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
                
                item {
                    Button(
                        onClick = { coverLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (coverUri != null) "Capa selecionada ✓" else "Selecionar Capa")
                    }
                }
                
                if (mediaType == MediaType.MOVIE) {
                    item {
                        Button(
                            onClick = { videoLauncher.launch(arrayOf("video/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.VideoLibrary, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (videoUri != null) "Vídeo selecionado ✓" else "Selecionar Vídeo")
                        }
                    }
                    
                    if (detectedDuration != null && detectedDuration!! > 0) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Duração detectada: $detectedDuration minutos",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val itemId = UUID.randomUUID().toString()
                    val coverPath = coverUri?.let { database.saveCoverImage(it, itemId) }
                    val videoPath = if (mediaType == MediaType.MOVIE && videoUri != null) {
                        database.getVideoPathFromUri(videoUri!!)
                    } else null
                    
                    val item = MediaItem(
                        id = itemId,
                        title = title,
                        year = year.toIntOrNull() ?: 2024,
                        genre = genre,
                        rating = rating.toFloatOrNull() ?: 8.0f,
                        description = description,
                        coverPath = coverPath,
                        type = mediaType,
                        filePath = videoPath,
                        seasons = if (mediaType == MediaType.SERIES) emptyList() else null
                    )
                    
                    onSave(item)
                },
                enabled = title.isNotEmpty()
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailsScreen(
    series: MediaItem,
    onDismiss: () -> Unit,
    onUpdate: (MediaItem) -> Unit
) {
    var seasons by remember { mutableStateOf(series.seasons ?: emptyList()) }
    var showAddSeasonDialog by remember { mutableStateOf(false) }
    var selectedSeasonIndex by remember { mutableStateOf<Int?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(series.title) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddSeasonDialog = true }) {
                        Icon(Icons.Default.Add, "Adicionar Temporada")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(seasons) { index, season ->
                SeasonCard(
                    season = season,
                    seasonNumber = season.seasonNumber,
                    onClick = { selectedSeasonIndex = index },
                    onDelete = {
                        seasons = seasons.filterIndexed { i, _ -> i != index }
                        onUpdate(series.copy(seasons = seasons))
                    }
                )
            }
        }
    }
    
    if (showAddSeasonDialog) {
        AddSeasonDialog(
            onDismiss = { showAddSeasonDialog = false },
            onSave = { newSeason ->
                seasons = seasons + newSeason
                onUpdate(series.copy(seasons = seasons))
                showAddSeasonDialog = false
            },
            nextSeasonNumber = (seasons.maxOfOrNull { it.seasonNumber } ?: 0) + 1
        )
    }
    
    if (selectedSeasonIndex != null) {
        EpisodeManagementScreen(
            seriesTitle = series.title,
            season = seasons[selectedSeasonIndex!!],
            onDismiss = { selectedSeasonIndex = null },
            onUpdate = { updatedSeason ->
                seasons = seasons.toMutableList().apply {
                    set(selectedSeasonIndex!!, updatedSeason)
                }
                onUpdate(series.copy(seasons = seasons))
            }
        )
    }
}

@Composable
fun SeasonCard(
    season: SeasonData,
    seasonNumber: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Tv,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Temporada $seasonNumber",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${season.episodes.size} episódios",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSeasonDialog(
    onDismiss: () -> Unit,
    onSave: (SeasonData) -> Unit,
    nextSeasonNumber: Int
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Temporada $nextSeasonNumber") },
        text = {
            Text("Uma nova temporada será criada. Você poderá adicionar episódios em seguida.")
        },
        confirmButton = {
            Button(
                onClick = {
                    val newSeason = SeasonData(
                        seasonNumber = nextSeasonNumber,
                        episodes = emptyList()
                    )
                    onSave(newSeason)
                }
            ) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeManagementScreen(
    seriesTitle: String,
    season: SeasonData,
    onDismiss: () -> Unit,
    onUpdate: (SeasonData) -> Unit
) {
    val context = LocalContext.current
    val database = remember { MediaDatabase(context) }
    
    var episodes by remember { mutableStateOf(season.episodes) }
    var showAddEpisodeDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(seriesTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Temporada ${season.seasonNumber}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddEpisodeDialog = true }) {
                        Icon(Icons.Default.Add, "Adicionar Episódio")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(episodes.sortedBy { it.episodeNumber }) { index, episode ->
                EpisodeCard(
                    episode = episode,
                    onDelete = {
                        episodes = episodes.filterIndexed { i, _ -> i != index }
                        onUpdate(season.copy(episodes = episodes))
                    }
                )
            }
            
            if (episodes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Nenhum episódio adicionado",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showAddEpisodeDialog) {
        AddEpisodeDialog(
            nextEpisodeNumber = (episodes.maxOfOrNull { it.episodeNumber } ?: 0) + 1,
            onDismiss = { showAddEpisodeDialog = false },
            onSave = { newEpisode ->
                episodes = episodes + newEpisode
                onUpdate(season.copy(episodes = episodes))
                showAddEpisodeDialog = false
            }
        )
    }
}

@Composable
fun EpisodeCard(
    episode: EpisodeData,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    
    var fileName by remember { mutableStateOf("") }
    
    LaunchedEffect(episode.filePath) {
        fileName = try {
            val uri = Uri.parse(episode.filePath)
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && it.moveToFirst()) {
                    it.getString(nameIndex)
                } else {
                    episode.filePath.split("/").lastOrNull() ?: "Arquivo"
                }
            } ?: "Arquivo"
        } catch (e: Exception) {
            episode.filePath.split("/").lastOrNull() ?: "Arquivo"
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PlayCircle,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "E${episode.episodeNumber} - ${episode.title}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${episode.duration} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEpisodeDialog(
    nextEpisodeNumber: Int,
    onDismiss: () -> Unit,
    onSave: (EpisodeData) -> Unit
) {
    val context = LocalContext.current
    val database = remember { MediaDatabase(context) }
    
    var episodeTitle by remember { mutableStateOf("Episódio $nextEpisodeNumber") }
    var description by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("45") }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoFileName by remember { mutableStateOf("") }
    var showFileBrowser by remember { mutableStateOf(false) }
    
    val videoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> 
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            videoUri = uri
            
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            videoFileName = cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && it.moveToFirst()) {
                    it.getString(nameIndex)
                } else {
                    "Arquivo selecionado"
                }
            } ?: "Arquivo selecionado"
            
            val detectedDuration = database.getVideoDuration(uri)
            if (detectedDuration > 0) {
                duration = detectedDuration.toString()
            }
        }
    }
    
    if (showFileBrowser) {
        FileBrowserScreen(
            onFileSelected = { uri, fileName ->
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                videoUri = uri
                videoFileName = fileName
                val detectedDuration = database.getVideoDuration(uri)
                if (detectedDuration > 0) {
                    duration = detectedDuration.toString()
                }
                showFileBrowser = false
            },
            onDismiss = { showFileBrowser = false }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Adicionar Episódio $nextEpisodeNumber") },
            text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = episodeTitle,
                        onValueChange = { episodeTitle = it },
                        label = { Text("Título do Episódio") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duração (minutos)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showFileBrowser = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Folder, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Navegar por Arquivos")
                        }
                        
                        OutlinedButton(
                            onClick = { videoLauncher.launch(arrayOf("video/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.VideoFile, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Seletor Rápido")
                        }
                    }
                }
                
                if (videoUri != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Arquivo selecionado:",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = videoFileName,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (videoUri != null) {
                        val videoPath = database.getVideoPathFromUri(videoUri!!)
                        
                        val episode = EpisodeData(
                            episodeNumber = nextEpisodeNumber,
                            title = episodeTitle,
                            description = description,
                            filePath = videoPath ?: videoUri.toString(),
                            duration = duration.toIntOrNull() ?: 45
                        )
                        
                        onSave(episode)
                    }
                },
                enabled = videoUri != null && episodeTitle.isNotEmpty()
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
    }
}
