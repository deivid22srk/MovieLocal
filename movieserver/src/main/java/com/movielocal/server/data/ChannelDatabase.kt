package com.movielocal.server.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.movielocal.server.models.Channel
import java.io.File

class ChannelDatabase(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("channels_db", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun saveChannel(channel: Channel) {
        val allChannels = getAllChannels().toMutableList()
        allChannels.removeAll { it.id == channel.id }
        allChannels.add(channel)
        
        val json = gson.toJson(allChannels)
        prefs.edit().putString("channels", json).apply()
    }
    
    fun getAllChannels(): List<Channel> {
        val json = prefs.getString("channels", null) ?: return emptyList()
        val type = object : TypeToken<List<Channel>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getChannel(id: String): Channel? {
        return getAllChannels().find { it.id == id }
    }
    
    fun getActiveChannels(): List<Channel> {
        return getAllChannels().filter { it.isActive }
    }
    
    fun deleteChannel(id: String) {
        val allChannels = getAllChannels().toMutableList()
        allChannels.removeAll { it.id == id }
        
        val json = gson.toJson(allChannels)
        prefs.edit().putString("channels", json).apply()
    }
    
    fun saveThumbnailImage(uri: Uri, channelId: String): String {
        val thumbsDir = File(context.getExternalFilesDir(null), "ChannelThumbnails").apply { mkdirs() }
        val extension = context.contentResolver.getType(uri)?.split("/")?.last() ?: "jpg"
        val thumbFile = File(thumbsDir, "$channelId.$extension")
        
        context.contentResolver.openInputStream(uri)?.use { input ->
            thumbFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        return thumbFile.absolutePath
    }
    
    fun clear() {
        prefs.edit().clear().apply()
    }
}
