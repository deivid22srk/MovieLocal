package com.movielocal.server.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class ConnectedClient(
    val clientId: String,
    val deviceName: String,
    val ipAddress: String,
    val lastSeen: Long,
    val currentlyWatching: String? = null,
    val currentPosition: Long = 0
)

class ConnectedClientsManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("connected_clients", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val clients = mutableMapOf<String, ConnectedClient>()
    
    init {
        loadClients()
    }
    
    private fun loadClients() {
        val json = prefs.getString("clients", null)
        if (json != null) {
            val type = object : TypeToken<Map<String, ConnectedClient>>() {}.type
            val loaded = gson.fromJson<Map<String, ConnectedClient>>(json, type)
            clients.putAll(loaded)
            cleanupOldClients()
        }
    }
    
    private fun saveClients() {
        val json = gson.toJson(clients)
        prefs.edit().putString("clients", json).apply()
    }
    
    private fun cleanupOldClients() {
        val now = System.currentTimeMillis()
        val timeout = 5 * 60 * 1000 // 5 minutos
        
        clients.entries.removeIf { (_, client) ->
            now - client.lastSeen > timeout
        }
        saveClients()
    }
    
    fun registerClient(clientId: String, deviceName: String, ipAddress: String) {
        clients[clientId] = ConnectedClient(
            clientId = clientId,
            deviceName = deviceName,
            ipAddress = ipAddress,
            lastSeen = System.currentTimeMillis()
        )
        saveClients()
    }
    
    fun updateClientHeartbeat(clientId: String) {
        clients[clientId]?.let { client ->
            clients[clientId] = client.copy(lastSeen = System.currentTimeMillis())
            saveClients()
        }
    }
    
    fun updateClientWatching(clientId: String, videoTitle: String?, position: Long) {
        clients[clientId]?.let { client ->
            clients[clientId] = client.copy(
                currentlyWatching = videoTitle,
                currentPosition = position,
                lastSeen = System.currentTimeMillis()
            )
            saveClients()
        }
    }
    
    fun getConnectedClients(): List<ConnectedClient> {
        cleanupOldClients()
        return clients.values.toList()
    }
    
    fun removeClient(clientId: String) {
        clients.remove(clientId)
        saveClients()
    }
    
    fun getClientCount(): Int {
        cleanupOldClients()
        return clients.size
    }
}
