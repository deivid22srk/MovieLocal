package com.movielocal.server.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class VideoProgress(
    val videoId: String,
    val position: Long,
    val duration: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val completed: Boolean = false
)

class ProgressDatabase(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("progress_db", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun saveProgress(progress: VideoProgress) {
        val allProgress = getAllProgress().toMutableMap()
        allProgress[progress.videoId] = progress
        
        val json = gson.toJson(allProgress)
        prefs.edit().putString("progress_data", json).apply()
    }
    
    fun getProgress(videoId: String): VideoProgress? {
        return getAllProgress()[videoId]
    }
    
    fun getAllProgress(): Map<String, VideoProgress> {
        val json = prefs.getString("progress_data", null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, VideoProgress>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }
    
    fun deleteProgress(videoId: String) {
        val allProgress = getAllProgress().toMutableMap()
        allProgress.remove(videoId)
        
        val json = gson.toJson(allProgress)
        prefs.edit().putString("progress_data", json).apply()
    }
    
    fun clear() {
        prefs.edit().clear().apply()
    }
}
