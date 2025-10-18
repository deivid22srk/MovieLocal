package com.movielocal.server.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.movielocal.server.models.Movie
import com.movielocal.server.models.Series
import java.io.File

data class MediaItem(
    val id: String,
    val title: String,
    val year: Int,
    val genre: String,
    val rating: Float,
    val description: String,
    val coverPath: String?,
    val type: MediaType,
    val filePath: String? = null,
    val seasons: List<SeasonData>? = null
)

data class SeasonData(
    val seasonNumber: Int,
    val episodes: List<EpisodeData>
)

data class EpisodeData(
    val episodeNumber: Int,
    val title: String,
    val description: String,
    val filePath: String,
    val duration: Int = 45
)

enum class MediaType {
    MOVIE, SERIES
}

class MediaDatabase(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("media_db", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun saveMediaItem(item: MediaItem) {
        val allItems = getAllMediaItems().toMutableList()
        allItems.removeAll { it.id == item.id }
        allItems.add(item)
        
        val json = gson.toJson(allItems)
        prefs.edit().putString("media_items", json).apply()
    }
    
    fun getAllMediaItems(): List<MediaItem> {
        val json = prefs.getString("media_items", null) ?: return emptyList()
        val type = object : TypeToken<List<MediaItem>>() {}.type
        return gson.fromJson(json, type)
    }
    
    fun getMovies(): List<MediaItem> {
        return getAllMediaItems().filter { it.type == MediaType.MOVIE }
    }
    
    fun getSeries(): List<MediaItem> {
        return getAllMediaItems().filter { it.type == MediaType.SERIES }
    }
    
    fun deleteMediaItem(id: String) {
        val allItems = getAllMediaItems().toMutableList()
        allItems.removeAll { it.id == id }
        
        val json = gson.toJson(allItems)
        prefs.edit().putString("media_items", json).apply()
    }
    
    fun saveCoverImage(uri: Uri, itemId: String): String {
        val coversDir = File(context.getExternalFilesDir(null), "Covers").apply { mkdirs() }
        val extension = context.contentResolver.getType(uri)?.split("/")?.last() ?: "jpg"
        val coverFile = File(coversDir, "$itemId.$extension")
        
        context.contentResolver.openInputStream(uri)?.use { input ->
            coverFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        return coverFile.absolutePath
    }
    
    fun saveVideoFile(uri: Uri, itemId: String, episodeId: String? = null): String {
        return getVideoPathFromUri(uri) ?: uri.toString()
    }
    
    fun getVideoPathFromUri(uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.path
        }
        
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            return cursor?.use {
                val columnIndex = it.getColumnIndex("_data")
                if (columnIndex >= 0 && it.moveToFirst()) {
                    it.getString(columnIndex)
                } else {
                    null
                }
            }
        }
        
        return null
    }
    
    fun getVideoDuration(uri: Uri): Int {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            
            if (uri.scheme == "content") {
                retriever.setDataSource(context, uri)
            } else {
                retriever.setDataSource(uri.toString())
            }
            
            val durationMs = retriever.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            
            retriever.release()
            
            (durationMs / 60000).toInt()
        } catch (e: Exception) {
            0
        }
    }
    
    fun exportToJson(): String {
        return gson.toJson(getAllMediaItems())
    }
    
    fun importFromJson(json: String) {
        prefs.edit().putString("media_items", json).apply()
    }
    
    fun clear() {
        prefs.edit().clear().apply()
    }
}
