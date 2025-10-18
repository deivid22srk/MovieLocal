package com.movielocal.server.server

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.movielocal.server.data.MediaDatabase
import com.movielocal.server.data.MediaType
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
    private val moviesDir: File
    private val seriesDir: File

    init {
        val externalStorage = context.getExternalFilesDir(null)
        moviesDir = File(externalStorage, "Movies").apply { mkdirs() }
        seriesDir = File(externalStorage, "Series").apply { mkdirs() }
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        
        return when {
            uri == "/api/content" -> serveContent()
            uri.startsWith("/api/stream/") -> serveVideo(uri.removePrefix("/api/stream/"))
            uri.startsWith("/api/thumbnail/") -> serveThumbnail(uri.removePrefix("/api/thumbnail/"))
            uri == "/api/health" -> serveHealth()
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
                    "http://localhost:8080/api/thumbnail/${item.coverPath}"
                } else {
                    ""
                },
                videoUrl = if (item.filePath != null) {
                    "http://localhost:8080/api/stream/${item.filePath}"
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
                    "http://localhost:8080/api/thumbnail/${item.coverPath}"
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
                                videoUrl = "http://localhost:8080/api/stream/${ep.filePath}",
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
        val decodedPath = java.net.URLDecoder.decode(path, "UTF-8")
        
        return try {
            val (inputStream, fileSize, mimeType) = if (decodedPath.startsWith("content://")) {
                val uri = Uri.parse(decodedPath)
                val stream = context.contentResolver.openInputStream(uri)
                val size = context.contentResolver.openAssetFileDescriptor(uri, "r")?.length ?: -1L
                val type = context.contentResolver.getType(uri) ?: "video/mp4"
                Triple(stream, size, type)
            } else {
                val file = File(decodedPath)
                if (!file.exists() || !file.isFile) {
                    return newFixedLengthResponse(
                        Response.Status.NOT_FOUND,
                        MIME_PLAINTEXT,
                        "Video not found: $decodedPath"
                    )
                }
                
                val mType = when (file.extension.lowercase()) {
                    "mp4" -> "video/mp4"
                    "mkv" -> "video/x-matroska"
                    "avi" -> "video/x-msvideo"
                    "webm" -> "video/webm"
                    else -> "video/mp4"
                }
                Triple(FileInputStream(file), file.length(), mType)
            }
            
            if (inputStream == null) {
                return newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    MIME_PLAINTEXT,
                    "Cannot open video"
                )
            }
            
            newChunkedResponse(Response.Status.OK, mimeType, inputStream).apply {
                addHeader("Accept-Ranges", "bytes")
                if (fileSize > 0) {
                    addHeader("Content-Length", fileSize.toString())
                }
            }
        } catch (e: Exception) {
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
}
