package com.movielocal.server.ui.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import java.io.File

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val isVideo: Boolean,
    val size: Long,
    val uri: Uri? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    onFileSelected: (Uri, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentPath by remember { mutableStateOf<File?>(null) }
    var currentUri by remember { mutableStateOf<Uri?>(null) }
    var fileList by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var useDocumentPicker by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            currentPath = Environment.getExternalStorageDirectory()
            loadFiles(context, currentPath!!, currentUri) { files ->
                fileList = files
                isLoading = false
            }
        } else {
            useDocumentPicker = true
        }
    }
    
    val documentTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            currentUri = uri
            isLoading = true
            loadFilesFromUri(context, uri) { files ->
                fileList = files
                isLoading = false
            }
        }
    }
    
    val singleFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val docFile = DocumentFile.fromSingleUri(context, uri)
            val fileName = docFile?.name ?: "video"
            onFileSelected(uri, fileName)
        }
    }
    
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.READ_MEDIA_VIDEO
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                useDocumentPicker = true
            } else {
                showPermissionDialog = true
            }
        } else {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                currentPath = Environment.getExternalStorageDirectory()
                isLoading = true
                loadFiles(context, currentPath!!, currentUri) { files ->
                    fileList = files
                    isLoading = false
                }
            } else {
                showPermissionDialog = true
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Selecionar Vídeo", style = MaterialTheme.typography.titleMedium)
                        if (currentPath != null) {
                            Text(
                                currentPath?.name ?: "Armazenamento",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (currentUri != null) {
                            Text(
                                "Pasta selecionada",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Fechar")
                    }
                },
                actions = {
                    if (currentPath?.parentFile != null) {
                        IconButton(
                            onClick = {
                                currentPath = currentPath?.parentFile
                                isLoading = true
                                loadFiles(context, currentPath!!, currentUri) { files ->
                                    fileList = files
                                    isLoading = false
                                }
                            }
                        ) {
                            Icon(Icons.Default.ArrowUpward, "Voltar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("Selecionar Método") },
                text = { 
                    Text("Escolha como deseja selecionar seus vídeos:")
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showPermissionDialog = false
                                documentTreeLauncher.launch(null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Folder, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Navegar por Pastas")
                        }
                        
                        Button(
                            onClick = {
                                showPermissionDialog = false
                                singleFileLauncher.launch(arrayOf("video/*"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.VideoFile, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Selecionar Arquivo Único")
                        }
                        
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            OutlinedButton(
                                onClick = {
                                    showPermissionDialog = false
                                    val permissions = listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                    permissionLauncher.launch(permissions.toTypedArray())
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Storage, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Usar Armazenamento Legado")
                            }
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                }
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (fileList.isEmpty() && !useDocumentPicker) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Selecione uma pasta para começar",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { showPermissionDialog = true }
                    ) {
                        Icon(Icons.Default.Folder, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Selecionar Pasta")
                    }
                }
            } else if (useDocumentPicker && fileList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Selecionar Vídeos",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Escolha uma pasta ou arquivo de vídeo do seu dispositivo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = { documentTreeLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Icon(Icons.Default.Folder, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Navegar por Pastas")
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = { singleFileLauncher.launch(arrayOf("video/*")) },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Icon(Icons.Default.VideoFile, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Selecionar Arquivo")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(fileList) { file ->
                        FileItemCard(
                            file = file,
                            onClick = {
                                if (file.isDirectory) {
                                    if (file.uri != null) {
                                        isLoading = true
                                        loadFilesFromUri(context, file.uri) { files ->
                                            fileList = files
                                            isLoading = false
                                        }
                                    } else {
                                        currentPath = File(file.path)
                                        isLoading = true
                                        loadFiles(context, currentPath!!, currentUri) { files ->
                                            fileList = files
                                            isLoading = false
                                        }
                                    }
                                } else if (file.isVideo && file.uri != null) {
                                    onFileSelected(file.uri, file.name)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileItemCard(
    file: FileItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (file.isDirectory) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (file.isDirectory)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        file.isDirectory -> Icons.Default.Folder
                        file.isVideo -> Icons.Default.VideoFile
                        else -> Icons.Default.InsertDriveFile
                    },
                    contentDescription = null,
                    tint = if (file.isDirectory)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (file.isDirectory) FontWeight.SemiBold else FontWeight.Normal
                )
                
                if (!file.isDirectory) {
                    Text(
                        text = formatFileSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.ChevronRight else Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun loadFiles(
    context: Context,
    directory: File,
    currentUri: Uri?,
    onLoaded: (List<FileItem>) -> Unit
) {
    try {
        val files = directory.listFiles()?.filter { file ->
            file.isDirectory || isVideoFile(file.name)
        }?.map { file ->
            FileItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                isVideo = isVideoFile(file.name),
                size = if (file.isFile) file.length() else 0,
                uri = Uri.fromFile(file)
            )
        }?.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() }) ?: emptyList()
        
        onLoaded(files)
    } catch (e: Exception) {
        onLoaded(emptyList())
    }
}

private fun loadFilesFromUri(
    context: Context,
    uri: Uri,
    onLoaded: (List<FileItem>) -> Unit
) {
    try {
        val docFile = DocumentFile.fromTreeUri(context, uri)
        val files = docFile?.listFiles()?.filter { file ->
            file.isDirectory || (file.isFile && isVideoFile(file.name ?: ""))
        }?.map { file ->
            FileItem(
                name = file.name ?: "Unknown",
                path = file.uri.toString(),
                isDirectory = file.isDirectory,
                isVideo = file.isFile && isVideoFile(file.name ?: ""),
                size = if (file.isFile) file.length() else 0,
                uri = file.uri
            )
        }?.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() }) ?: emptyList()
        
        onLoaded(files)
    } catch (e: Exception) {
        e.printStackTrace()
        onLoaded(emptyList())
    }
}

private fun isVideoFile(fileName: String): Boolean {
    val videoExtensions = listOf("mp4", "mkv", "avi", "webm", "mov", "flv", "wmv", "m4v", "3gp")
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return extension in videoExtensions
}

private fun formatFileSize(size: Long): String {
    if (size < 1024) return "$size B"
    val kb = size / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}
