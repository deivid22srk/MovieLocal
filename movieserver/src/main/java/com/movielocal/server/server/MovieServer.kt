package com.movielocal.server.server

import android.content.Context
import android.net.Uri
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
) : NanoHTTPD(port) {

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
        
        return when {
            uri == "/api/content" -> serveContent()
            uri.startsWith("/api/stream/") -> serveVideo(uri.removePrefix("/api/stream/"))
            uri.startsWith("/api/thumbnail/") -> serveThumbnail(uri.removePrefix("/api/thumbnail/"))
            uri == "/api/health" -> serveHealth()
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
            android.util.Log.d("MovieServer", "Serving video: $decodedPath")
            
            if (decodedPath.startsWith("content://")) {
                val uri = Uri.parse(decodedPath)
                
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    
                    if (inputStream == null) {
                        android.util.Log.e("MovieServer", "Cannot open input stream for URI: $uri")
                        return newFixedLengthResponse(
                            Response.Status.NOT_FOUND,
                            MIME_PLAINTEXT,
                            "Cannot open video from URI"
                        )
                    }
                    
                    val fileSize = try {
                        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                            it.length
                        } ?: -1L
                    } catch (e: Exception) {
                        android.util.Log.w("MovieServer", "Cannot get file size: ${e.message}")
                        -1L
                    }
                    
                    val mimeType = context.contentResolver.getType(uri) ?: "video/mp4"
                    android.util.Log.d("MovieServer", "Streaming URI with mimeType: $mimeType, size: $fileSize")
                    
                    newChunkedResponse(Response.Status.OK, mimeType, inputStream).apply {
                        addHeader("Accept-Ranges", "bytes")
                        addHeader("Access-Control-Allow-Origin", "*")
                        if (fileSize > 0) {
                            addHeader("Content-Length", fileSize.toString())
                        }
                    }
                } catch (e: SecurityException) {
                    android.util.Log.e("MovieServer", "Security exception accessing URI: ${e.message}")
                    newFixedLengthResponse(
                        Response.Status.FORBIDDEN,
                        MIME_PLAINTEXT,
                        "Permission denied to access video"
                    )
                } catch (e: Exception) {
                    android.util.Log.e("MovieServer", "Error opening URI: ${e.message}", e)
                    newFixedLengthResponse(
                        Response.Status.INTERNAL_ERROR,
                        MIME_PLAINTEXT,
                        "Error opening video: ${e.message}"
                    )
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
