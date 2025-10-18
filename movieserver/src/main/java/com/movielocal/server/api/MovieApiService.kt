package com.movielocal.server.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class MovieInfo(
    val title: String,
    val year: String,
    val genre: String,
    val rating: Float,
    val plot: String,
    val posterUrl: String,
    val runtime: String
)

object MovieApiService {
    
    private const val OMDB_API_KEY = "6e4d3b57"
    private const val OMDB_BASE_URL = "https://www.omdbapi.com/"
    
    private const val TAG = "MovieApiService"
    
    suspend fun searchMovie(title: String): MovieInfo? = withContext(Dispatchers.IO) {
        try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val urlString = "${OMDB_BASE_URL}?apikey=${OMDB_API_KEY}&t=${encodedTitle}"
            
            Log.d(TAG, "Searching movie: $title")
            Log.d(TAG, "URL: $urlString")
            
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Response: $response")
                
                val jsonObject = JSONObject(response)
                
                if (jsonObject.optString("Response") == "True") {
                    val imdbRating = jsonObject.optString("imdbRating", "N/A")
                    val rating = if (imdbRating != "N/A") {
                        imdbRating.toFloatOrNull() ?: 0f
                    } else {
                        0f
                    }
                    
                    val movieInfo = MovieInfo(
                        title = jsonObject.optString("Title", title),
                        year = jsonObject.optString("Year", "2024").take(4),
                        genre = jsonObject.optString("Genre", "Unknown"),
                        rating = rating,
                        plot = jsonObject.optString("Plot", ""),
                        posterUrl = jsonObject.optString("Poster", ""),
                        runtime = jsonObject.optString("Runtime", "")
                    )
                    
                    Log.d(TAG, "Movie found: ${movieInfo.title}")
                    return@withContext movieInfo
                } else {
                    val error = jsonObject.optString("Error", "Unknown error")
                    Log.e(TAG, "API Error: $error")
                }
            } else {
                Log.e(TAG, "HTTP Error: $responseCode")
            }
            
            connection.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Exception searching movie: ${e.message}", e)
            e.printStackTrace()
        }
        
        return@withContext null
    }
    
    suspend fun searchSeries(title: String): MovieInfo? = withContext(Dispatchers.IO) {
        try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val urlString = "${OMDB_BASE_URL}?apikey=${OMDB_API_KEY}&t=${encodedTitle}&type=series"
            
            Log.d(TAG, "Searching series: $title")
            
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)
                
                if (jsonObject.optString("Response") == "True") {
                    val imdbRating = jsonObject.optString("imdbRating", "N/A")
                    val rating = if (imdbRating != "N/A") {
                        imdbRating.toFloatOrNull() ?: 0f
                    } else {
                        0f
                    }
                    
                    val movieInfo = MovieInfo(
                        title = jsonObject.optString("Title", title),
                        year = jsonObject.optString("Year", "2024").substringBefore("–").trim(),
                        genre = jsonObject.optString("Genre", "Unknown"),
                        rating = rating,
                        plot = jsonObject.optString("Plot", ""),
                        posterUrl = jsonObject.optString("Poster", ""),
                        runtime = jsonObject.optString("Runtime", "")
                    )
                    
                    Log.d(TAG, "Series found: ${movieInfo.title}")
                    return@withContext movieInfo
                }
            }
            
            connection.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Exception searching series: ${e.message}", e)
        }
        
        return@withContext null
    }
    
    suspend fun downloadPoster(posterUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            if (posterUrl.isEmpty() || posterUrl == "N/A") {
                return@withContext null
            }
            
            Log.d(TAG, "Downloading poster: $posterUrl")
            
            val url = URL(posterUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val bytes = connection.inputStream.use { it.readBytes() }
                Log.d(TAG, "Poster downloaded: ${bytes.size} bytes")
                return@withContext bytes
            }
            
            connection.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Exception downloading poster: ${e.message}", e)
        }
        
        return@withContext null
    }
}
