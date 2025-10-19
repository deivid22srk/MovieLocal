package com.movielocal.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.movielocal.client.data.ProfileManager
import com.movielocal.client.data.api.RetrofitClient
import com.movielocal.client.data.models.Profile
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ProfileSelectionScreen(
    onProfileSelected: (Profile) -> Unit,
    onManageProfiles: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileManager = remember { ProfileManager(context) }
    val serverUrl = remember { 
        context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .getString("server_url", "") ?: ""
    }
    
    var profiles by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                if (serverUrl.isNotEmpty()) {
                    val baseUrl = if (serverUrl.startsWith("http")) serverUrl else "http://$serverUrl"
                    val api = RetrofitClient.getMovieApi(baseUrl)
                    val response = api.getProfiles()
                    if (response.isSuccessful && response.body() != null) {
                        profiles = response.body()!!.profiles
                    } else {
                        errorMessage = "Erro ao carregar perfis"
                    }
                } else {
                    errorMessage = "Servidor não conectado"
                }
            } catch (e: Exception) {
                errorMessage = "Erro: ${e.message}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Quem está assistindo?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }
                errorMessage != null -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                profiles.isEmpty() -> {
                    Text("Nenhum perfil encontrado")
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(profiles) { profile ->
                            ProfileCard(
                                profile = profile,
                                onClick = {
                                    profileManager.saveCurrentProfile(profile)
                                    onProfileSelected(profile)
                                }
                            )
                        }
                        
                        item {
                            AddProfileCard(onClick = onManageProfiles)
                        }
                    }
                }
            }
            
            if (profiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                
                OutlinedButton(
                    onClick = onManageProfiles,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gerenciar Perfis")
                }
            }
        }
    }
}

@Composable
fun ProfileCard(
    profile: Profile,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getIconForAvatar(profile.avatarIcon),
                contentDescription = profile.name,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = profile.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        if (profile.isKidsMode) {
            Text(
                text = "👶 Infantil",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AddProfileCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Adicionar Perfil",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Adicionar Perfil",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

fun getIconForAvatar(avatarIcon: String): ImageVector {
    return when (avatarIcon) {
        "person" -> Icons.Default.Person
        "face" -> Icons.Default.Face
        "child" -> Icons.Default.ChildCare
        "star" -> Icons.Default.Star
        "favorite" -> Icons.Default.Favorite
        "movie" -> Icons.Default.Movie
        "music" -> Icons.Default.MusicNote
        "sports" -> Icons.Default.SportsEsports
        "school" -> Icons.Default.School
        "work" -> Icons.Default.Work
        else -> Icons.Default.Person
    }
}
