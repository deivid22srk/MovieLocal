package com.movielocal.server.server

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.movielocal.server.models.Channel
import com.movielocal.server.models.ChannelState
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class ChannelStreamer(private val context: Context) {
    
    private val channelStates = ConcurrentHashMap<String, ChannelState>()
    private val streamingJobs = ConcurrentHashMap<String, Job>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    fun startChannel(channel: Channel) {
        if (streamingJobs.containsKey(channel.id)) {
            return
        }
        
        val videoPaths = collectAllVideos(channel.folderPaths)
        if (videoPaths.isEmpty()) {
            return
        }
        
        val initialState = ChannelState(
            channelId = channel.id,
            currentVideoPath = videoPaths[0],
            currentVideoIndex = 0,
            currentPosition = 0,
            allVideoPaths = videoPaths,
            lastUpdated = System.currentTimeMillis()
        )
        channelStates[channel.id] = initialState
        
        val job = scope.launch {
            streamChannel(channel, videoPaths)
        }
        streamingJobs[channel.id] = job
    }
    
    fun stopChannel(channelId: String) {
        streamingJobs[channelId]?.cancel()
        streamingJobs.remove(channelId)
        channelStates.remove(channelId)
    }
    
    fun getChannelState(channelId: String): ChannelState? {
        return channelStates[channelId]
    }
    
    fun isChannelActive(channelId: String): Boolean {
        return streamingJobs.containsKey(channelId)
    }
    
    private suspend fun streamChannel(channel: Channel, videoPaths: List<String>) {
        var currentIndex = 0
        
        while (isActive) {
            val videoPath = videoPaths[currentIndex]
            val duration = getVideoDuration(videoPath)
            
            if (duration > 0) {
                var position = 0L
                while (position < duration && isActive) {
                    channelStates[channel.id] = ChannelState(
                        channelId = channel.id,
                        currentVideoPath = videoPath,
                        currentVideoIndex = currentIndex,
                        currentPosition = position,
                        allVideoPaths = videoPaths,
                        lastUpdated = System.currentTimeMillis()
                    )
                    
                    delay(1000)
                    position += 1000
                }
            }
            
            currentIndex = (currentIndex + 1) % videoPaths.size
        }
    }
    
    private fun collectAllVideos(folderPaths: List<String>): List<String> {
        val allVideos = mutableListOf<String>()
        
        for (folderPath in folderPaths) {
            val videos = getVideosFromFolder(folderPath)
            allVideos.addAll(videos)
        }
        
        return allVideos.sorted()
    }
    
    private fun getVideosFromFolder(folderPath: String): List<String> {
        val videos = mutableListOf<String>()
        
        try {
            if (folderPath.startsWith("content://")) {
                val uri = Uri.parse(folderPath)
                val documentFile = DocumentFile.fromTreeUri(context, uri)
                documentFile?.let { folder ->
                    collectVideosRecursive(folder, videos)
                }
            } else {
                val folder = java.io.File(folderPath)
                if (folder.exists() && folder.isDirectory) {
                    collectVideosFromFileSystem(folder, videos)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return videos
    }
    
    private fun collectVideosRecursive(folder: DocumentFile, videos: MutableList<String>) {
        folder.listFiles().forEach { file ->
            if (file.isDirectory) {
                collectVideosRecursive(file, videos)
            } else if (file.isFile && isVideoFile(file.name ?: "")) {
                videos.add(file.uri.toString())
            }
        }
    }
    
    private fun collectVideosFromFileSystem(folder: java.io.File, videos: MutableList<String>) {
        folder.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                collectVideosFromFileSystem(file, videos)
            } else if (file.isFile && isVideoFile(file.name)) {
                videos.add(file.absolutePath)
            }
        }
    }
    
    private fun isVideoFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in listOf("mp4", "mkv", "avi", "webm", "mov", "flv", "wmv", "m4v")
    }
    
    private fun getVideoDuration(videoPath: String): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            
            if (videoPath.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(videoPath))
            } else {
                retriever.setDataSource(videoPath)
            }
            
            val durationStr = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )
            retriever.release()
            
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }
    
    fun shutdown() {
        streamingJobs.values.forEach { it.cancel() }
        streamingJobs.clear()
        channelStates.clear()
        scope.cancel()
    }
}
