package com.movielocal.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.movielocal.client.data.api.RetrofitClient
import com.movielocal.client.data.models.Profile
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagementScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val serverUrl = remember { 
        context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .getString("server_url", "") ?: ""
    }
    
    var profiles by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<Profile?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    
    fun loadProfiles() {
        isLoading = true
        scope.launch {
            try {
                if (serverUrl.isNotEmpty()) {
                    val baseUrl = if (serverUrl.startsWith("http")) serverUrl else "http://$serverUrl"
                    android.util.Log.d("ProfileManagement", "Loading profiles from: $baseUrl")
                    val api = RetrofitClient.getMovieApi(baseUrl)
                    val response = api.getProfiles()
                    android.util.Log.d("ProfileManagement", "Response: ${response.code()}")
                    if (response.isSuccessful && response.body() != null) {
                        profiles = response.body()!!.profiles
                        android.util.Log.d("ProfileManagement", "Loaded ${profiles.size} profiles")
                    } else {
                        statusMessage = "Erro ao carregar perfis: ${response.code()}"
                    }
                } else {
                    statusMessage = "Servidor não configurado"
                }
            } catch (e: Exception) {
                statusMessage = "Erro: ${e.message}"
                android.util.Log.e("ProfileManagement", "Error loading profiles", e)
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(Unit) {
        loadProfiles()
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            statusMessage = null
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gerenciar Perfis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Adicionar Perfil")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(profiles) { profile ->
                    ProfileManagementCard(
                        profile = profile,
                        onEdit = {
                            selectedProfile = profile
                            showEditDialog = true
                        },
                        onDelete = {
                            selectedProfile = profile
                            showDeleteDialog = true
                        }
                    )
                }
                
                if (profiles.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Nenhum perfil cadastrado",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        ProfileEditDialog(
            profile = null,
            onDismiss = { showAddDialog = false },
            onSave = { profile ->
                scope.launch {
                    try {
                        android.util.Log.d("ProfileManagement", "Creating profile: ${profile.name}")
                        if (serverUrl.isNotEmpty()) {
                            val baseUrl = if (serverUrl.startsWith("http")) serverUrl else "http://$serverUrl"
                            val api = RetrofitClient.getMovieApi(baseUrl)
                            android.util.Log.d("ProfileManagement", "API call to: $baseUrl/api/profiles")
                            val response = api.createProfile(profile)
                            android.util.Log.d("ProfileManagement", "Response code: ${response.code()}")
                            if (response.isSuccessful) {
                                statusMessage = "Perfil criado com sucesso!"
                                showAddDialog = false
                                loadProfiles()
                            } else {
                                statusMessage = "Erro ao criar perfil: ${response.code()}"
                                android.util.Log.e("ProfileManagement", "Error response: ${response.errorBody()?.string()}")
                            }
                        }
                    } catch (e: Exception) {
                        statusMessage = "Erro: ${e.message}"
                        android.util.Log.e("ProfileManagement", "Exception creating profile", e)
                        e.printStackTrace()
                    }
                }
            }
        )
    }
    
    if (showEditDialog && selectedProfile != null) {
        ProfileEditDialog(
            profile = selectedProfile,
            onDismiss = { showEditDialog = false },
            onSave = { profile ->
                scope.launch {
                    try {
                        android.util.Log.d("ProfileManagement", "Updating profile: ${profile.id} - ${profile.name}")
                        if (serverUrl.isNotEmpty()) {
                            val baseUrl = if (serverUrl.startsWith("http")) serverUrl else "http://$serverUrl"
                            val api = RetrofitClient.getMovieApi(baseUrl)
                            val response = api.updateProfile(profile.id, profile)
                            android.util.Log.d("ProfileManagement", "Update response code: ${response.code()}")
                            if (response.isSuccessful) {
                                statusMessage = "Perfil atualizado com sucesso!"
                                showEditDialog = false
                                loadProfiles()
                            } else {
                                statusMessage = "Erro ao atualizar perfil: ${response.code()}"
                                android.util.Log.e("ProfileManagement", "Error response: ${response.errorBody()?.string()}")
                            }
                        }
                    } catch (e: Exception) {
                        statusMessage = "Erro: ${e.message}"
                        android.util.Log.e("ProfileManagement", "Exception updating profile", e)
                        e.printStackTrace()
                    }
                }
            }
        )
    }
    
    if (showDeleteDialog && selectedProfile != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir Perfil") },
            text = { Text("Tem certeza que deseja excluir o perfil \"${selectedProfile!!.name}\"? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                if (serverUrl.isNotEmpty()) {
                                    val baseUrl = if (serverUrl.startsWith("http")) serverUrl else "http://$serverUrl"
                                    val api = RetrofitClient.getMovieApi(baseUrl)
                                    val response = api.deleteProfile(selectedProfile!!.id)
                                    if (response.isSuccessful) {
                                        statusMessage = "Perfil excluído com sucesso!"
                                        showDeleteDialog = false
                                        loadProfiles()
                                    } else {
                                        statusMessage = "Erro ao excluir perfil"
                                    }
                                }
                            } catch (e: Exception) {
                                statusMessage = "Erro: ${e.message}"
                                e.printStackTrace()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Excluir")
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

@Composable
fun ProfileManagementCard(
    profile: Profile,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconForAvatar(profile.avatarIcon),
                        contentDescription = profile.name,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Column {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (profile.isKidsMode) {
                        Text(
                            text = "👶 Modo Infantil",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        "Editar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        "Excluir",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditDialog(
    profile: Profile?,
    onDismiss: () -> Unit,
    onSave: (Profile) -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var selectedAvatar by remember { mutableStateOf(profile?.avatarIcon ?: "person") }
    var isKidsMode by remember { mutableStateOf(profile?.isKidsMode ?: false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    
    val avatarOptions = listOf(
        "person" to "Pessoa",
        "face" to "Rosto",
        "child" to "Criança",
        "star" to "Estrela",
        "favorite" to "Coração",
        "movie" to "Filme",
        "music" to "Música",
        "sports" to "Jogos",
        "school" to "Escola",
        "work" to "Trabalho"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(if (profile == null) "Novo Perfil" else "Editar Perfil") 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Perfil") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedButton(
                    onClick = { showAvatarPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = getIconForAvatar(selectedAvatar),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Escolher Avatar")
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Modo Infantil")
                    Switch(
                        checked = isKidsMode,
                        onCheckedChange = { isKidsMode = it }
                    )
                }
                
                if (isKidsMode) {
                    Text(
                        text = "👶 Conteúdo será filtrado para crianças",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    android.util.Log.d("ProfileEditDialog", "Save button clicked!")
                    android.util.Log.d("ProfileEditDialog", "Profile name: $name")
                    val newProfile = Profile(
                        id = profile?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        avatarIcon = selectedAvatar,
                        isKidsMode = isKidsMode,
                        createdAt = profile?.createdAt ?: System.currentTimeMillis()
                    )
                    android.util.Log.d("ProfileEditDialog", "Calling onSave with profile: $newProfile")
                    onSave(newProfile)
                },
                enabled = name.isNotBlank()
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
    
    if (showAvatarPicker) {
        AlertDialog(
            onDismissRequest = { showAvatarPicker = false },
            title = { Text("Escolher Avatar") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(avatarOptions) { (icon, label) ->
                        Column(
                            modifier = Modifier
                                .clickable {
                                    selectedAvatar = icon
                                    showAvatarPicker = false
                                }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedAvatar == icon) 
                                            MaterialTheme.colorScheme.primaryContainer 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getIconForAvatar(icon),
                                    contentDescription = label,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarPicker = false }) {
                    Text("Fechar")
                }
            }
        )
    }
}
