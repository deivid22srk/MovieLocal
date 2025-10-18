package com.movielocal.server.ui.media

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
        AddMediaDialog(
            onDismiss = { showAddDialog = false },
            onSave = { item ->
                database.saveMediaItem(item)
                mediaItems = database.getAllMediaItems()
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MediaItemCard(
    item: MediaItem,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                        text = "${item.seasons.size} temporadas",
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
    
    val coverLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> coverUri = uri }
    
    val videoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> videoUri = uri }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Mídia") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        Text(if (coverUri != null) "Capa selecionada" else "Selecionar Capa")
                    }
                }
                
                if (mediaType == MediaType.MOVIE) {
                    item {
                        Button(
                            onClick = { videoLauncher.launch("video/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.VideoLibrary, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (videoUri != null) "Vídeo selecionado" else "Selecionar Vídeo")
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
                        database.saveVideoFile(videoUri!!, itemId)
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
                        filePath = videoPath
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
