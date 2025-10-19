package com.movielocal.server.ui.channels

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.movielocal.server.data.ChannelDatabase
import com.movielocal.server.models.Channel
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { ChannelDatabase(context) }
    var channels by remember { mutableStateOf(database.getAllChannels()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingChannel by remember { mutableStateOf<Channel?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerenciar Canais") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Adicionar Canal")
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
            if (channels.isEmpty()) {
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
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Nenhum canal criado",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Adicione um canal para transmitir vídeos ao vivo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(channels) { channel ->
                        ChannelCard(
                            channel = channel,
                            onEdit = { editingChannel = channel },
                            onDelete = {
                                database.deleteChannel(channel.id)
                                channels = database.getAllChannels()
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddChannelDialog(
            onDismiss = { showAddDialog = false },
            onSave = { channel ->
                database.saveChannel(channel)
                channels = database.getAllChannels()
                showAddDialog = false
            }
        )
    }
    
    if (editingChannel != null) {
        EditChannelDialog(
            channel = editingChannel!!,
            onDismiss = { editingChannel = null },
            onSave = { channel ->
                database.saveChannel(channel)
                channels = database.getAllChannels()
                editingChannel = null
            }
        )
    }
}

@Composable
fun ChannelCard(
    channel: Channel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (channel.thumbnailUrl.isNotEmpty()) {
                val file = File(channel.thumbnailUrl)
                if (file.exists()) {
                    Image(
                        painter = rememberAsyncImagePainter(file),
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Tv,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Tv,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = channel.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                Text(
                    text = "${channel.folderPaths.size} pasta(s) selecionada(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Editar")
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, "Deletar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Deletar Canal") },
            text = { Text("Tem certeza que deseja deletar o canal '${channel.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Deletar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChannelDialog(
    onDismiss: () -> Unit,
    onSave: (Channel) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedFolders by remember { mutableStateOf<List<String>>(emptyList()) }
    var thumbnailUri by remember { mutableStateOf<Uri?>(null) }
    var showFolderSelector by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        thumbnailUri = uri
    }
    
    if (showFolderSelector) {
        FolderSelectorScreen(
            onFoldersSelected = { folders ->
                selectedFolders = folders
                showFolderSelector = false
            },
            onDismiss = { showFolderSelector = false }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Novo Canal") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome do Canal") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        minLines = 2
                    )
                    
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, null, Modifier.padding(end = 8.dp))
                        Text("Selecionar Imagem")
                    }
                    
                    thumbnailUri?.let {
                        Text("Imagem selecionada: ✓", color = MaterialTheme.colorScheme.primary)
                    }
                    
                    Button(
                        onClick = { showFolderSelector = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, null, Modifier.padding(end = 8.dp))
                        Text("Selecionar Pastas de Vídeos")
                    }
                    
                    if (selectedFolders.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${selectedFolders.size} pasta(s) selecionada(s)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(
                                        onClick = { selectedFolders = emptyList() }
                                    ) {
                                        Text("Limpar")
                                    }
                                }
                                
                                selectedFolders.take(3).forEach { path ->
                                    Text(
                                        text = "• ${File(path).name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                
                                if (selectedFolders.size > 3) {
                                    Text(
                                        text = "... e mais ${selectedFolders.size - 3} pasta(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank() && selectedFolders.isNotEmpty()) {
                            val channelId = UUID.randomUUID().toString()
                            val thumbnailPath = thumbnailUri?.let {
                                ChannelDatabase(context).saveThumbnailImage(it, channelId)
                            } ?: ""
                            
                            val channel = Channel(
                                id = channelId,
                                name = name,
                                description = description,
                                thumbnailUrl = thumbnailPath,
                                folderPaths = selectedFolders
                            )
                            onSave(channel)
                        }
                    },
                    enabled = name.isNotBlank() && selectedFolders.isNotEmpty()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChannelDialog(
    channel: Channel,
    onDismiss: () -> Unit,
    onSave: (Channel) -> Unit
) {
    var name by remember { mutableStateOf(channel.name) }
    var description by remember { mutableStateOf(channel.description) }
    var selectedFolders by remember { mutableStateOf(channel.folderPaths) }
    var thumbnailUri by remember { mutableStateOf<Uri?>(null) }
    var showFolderSelector by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        thumbnailUri = uri
    }
    
    if (showFolderSelector) {
        FolderSelectorScreen(
            onFoldersSelected = { folders ->
                selectedFolders = folders
                showFolderSelector = false
            },
            onDismiss = { showFolderSelector = false }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Editar Canal") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome do Canal") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        minLines = 2
                    )
                    
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, null, Modifier.padding(end = 8.dp))
                        Text("Alterar Imagem")
                    }
                    
                    thumbnailUri?.let {
                        Text("Nova imagem selecionada: ✓", color = MaterialTheme.colorScheme.primary)
                    }
                    
                    Button(
                        onClick = { showFolderSelector = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, null, Modifier.padding(end = 8.dp))
                        Text("Selecionar Pastas de Vídeos")
                    }
                    
                    if (selectedFolders.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${selectedFolders.size} pasta(s) selecionada(s)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(
                                        onClick = { selectedFolders = emptyList() }
                                    ) {
                                        Text("Limpar")
                                    }
                                }
                                
                                selectedFolders.take(3).forEach { path ->
                                    Text(
                                        text = "• ${File(path).name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                
                                if (selectedFolders.size > 3) {
                                    Text(
                                        text = "... e mais ${selectedFolders.size - 3} pasta(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank() && selectedFolders.isNotEmpty()) {
                            val thumbnailPath = thumbnailUri?.let {
                                ChannelDatabase(context).saveThumbnailImage(it, channel.id)
                            } ?: channel.thumbnailUrl
                            
                            val updatedChannel = channel.copy(
                                name = name,
                                description = description,
                                thumbnailUrl = thumbnailPath,
                                folderPaths = selectedFolders
                            )
                            onSave(updatedChannel)
                        }
                    },
                    enabled = name.isNotBlank() && selectedFolders.isNotEmpty()
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
