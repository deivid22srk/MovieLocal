package com.movielocal.client.data.api

import com.movielocal.client.data.models.Profile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ProfileApi {

    @GET("/profiles")
    suspend fun getProfiles(): Response<List<Profile>>

    @POST("/profiles")
    suspend fun createProfile(@Body profile: Profile): Response<Unit>
}
