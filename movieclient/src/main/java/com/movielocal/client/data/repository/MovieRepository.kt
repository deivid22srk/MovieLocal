package com.movielocal.client.data.repository

import com.movielocal.client.data.api.MovieApi
import com.movielocal.client.data.api.RetrofitClient
import com.movielocal.client.data.models.ContentResponse
import com.movielocal.client.data.models.HealthResponse
import com.movielocal.client.data.models.VideoProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MovieRepository {
    
    private var api: MovieApi? = null
    
    fun setServerUrl(url: String) {
        api = RetrofitClient.getMovieApi(url)
    }
    
    suspend fun checkHealth(): Result<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api?.checkHealth()
            if (response?.isSuccessful == true && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Health check failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getContent(): Result<ContentResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api?.getContent()
            if (response?.isSuccessful == true && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch content"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getProgress(videoId: String): Result<VideoProgress> = withContext(Dispatchers.IO) {
        try {
            val response = api?.getProgress(videoId)
            if (response?.isSuccessful == true && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("No progress found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun saveProgress(videoId: String, progress: VideoProgress): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api?.saveProgress(videoId, progress)
            if (response?.isSuccessful == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to save progress"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
