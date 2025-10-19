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
    
    suspend fun markCompleted(videoId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api?.markCompleted(videoId)
            if (response?.isSuccessful == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to mark as completed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun registerClient(clientId: String, deviceName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = mapOf("clientId" to clientId, "deviceName" to deviceName)
            val response = api?.registerClient(data)
            if (response?.isSuccessful == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to register client"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendHeartbeat(clientId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api?.sendHeartbeat(clientId)
            if (response?.isSuccessful == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to send heartbeat"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateWatching(clientId: String, videoTitle: String?, position: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = mapOf<String, Any>(
                "videoTitle" to (videoTitle ?: ""),
                "position" to position
            )
            val response = api?.updateWatching(clientId, data)
            if (response?.isSuccessful == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update watching"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getChannels(): Result<List<com.movielocal.client.data.models.Channel>> = withContext(Dispatchers.IO) {
        try {
            val response = api?.getChannels()
            if (response?.isSuccessful == true && response.body() != null) {
                Result.success(response.body()!!.channels)
            } else {
                Result.failure(Exception("Failed to fetch channels"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getChannel(channelId: String): Result<com.movielocal.client.data.models.Channel> = withContext(Dispatchers.IO) {
        try {
            val response = api?.getChannel(channelId)
            if (response?.isSuccessful == true && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch channel"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getChannelState(channelId: String): Result<com.movielocal.client.data.models.ChannelState> = withContext(Dispatchers.IO) {
        try {
            val response = api?.getChannelState(channelId)
            if (response?.isSuccessful == true && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch channel state"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun startChannel(channelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api?.startChannel(channelId)
            if (response?.isSuccessful == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to start channel"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun stopChannel(channelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api?.stopChannel(channelId)
            if (response?.isSuccessful == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to stop channel"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
