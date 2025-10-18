package com.movielocal.server.ui.media

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class RealFileItem(
    val file: File,
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val isVideo: Boolean,
    val size: Long,
    val lastModified: Long
)

data class StorageLocation(
    val name: String,
    val path: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isRemovable: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealFileBrowserScreen(
    onFileSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val availableStorages = remember { getAvailableStorages(context) }
    var currentStorageIndex by remember { mutableStateOf(0) }
    var currentDirectory by remember { 
        mutableStateOf<File>(if (availableStorages.isNotEmpty()) File(availableStorages[0].path) else Environment.getExternalStorageDirectory())
    }
    var fileList by remember { mutableStateOf<List<RealFileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showShortcutsDialog by remember { mutableStateOf(false) }
    var showStorageSelector by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (hasStoragePermission(context)) {
            loadDirectory(currentDirectory) { files, error ->
                fileList = files
                errorMessage = error
                isLoading = false
            }
        }
    }
    
    fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showPermissionDialog = true
            }
        }
    }
    
    fun loadCurrentDirectory() {
        isLoading = true
        errorMessage = null
        loadDirectory(currentDirectory) { files, error ->
            fileList = files
            errorMessage = error
            isLoading = false
        }
    }
    
    LaunchedEffect(currentDirectory) {
        loadCurrentDirectory()
    }
    
    LaunchedEffect(Unit) {
        if (!hasStoragePermission(context)) {
            requestStoragePermission()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            "Gerenciador de Arquivos",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            currentDirectory.absolutePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Fechar")
                    }
                },
                actions = {
                    if (availableStorages.size > 1) {
                        IconButton(
                            onClick = { showStorageSelector = true }
                        ) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Text(availableStorages.size.toString())
                            }
                        }
                    }
                    
                    IconButton(
                        onClick = { showShortcutsDialog = true }
                    ) {
                        Icon(Icons.Default.BookmarkBorder, "Atalhos")
                    }
                    
                    if (currentDirectory.parentFile != null) {
                        IconButton(
                            onClick = {
                                currentDirectory = currentDirectory.parentFile!!
                            }
                        ) {
                            Icon(Icons.Default.ArrowUpward, "Pasta anterior")
                        }
                    }
                    
                    IconButton(
                        onClick = {
                            if (availableStorages.isNotEmpty()) {
                                currentDirectory = File(availableStorages[currentStorageIndex].path)
                            } else {
                                currentDirectory = Environment.getExternalStorageDirectory()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Home, "Raiz do armazenamento")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                showPermissionDialog -> {
                    PermissionRequiredScreen(
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                intent.data = Uri.parse("package:${context.packageName}")
                                permissionLauncher.launch(intent)
                            }
                            showPermissionDialog = false
                        },
                        onDismiss = onDismiss
                    )
                }
                
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                errorMessage != null -> {
                    ErrorScreen(
                        message = errorMessage!!,
                        onRetry = { loadCurrentDirectory() },
                        onBack = {
                            if (currentDirectory.parentFile != null) {
                                currentDirectory = currentDirectory.parentFile!!
                            }
                        }
                    )
                }
                
                fileList.isEmpty() -> {
                    EmptyDirectoryScreen(
                        onSelectShortcuts = { showShortcutsDialog = true }
                    )
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(fileList) { fileItem ->
                            RealFileItemCard(
                                fileItem = fileItem,
                                onClick = {
                                    if (fileItem.isDirectory) {
                                        currentDirectory = fileItem.file
                                    } else if (fileItem.isVideo) {
                                        onFileSelected(fileItem.path)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showShortcutsDialog) {
        ShortcutsDialog(
            currentStorage = if (availableStorages.isNotEmpty()) availableStorages[currentStorageIndex] else null,
            onDismiss = { showShortcutsDialog = false },
            onSelectPath = { path ->
                val file = File(path)
                if (file.exists() && file.isDirectory) {
                    currentDirectory = file
                }
                showShortcutsDialog = false
            }
        )
    }
    
    if (showStorageSelector && availableStorages.isNotEmpty()) {
        StorageSelectorDialog(
            storages = availableStorages,
            currentIndex = currentStorageIndex,
            onDismiss = { showStorageSelector = false },
            onSelectStorage = { index ->
                currentStorageIndex = index
                currentDirectory = File(availableStorages[index].path)
                showStorageSelector = false
            }
        )
    }
}

@Composable
fun RealFileItemCard(
    fileItem: RealFileItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (fileItem.isDirectory) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else if (fileItem.isVideo)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
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
                        if (fileItem.isDirectory)
                            MaterialTheme.colorScheme.primaryContainer
                        else if (fileItem.isVideo)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        fileItem.isDirectory -> Icons.Default.Folder
                        fileItem.isVideo -> Icons.Default.VideoFile
                        else -> Icons.Default.InsertDriveFile
                    },
                    contentDescription = null,
                    tint = if (fileItem.isDirectory)
                        MaterialTheme.colorScheme.primary
                    else if (fileItem.isVideo)
                        MaterialTheme.colorScheme.secondary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = fileItem.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (fileItem.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!fileItem.isDirectory) {
                        Text(
                            text = formatFileSize(fileItem.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = formatDate(fileItem.lastModified),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (fileItem.isVideo) {
                    Text(
                        text = fileItem.path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Icon(
                imageVector = if (fileItem.isDirectory) 
                    Icons.Default.ChevronRight 
                else 
                    Icons.Default.Check,
                contentDescription = null,
                tint = if (fileItem.isVideo)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun PermissionRequiredScreen(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            "Permissão Necessária",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            "Para navegar pelos arquivos do seu dispositivo, o aplicativo precisa de permissão de acesso ao armazenamento.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Security, null)
            Spacer(Modifier.width(8.dp))
            Text("Conceder Permissão")
        }
        
        Spacer(Modifier.height(12.dp))
        
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancelar")
        }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            "Erro ao acessar pasta",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(24.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null)
                Spacer(Modifier.width(8.dp))
                Text("Voltar")
            }
            
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Tentar novamente")
            }
        }
    }
}

@Composable
fun EmptyDirectoryScreen(
    onSelectShortcuts: () -> Unit
) {
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
            "Pasta Vazia",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            "Não há arquivos de vídeo nesta pasta",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(24.dp))
        
        OutlinedButton(onClick = onSelectShortcuts) {
            Icon(Icons.Default.BookmarkBorder, null)
            Spacer(Modifier.width(8.dp))
            Text("Ver Atalhos")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutsDialog(
    currentStorage: StorageLocation?,
    onDismiss: () -> Unit,
    onSelectPath: (String) -> Unit
) {
    val storagePath = currentStorage?.path ?: Environment.getExternalStorageDirectory().absolutePath
    val shortcuts = remember(storagePath) {
        listOf(
            "Downloads" to "$storagePath/Download",
            "Filmes" to "$storagePath/Movies",
            "DCIM (Câmera)" to "$storagePath/DCIM",
            "Música" to "$storagePath/Music",
            "Documentos" to "$storagePath/Documents",
            "Vídeos" to "$storagePath/Videos"
        ).filter { (_, path) -> File(path).exists() }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.BookmarkBorder, null)
                Text("Atalhos Rápidos")
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shortcuts) { (name, path) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPath(path) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (name) {
                                    "Armazenamento Interno" -> Icons.Default.Storage
                                    "Downloads" -> Icons.Default.Download
                                    "Filmes" -> Icons.Default.Movie
                                    "DCIM (Câmera)" -> Icons.Default.Camera
                                    "Música" -> Icons.Default.MusicNote
                                    else -> Icons.Default.Folder
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = path,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

private fun hasStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun loadDirectory(
    directory: File,
    onResult: (List<RealFileItem>, String?) -> Unit
) {
    try {
        if (!directory.exists()) {
            onResult(emptyList(), "Pasta não existe")
            return
        }
        
        if (!directory.canRead()) {
            onResult(emptyList(), "Sem permissão para ler esta pasta")
            return
        }
        
        val files = directory.listFiles()
        
        if (files == null) {
            onResult(emptyList(), "Não foi possível listar os arquivos desta pasta")
            return
        }
        
        val fileItems = files
            .filter { file ->
                try {
                    file.canRead() && (file.isDirectory || isVideoFile(file.name))
                } catch (e: Exception) {
                    false
                }
            }
            .map { file ->
                RealFileItem(
                    file = file,
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    isVideo = !file.isDirectory && isVideoFile(file.name),
                    size = if (file.isFile) file.length() else 0L,
                    lastModified = file.lastModified()
                )
            }
            .sortedWith(
                compareBy<RealFileItem> { !it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
        
        onResult(fileItems, null)
        
    } catch (e: SecurityException) {
        onResult(emptyList(), "Sem permissão de acesso: ${e.message}")
    } catch (e: Exception) {
        onResult(emptyList(), "Erro: ${e.message}")
    }
}

private fun isVideoFile(fileName: String): Boolean {
    val videoExtensions = listOf("mp4", "mkv", "avi", "webm", "mov", "flv", "wmv", "m4v", "3gp", "mpg", "mpeg", "ts")
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

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getAvailableStorages(context: Context): List<StorageLocation> {
    val storages = mutableListOf<StorageLocation>()
    
    // Adiciona memória interna
    val internalStorage = Environment.getExternalStorageDirectory()
    storages.add(
        StorageLocation(
            name = "Memória Interna",
            path = internalStorage.absolutePath,
            icon = Icons.Default.Storage,
            isRemovable = false
        )
    )
    
    // Tenta detectar cartão SD externo
    try {
        val externalDirs = context.getExternalFilesDirs(null)
        
        externalDirs?.forEachIndexed { index, file ->
            if (file != null && index > 0) {
                // Navega até a raiz do storage removível
                var current: File? = file
                var sdCardRoot: File? = null
                
                while (current != null) {
                    val parent = current.parentFile
                    if (parent != null && parent.absolutePath == "/storage") {
                        sdCardRoot = current
                        break
                    }
                    current = parent
                }
                
                if (sdCardRoot != null && sdCardRoot.exists() && sdCardRoot.canRead()) {
                    val sdPath = sdCardRoot.absolutePath
                    // Verifica se não é a memória interna
                    if (!sdPath.contains("emulated") && sdPath != internalStorage.absolutePath) {
                        storages.add(
                            StorageLocation(
                                name = "Cartão SD ${if (storages.size > 1) storages.size else ""}".trim(),
                                path = sdPath,
                                icon = Icons.Default.SdCard,
                                isRemovable = true
                            )
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        // Ignora erros de detecção de SD card
    }
    
    // Método alternativo: verifica diretórios comuns de SD card
    val commonSdPaths = listOf(
        "/storage/sdcard1",
        "/storage/extSdCard",
        "/storage/external_SD",
        "/mnt/sdcard/external_sd"
    )
    
    commonSdPaths.forEach { path ->
        val sdFile = File(path)
        if (sdFile.exists() && sdFile.canRead() && !storages.any { it.path == path }) {
            storages.add(
                StorageLocation(
                    name = "Cartão SD",
                    path = path,
                    icon = Icons.Default.SdCard,
                    isRemovable = true
                )
            )
        }
    }
    
    return storages
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSelectorDialog(
    storages: List<StorageLocation>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSelectStorage: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Storage, null)
                Text("Selecionar Armazenamento")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Escolha qual armazenamento deseja navegar:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(8.dp))
                
                storages.forEachIndexed { index, storage ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectStorage(index) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (index == currentIndex)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (index == currentIndex)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.primaryContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = storage.icon,
                                    contentDescription = null,
                                    tint = if (index == currentIndex)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = storage.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (index == currentIndex) 
                                        FontWeight.Bold 
                                    else 
                                        FontWeight.Medium
                                )
                                
                                Text(
                                    text = storage.path,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                if (storage.isRemovable) {
                                    Text(
                                        text = "Removível",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            
                            if (index == currentIndex) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}
