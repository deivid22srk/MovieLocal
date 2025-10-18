package com.movielocal.server.ui.settings

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.movielocal.server.data.MediaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { MediaDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var selectedAccount by remember { mutableStateOf<String?>(null) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    
    val accountLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            selectedAccount = accountName
        }
    }
    
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                try {
                    withContext(Dispatchers.IO) {
                        val json = database.exportToJson()
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(json.toByteArray())
                        }
                    }
                    statusMessage = "Backup realizado com sucesso!"
                } catch (e: Exception) {
                    statusMessage = "Erro ao fazer backup: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }
    
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                try {
                    withContext(Dispatchers.IO) {
                        val json = context.contentResolver.openInputStream(uri)?.use { input ->
                            input.readBytes().toString(Charsets.UTF_8)
                        }
                        if (json != null) {
                            database.importFromJson(json)
                        }
                    }
                    statusMessage = "Restauração realizada com sucesso!"
                } catch (e: Exception) {
                    statusMessage = "Erro ao restaurar: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
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
            item {
                Text(
                    text = "Conta Google",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { 
                            Text(selectedAccount ?: "Nenhuma conta selecionada") 
                        },
                        supportingContent = { 
                            Text("Selecione uma conta para backup") 
                        },
                        leadingContent = {
                            Icon(Icons.Default.AccountCircle, null)
                        },
                        trailingContent = {
                            TextButton(
                                onClick = {
                                    val intent = AccountManager.newChooseAccountIntent(
                                        null,
                                        null,
                                        arrayOf("com.google"),
                                        null,
                                        null,
                                        null,
                                        null
                                    )
                                    accountLauncher.launch(intent)
                                }
                            ) {
                                Text("Selecionar")
                            }
                        }
                    )
                }
            }
            
            item {
                Text(
                    text = "Backup e Restauração",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                    .format(Date())
                                backupLauncher.launch("movielocal_backup_$timestamp.json")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Backup, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Fazer Backup no Google Drive")
                        }
                        
                        Button(
                            onClick = {
                                showRestoreDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.CloudDownload, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Restaurar do Google Drive")
                        }
                        
                        if (isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        if (statusMessage != null) {
                            Text(
                                text = statusMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (statusMessage!!.contains("sucesso"))
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            item {
                Text(
                    text = "Informações",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoRow("Versão", "1.0.0")
                        InfoRow("Total de Filmes", database.getMovies().size.toString())
                        InfoRow("Total de Séries", database.getSeries().size.toString())
                    }
                }
            }
            
            item {
                Text(
                    text = "Zona de Perigo",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text("Limpar Todos os Dados") },
                        supportingContent = { Text("Esta ação não pode ser desfeita") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        trailingContent = {
                            TextButton(
                                onClick = { showBackupDialog = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Limpar")
                            }
                        }
                    )
                }
            }
        }
    }
    
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restaurar Backup") },
            text = { Text("Selecione o arquivo de backup do Google Drive para restaurar seus dados.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDialog = false
                        restoreLauncher.launch("application/json")
                    }
                ) {
                    Text("Selecionar Arquivo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Limpar Dados") },
            text = { Text("Tem certeza que deseja limpar todos os dados? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = {
                        database.clear()
                        showBackupDialog = false
                        statusMessage = "Dados limpos com sucesso"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Limpar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
