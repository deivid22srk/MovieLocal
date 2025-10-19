package com.movielocal.client.data.repository

import android.content.Context
import com.google.gson.Gson
import com.movielocal.client.data.api.ProfileApi
import com.movielocal.client.data.api.RetrofitClient
import com.movielocal.client.data.models.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRepository(private val context: Context) {

    private var api: ProfileApi? = null
    private val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun setServerUrl(url: String) {
        api = RetrofitClient.getProfileApi(url)
    }

    suspend fun getProfiles(): Result<List<Profile>> = withContext(Dispatchers.IO) {
        try {
            val response = api?.getProfiles()
            if (response?.isSuccessful == true && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch profiles"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProfile(profile: Profile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api?.createProfile(profile)
            if (response?.isSuccessful == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to create profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSelectedProfile(): Profile? {
        val json = prefs.getString("selected_profile", null)
        return if (json != null) {
            gson.fromJson(json, Profile::class.java)
        } else {
            null
        }
    }

    fun saveSelectedProfile(profile: Profile) {
        val json = gson.toJson(profile)
        prefs.edit().putString("selected_profile", json).apply()
    }
}
