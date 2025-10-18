package com.movielocal.server.server

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.movielocal.server.data.MediaDatabase
import com.movielocal.server.data.MediaType
import com.movielocal.server.data.ProgressDatabase
import com.movielocal.server.data.VideoProgress
import com.movielocal.server.models.ContentResponse
import com.movielocal.server.models.Episode
import com.movielocal.server.models.Movie
import com.movielocal.server.models.Season
import com.movielocal.server.models.Series
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class MovieServer(
    private val context: Context,
    port: Int = 8080
) : NanoHTTPD("0.0.0.0", port) {

    private val gson = Gson()
    private val database = MediaDatabase(context)
    private val progressDatabase = ProgressDatabase(context)
    private val moviesDir: File
    private val seriesDir: File
    private var serverIp: String = ""

    init {
        val externalStorage = context.getExternalFilesDir(null)
        moviesDir = File(externalStorage, "Movies").apply { mkdirs() }
        seriesDir = File(externalStorage, "Series").apply { mkdirs() }
        serverIp = getServerIp()
        
        android.util.Log.d("MovieServer", "Server initialized on 0.0.0.0:$port")
        android.util.Log.d("MovieServer", "Server accessible at http://$serverIp:$port")
        
        logPermissions()
    }
    
    private fun logPermissions() {
        try {
            val permissions = context.contentResolver.persistedUriPermissions
            android.util.Log.d("MovieServer", "=== SERVER STARTED ===")
            android.util.Log.d("MovieServer", "Server IP: $serverIp")
            android.util.Log.d("MovieServer", "Total persistent URI permissions: ${permissions.size}")
            permissions.forEach { perm ->
                android.util.Log.d("MovieServer", "  - ${perm.uri} (read=${perm.isReadPermission}, write=${perm.isWritePermission})")
            }
        } catch (e: Exception) {
            android.util.Log.e("MovieServer", "Error logging permissions: ${e.message}")
        }
    }
    
    private fun getServerIp(): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ipInt = wifiInfo.ipAddress
            
            if (ipInt == 0) {
                "localhost"
            } else {
                String.format(
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            }
        } catch (e: Exception) {
            "localhost"
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method
        
        android.util.Log.d("MovieServer", "Request from ${session.remoteIpAddress}: $method $uri")
        
        if (method == Method.OPTIONS) {
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "").apply {
                addCorsHeaders()
            }
        }
        
        val response = when {
            uri == "/api/content" -> serveContent()
            uri.startsWith("/api/stream/") -> serveVideo(uri.removePrefix("/api/stream/"))
            uri.startsWith("/api/thumbnail/") -> serveThumbnail(uri.removePrefix("/api/thumbnail/"))
            uri == "/api/health" -> serveHealth()
            uri == "/api/debug/permissions" -> serveDebugPermissions()
            uri.startsWith("/api/progress/") && method == Method.GET -> {
                val videoId = uri.removePrefix("/api/progress/")
                getProgress(videoId)
            }
            uri.startsWith("/api/progress/") && method == Method.POST -> {
                val videoId = uri.removePrefix("/api/progress/")
                saveProgress(session, videoId)
            }
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
        }
        
        return response.apply { addCorsHeaders() }
    }
    
    private fun Response.addCorsHeaders() {
        addHeader("Access-Control-Allow-Origin", "*")
        addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
        addHeader("Access-Control-Max-Age", "3600")
    }
    
    private fun serveDebugPermissions(): Response {
        val permissions = context.contentResolver.persistedUriPermissions
        val debugInfo = mapOf(
            "serverIp" to serverIp,
            "totalPermissions" to permissions.size,
            "permissions" to permissions.map { 
                mapOf(
                    "uri" to it.uri.toString(),
                    "read" to it.isReadPermission,
                    "write" to it.isWritePermission
                )
            }
        )
        
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            gson.toJson(debugInfo)
        ).apply {
            addHeader("Access-Control-Allow-Origin", "*")
        }
    }

    private fun serveHealth(): Response {
        val response = mapOf(
            "status" to "ok",
            "version" to "1.0",
            "serverTime" to System.currentTimeMillis()
        )
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            gson.toJson(response)
        )
    }

    private fun serveContent(): Response {
        val dbMovies = database.getMovies().map { item ->
            Movie(
                id = item.id,
                title = item.title,
                description = item.description,
                year = item.year,
                genre = item.genre,
                rating = item.rating,
                duration = 120,
                thumbnailUrl = if (item.coverPath != null) {
                    "http://$serverIp:8080/api/thumbnail/${item.coverPath}"
                } else {
                    ""
                },
                videoUrl = if (item.filePath != null) {
                    "http://$serverIp:8080/api/stream/${item.filePath}"
                } else {
                    ""
                },
                filePath = item.filePath ?: ""
            )
        }
        
        val dbSeries = database.getSeries().map { item ->
            Series(
                id = item.id,
                title = item.title,
                description = item.description,
                year = item.year,
                genre = item.genre,
                rating = item.rating,
                thumbnailUrl = if (item.coverPath != null) {
                    "http://$serverIp:8080/api/thumbnail/${item.coverPath}"
                } else {
                    ""
                },
                seasons = item.seasons?.map { season ->
                    Season(
                        seasonNumber = season.seasonNumber,
                        episodes = season.episodes.map { ep ->
                            Episode(
                                id = "${item.id}_S${season.seasonNumber}E${ep.episodeNumber}",
                                episodeNumber = ep.episodeNumber,
                                title = ep.title,
                                description = ep.description,
                                duration = ep.duration,
                                thumbnailUrl = "",
                                videoUrl = "http://$serverIp:8080/api/stream/${ep.filePath}",
                                filePath = ep.filePath
                            )
                        }
                    )
                } ?: emptyList()
            )
        }
        
        val contentResponse = ContentResponse(movies = dbMovies, series = dbSeries)
        val json = gson.toJson(contentResponse)
        
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            json
        ).apply {
            addHeader("Access-Control-Allow-Origin", "*")
        }
    }

    private fun serveVideo(path: String): Response {
        return try {
            val decodedPath = java.net.URLDecoder.decode(path, "UTF-8")
            android.util.Log.d("MovieServer", "Serving video request for: $decodedPath")
            
            if (decodedPath.startsWith("content://")) {
                val uri = Uri.parse(decodedPath)
                android.util.Log.d("MovieServer", "Parsed URI: $uri")
                
                try {
                    val persistedPermissions = context.contentResolver.persistedUriPermissions
                    android.util.Log.d("MovieServer", "Total persisted permissions: ${persistedPermissions.size}")
                    persistedPermissions.forEach { perm ->
                        android.util.Log.d("MovieServer", "Permission: ${perm.uri} (read=${perm.isReadPermission})")
                    }
                    
                    val hasPermission = persistedPermissions.any { 
                        perm -> perm.uri.toString() == uri.toString() && perm.isReadPermission 
                    }
                    
                    android.util.Log.d("MovieServer", "Has permission for $uri: $hasPermission")
                    
                    if (!hasPermission) {
                        android.util.Log.w("MovieServer", "URI not in persisted permissions, checking tree permissions...")
                        
                        val treePermission = persistedPermissions.firstOrNull { perm ->
                            perm.isReadPermission && uri.toString().contains(perm.uri.toString().substringAfter("document/"))
                        }
                        
                        if (treePermission != null) {
                            android.util.Log.d("MovieServer", "Found tree permission: ${treePermission.uri}")
                        } else {
                            android.util.Log.e("MovieServer", "WARNING: No permission found. This may cause access failure.")
                        }
                    }
                    
                    val assetFileDescriptor = try {
                        context.contentResolver.openAssetFileDescriptor(uri, "r")
                    } catch (e: Exception) {
                        android.util.Log.e("MovieServer", "AssetFileDescriptor failed, trying DocumentFile...")
                        
                        val documentFile = DocumentFile.fromSingleUri(context, uri)
                        if (documentFile != null && documentFile.canRead()) {
                            android.util.Log.d("MovieServer", "DocumentFile accessible, trying via ParcelFileDescriptor...")
                            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                            if (pfd != null) {
                                val inputStream = android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd)
                                val mimeType = documentFile.type ?: "video/mp4"
                                val fileSize = documentFile.length()
                                
                                android.util.Log.d("MovieServer", "SUCCESS via DocumentFile - size: $fileSize")
                                
                                return newChunkedResponse(Response.Status.OK, mimeType, inputStream).apply {
                                    addHeader("Accept-Ranges", "bytes")
                                    addHeader("Access-Control-Allow-Origin", "*")
                                    if (fileSize > 0) {
                                        addHeader("Content-Length", fileSize.toString())
                                    }
                                }
                            }
                        }
                        throw e
                    }
                    
                    if (assetFileDescriptor == null) {
                        android.util.Log.e("MovieServer", "openAssetFileDescriptor returned null for URI: $uri")
                        return newFixedLengthResponse(
                            Response.Status.FORBIDDEN,
                            "application/json",
                            "{\"error\":\"Cannot open video - permission denied. Please re-add this video in the server app.\"}"
                        ).apply {
                            addHeader("Access-Control-Allow-Origin", "*")
                        }
                    }
                    
                    val inputStream = assetFileDescriptor.createInputStream()
                    val fileSize = assetFileDescriptor.length
                    
                    val mimeType = context.contentResolver.getType(uri) ?: "video/mp4"
                    android.util.Log.d("MovieServer", "SUCCESS - Streaming URI with mimeType: $mimeType, size: $fileSize bytes")
                    
                    newChunkedResponse(Response.Status.OK, mimeType, inputStream).apply {
                        addHeader("Accept-Ranges", "bytes")
                        addHeader("Access-Control-Allow-Origin", "*")
                        if (fileSize > 0) {
                            addHeader("Content-Length", fileSize.toString())
                        }
                    }
                } catch (e: SecurityException) {
                    android.util.Log.e("MovieServer", "SecurityException for URI $uri: ${e.message}", e)
                    newFixedLengthResponse(
                        Response.Status.FORBIDDEN,
                        "application/json",
                        "{\"error\":\"Permission denied: ${e.message}. Please re-add this video in the server app to grant permissions again.\"}"
                    ).apply {
                        addHeader("Access-Control-Allow-Origin", "*")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MovieServer", "Exception opening URI $uri: ${e.message}", e)
                    e.printStackTrace()
                    newFixedLengthResponse(
                        Response.Status.INTERNAL_ERROR,
                        "application/json",
                        "{\"error\":\"Error opening video: ${e.message}. If you reinstalled the app, please re-add all videos.\"}"
                    ).apply {
                        addHeader("Access-Control-Allow-Origin", "*")
                    }
                }
            } else {
                val file = File(decodedPath)
                if (!file.exists() || !file.isFile) {
                    android.util.Log.e("MovieServer", "File not found: $decodedPath")
                    return newFixedLengthResponse(
                        Response.Status.NOT_FOUND,
                        MIME_PLAINTEXT,
                        "Video not found: $decodedPath"
                    )
                }
                
                val mimeType = when (file.extension.lowercase()) {
                    "mp4" -> "video/mp4"
                    "mkv" -> "video/x-matroska"
                    "avi" -> "video/x-msvideo"
                    "webm" -> "video/webm"
                    else -> "video/mp4"
                }
                
                val inputStream = FileInputStream(file)
                
                newChunkedResponse(Response.Status.OK, mimeType, inputStream).apply {
                    addHeader("Accept-Ranges", "bytes")
                    addHeader("Access-Control-Allow-Origin", "*")
                    addHeader("Content-Length", file.length().toString())
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MovieServer", "Fatal error in serveVideo: ${e.message}", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT,
                "Error streaming video: ${e.message}"
            )
        }
    }

    private fun serveThumbnail(path: String): Response {
        val decodedPath = java.net.URLDecoder.decode(path, "UTF-8")
        val file = File(decodedPath)
        
        if (!file.exists() || !file.isFile) {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "Thumbnail not found"
            )
        }

        val mimeType = when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }

        return try {
            val fis = FileInputStream(file)
            newChunkedResponse(Response.Status.OK, mimeType, fis)
        } catch (e: Exception) {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT,
                "Error loading thumbnail"
            )
        }
    }
    
    private fun getProgress(videoId: String): Response {
        val decodedVideoId = java.net.URLDecoder.decode(videoId, "UTF-8")
        val progress = progressDatabase.getProgress(decodedVideoId)
        
        return if (progress != null) {
            newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                gson.toJson(progress)
            ).apply {
                addHeader("Access-Control-Allow-Origin", "*")
            }
        } else {
            newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                "application/json",
                gson.toJson(mapOf("error" to "No progress found"))
            ).apply {
                addHeader("Access-Control-Allow-Origin", "*")
            }
        }
    }
    
    private fun saveProgress(session: IHTTPSession, videoId: String): Response {
        return try {
            val decodedVideoId = java.net.URLDecoder.decode(videoId, "UTF-8")
            
            val bodyMap = mutableMapOf<String, String>()
            session.parseBody(bodyMap)
            val body = bodyMap["postData"] ?: ""
            
            val progressData = gson.fromJson(body, VideoProgress::class.java)
            
            val progress = VideoProgress(
                videoId = decodedVideoId,
                position = progressData.position,
                duration = progressData.duration,
                timestamp = System.currentTimeMillis()
            )
            
            progressDatabase.saveProgress(progress)
            
            newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                gson.toJson(mapOf("status" to "success"))
            ).apply {
                addHeader("Access-Control-Allow-Origin", "*")
            }
        } catch (e: Exception) {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                gson.toJson(mapOf("error" to e.message))
            ).apply {
                addHeader("Access-Control-Allow-Origin", "*")
            }
        }
    }
}
