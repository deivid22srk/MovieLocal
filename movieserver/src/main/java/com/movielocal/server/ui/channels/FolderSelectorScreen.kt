package com.movielocal.server.ui.channels

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
import com.movielocal.server.ui.media.StorageLocation
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderSelectorScreen(
    onFoldersSelected: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val availableStorages = remember { getAvailableStorages(context) }
    var currentDirectory by remember { 
        mutableStateOf<File>(if (availableStorages.isNotEmpty()) File(availableStorages[0].path) else Environment.getExternalStorageDirectory())
    }
    var selectedFolders by remember { mutableStateOf<Set<String>>(emptySet()) }
    var folderList by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showStorageSelector by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (hasStoragePermission(context)) {
            loadFolders(currentDirectory) { folders ->
                folderList = folders
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
        loadFolders(currentDirectory) { folders ->
            folderList = folders
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
                            "Selecionar Pastas de Vídeos",
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
                            Icon(Icons.Default.Storage, "Armazenamento")
                        }
                    }
                    
                    if (currentDirectory.parent != null) {
                        IconButton(
                            onClick = { 
                                currentDirectory = currentDirectory.parentFile ?: currentDirectory
                            }
                        ) {
                            Icon(Icons.Default.ArrowUpward, "Voltar")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (selectedFolders.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${selectedFolders.size} pasta(s) selecionada(s)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Button(
                            onClick = { onFoldersSelected(selectedFolders.toList()) }
                        ) {
                            Icon(Icons.Default.Check, null, Modifier.padding(end = 8.dp))
                            Text("Confirmar")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text("Erro ao acessar pasta", style = MaterialTheme.typography.titleMedium)
                        Text(errorMessage!!, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(folderList) { folder ->
                        FolderItem(
                            folder = folder,
                            isSelected = selectedFolders.contains(folder.absolutePath),
                            onToggleSelect = {
                                selectedFolders = if (selectedFolders.contains(folder.absolutePath)) {
                                    selectedFolders - folder.absolutePath
                                } else {
                                    selectedFolders + folder.absolutePath
                                }
                            },
                            onNavigate = {
                                currentDirectory = folder
                            }
                        )
                    }
                    
                    if (folderList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Nenhuma pasta neste diretório",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permissão Necessária") },
            text = { 
                Text("Este app precisa de permissão para acessar todos os arquivos para gerenciar sua biblioteca de mídia.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intent.data = Uri.parse("package:${context.packageName}")
                            permissionLauncher.launch(intent)
                        }
                        showPermissionDialog = false
                    }
                ) {
                    Text("Conceder Permissão")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    if (showStorageSelector) {
        AlertDialog(
            onDismissRequest = { showStorageSelector = false },
            title = { Text("Selecionar Armazenamento") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableStorages.forEachIndexed { index, storage ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentDirectory = File(storage.path)
                                    showStorageSelector = false
                                },
                            color = if (currentDirectory.absolutePath.startsWith(storage.path))
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(storage.icon, null)
                                Text(storage.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStorageSelector = false }) {
                    Text("Fechar")
                }
            }
        )
    }
}

@Composable
fun FolderItem(
    folder: File,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onNavigate: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 3.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() }
            )
            
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigate() }
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = folder.absolutePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = { onNavigate() }) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Abrir"
                )
            }
        }
    }
}

private fun loadFolders(directory: File, callback: (List<File>) -> Unit) {
    try {
        if (!directory.exists() || !directory.canRead()) {
            callback(emptyList())
            return
        }
        
        val folders = directory.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name } ?: emptyList()
        callback(folders)
    } catch (e: Exception) {
        e.printStackTrace()
        callback(emptyList())
    }
}

private fun getAvailableStorages(context: Context): List<StorageLocation> {
    val storages = mutableListOf<StorageLocation>()
    
    val externalStorage = Environment.getExternalStorageDirectory()
    storages.add(
        StorageLocation(
            name = "Armazenamento Interno",
            path = externalStorage.absolutePath,
            icon = Icons.Default.PhoneAndroid,
            isRemovable = false
        )
    )
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.getExternalFilesDirs(null).forEach { file ->
            if (file != null && file.absolutePath.contains("emulated/0").not()) {
                storages.add(
                    StorageLocation(
                        name = "Cartão SD",
                        path = file.absolutePath.substringBefore("/Android"),
                        icon = Icons.Default.SdCard,
                        isRemovable = true
                    )
                )
            }
        }
    }
    
    return storages
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
