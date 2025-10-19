package com.movielocal.client.data.api

import com.movielocal.client.data.models.ContentResponse
import com.movielocal.client.data.models.HealthResponse
import com.movielocal.client.data.models.VideoProgress
import com.movielocal.client.data.models.Profile
import com.movielocal.client.data.models.ProfilesResponse
import com.movielocal.client.data.models.WatchProgress
import com.movielocal.client.data.models.ContinueWatchingResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
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
    
    @GET("/api/profiles")
    suspend fun getProfiles(): Response<ProfilesResponse>
    
    @POST("/api/profiles")
    suspend fun createProfile(@Body profile: Profile): Response<Profile>
    
    @PUT("/api/profiles/{profileId}")
    suspend fun updateProfile(
        @Path("profileId") profileId: String,
        @Body profile: Profile
    ): Response<Profile>
    
    @DELETE("/api/profiles/{profileId}")
    suspend fun deleteProfile(@Path("profileId") profileId: String): Response<Map<String, String>>
    
    @GET("/api/profiles/{profileId}/continue-watching")
    suspend fun getContinueWatching(@Path("profileId") profileId: String): Response<ContinueWatchingResponse>
    
    @POST("/api/profiles/{profileId}/progress")
    suspend fun saveWatchProgress(
        @Path("profileId") profileId: String,
        @Body progress: WatchProgress
    ): Response<Map<String, String>>
}
