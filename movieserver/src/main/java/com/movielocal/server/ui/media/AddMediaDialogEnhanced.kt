package com.movielocal.server.ui.media

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.movielocal.server.api.MovieApiService
import com.movielocal.server.api.MovieInfo
import com.movielocal.server.data.MediaDatabase
import com.movielocal.server.data.MediaItem
import com.movielocal.server.data.MediaType
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMediaDialogEnhanced(
    onDismiss: () -> Unit,
    onSave: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    val database = remember { MediaDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var mediaType by remember { mutableStateOf(MediaType.MOVIE) }
    var title by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2024") }
    var genre by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("8.0") }
    var description by remember { mutableStateOf("") }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoFileName by remember { mutableStateOf("") }
    var detectedDuration by remember { mutableStateOf<Int?>(null) }
    
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var movieInfo by remember { mutableStateOf<MovieInfo?>(null) }
    var autoFilledCover by remember { mutableStateOf(false) }
    
    var showFileBrowser by remember { mutableStateOf(false) }
    
    val coverLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> 
        coverUri = uri
        autoFilledCover = false
    }
    
    val videoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> 
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            videoUri = uri
            videoFileName = getFileName(context.contentResolver, uri) ?: "video"
            detectedDuration = database.getVideoDuration(uri)
        }
    }
    
    fun searchMovieInfo() {
        if (title.isBlank()) {
            searchError = "Digite um título primeiro"
            return
        }
        
        isSearching = true
        searchError = null
        
        scope.launch {
            try {
                val info = if (mediaType == MediaType.MOVIE) {
                    MovieApiService.searchMovie(title)
                } else {
                    MovieApiService.searchSeries(title)
                }
                
                if (info != null) {
                    movieInfo = info
                    title = info.title
                    year = info.year
                    genre = info.genre
                    rating = info.rating.toString()
                    description = info.plot
                    
                    if (info.posterUrl.isNotEmpty() && info.posterUrl != "N/A") {
                        val posterBytes = MovieApiService.downloadPoster(info.posterUrl)
                        if (posterBytes != null) {
                            val itemId = UUID.randomUUID().toString()
                            val coversDir = File(context.getExternalFilesDir(null), "Covers").apply { mkdirs() }
                            val coverFile = File(coversDir, "$itemId.jpg")
                            coverFile.writeBytes(posterBytes)
                            coverUri = Uri.fromFile(coverFile)
                            autoFilledCover = true
                        }
                    }
                    
                    searchError = null
                } else {
                    searchError = "Filme/Série não encontrado"
                }
            } catch (e: Exception) {
                searchError = "Erro ao buscar: ${e.message}"
            } finally {
                isSearching = false
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
                detectedDuration = database.getVideoDuration(uri)
                showFileBrowser = false
            },
            onDismiss = { showFileBrowser = false }
        )
    } else {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Título") },
                                modifier = Modifier.weight(1f)
                            )
                            
                            IconButton(
                                onClick = { searchMovieInfo() },
                                enabled = !isSearching && title.isNotBlank()
                            ) {
                                if (isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Search, "Buscar")
                                }
                            }
                        }
                    }
                    
                    if (searchError != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = searchError!!,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    
                    if (movieInfo != null) {
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
                                    Text(
                                        text = "Informações carregadas!",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
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
                    
                    if (coverUri != null) {
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(coverUri),
                                    contentDescription = "Capa",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { coverLauncher.launch("image/*") },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Alterar")
                                    }
                                    
                                    OutlinedButton(
                                        onClick = { 
                                            coverUri = null
                                            autoFilledCover = false
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Remover")
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Button(
                                onClick = { coverLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Image, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Selecionar Capa")
                            }
                        }
                    }
                    
                    if (mediaType == MediaType.MOVIE) {
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
                                    Icon(Icons.Default.VideoLibrary, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Seletor Rápido de Vídeo")
                                }
                                
                                if (videoUri != null) {
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
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Vídeo selecionado",
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
                        
                        if (detectedDuration != null && detectedDuration!! > 0) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = "Duração: $detectedDuration minutos",
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
                        val coverPath = coverUri?.let { 
                            if (autoFilledCover) {
                                it.path
                            } else {
                                database.saveCoverImage(it, itemId)
                            }
                        }
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
}

private fun getFileName(contentResolver: android.content.ContentResolver, uri: Uri): String? {
    var fileName: String? = null
    val cursor = contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && it.moveToFirst()) {
            fileName = it.getString(nameIndex)
        }
    }
    return fileName
}
