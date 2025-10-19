package com.movielocal.client.data

import android.content.Context
import android.os.Build
import com.movielocal.client.data.repository.MovieRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class ClientManager(private val context: Context, private val repository: MovieRepository) {
    
    private val prefs = context.getSharedPreferences("client_prefs", Context.MODE_PRIVATE)
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    val clientId: String
        get() {
            var id = prefs.getString("client_id", null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString("client_id", id).apply()
            }
            return id
        }
    
    val deviceName: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL}"
    
    suspend fun register() {
        try {
            repository.registerClient(clientId, deviceName)
            startHeartbeat()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive) {
                try {
                    repository.sendHeartbeat(clientId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(60000) // 1 minuto
            }
        }
    }
    
    fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
    
    suspend fun updateWatching(videoTitle: String?, position: Long) {
        try {
            repository.updateWatching(clientId, videoTitle, position)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
