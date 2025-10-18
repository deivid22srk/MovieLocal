package com.movielocal.client.data.api

import com.movielocal.client.data.models.ContentResponse
import com.movielocal.client.data.models.HealthResponse
import com.movielocal.client.data.models.VideoProgress
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MovieApi {
    
    @GET("/api/health")
    suspend fun checkHealth(): Response<HealthResponse>
    
    @GET("/api/content")
    suspend fun getContent(): Response<ContentResponse>
    
    @GET("/api/progress/{videoId}")
    suspend fun getProgress(@Path("videoId") videoId: String): Response<VideoProgress>
    
    @POST("/api/progress/{videoId}")
    suspend fun saveProgress(
        @Path("videoId") videoId: String,
        @Body progress: VideoProgress
    ): Response<Map<String, String>>
    
    @POST("/api/progress/{videoId}/completed")
    suspend fun markCompleted(@Path("videoId") videoId: String): Response<Map<String, String>>
    
    @POST("/api/clients/register")
    suspend fun registerClient(@Body data: Map<String, String>): Response<Map<String, String>>
    
    @POST("/api/clients/{clientId}/heartbeat")
    suspend fun sendHeartbeat(@Path("clientId") clientId: String): Response<Map<String, String>>
    
    @POST("/api/clients/{clientId}/watching")
    suspend fun updateWatching(
        @Path("clientId") clientId: String,
        @Body data: Map<String, Any>
    ): Response<Map<String, String>>
}
