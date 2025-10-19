package com.movielocal.server.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.movielocal.server.models.Profile
import com.movielocal.server.models.WatchProgress
import java.io.File
import java.util.UUID

class ProfileDatabase(context: Context) {
    private val profilesFile = File(context.filesDir, "profiles.json")
    private val progressFile = File(context.filesDir, "watch_progress.json")
    private val gson = Gson()
    
    init {
        if (!profilesFile.exists()) {
            val defaultProfile = Profile(
                id = UUID.randomUUID().toString(),
                name = "Perfil Principal",
                avatarIcon = "person",
                isKidsMode = false
            )
            saveProfiles(listOf(defaultProfile))
        }
        
        if (!progressFile.exists()) {
            saveProgress(emptyList())
        }
    }
    
    fun getAllProfiles(): List<Profile> {
        return try {
            if (profilesFile.exists()) {
                val json = profilesFile.readText()
                val type = object : TypeToken<List<Profile>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getProfile(profileId: String): Profile? {
        return getAllProfiles().find { it.id == profileId }
    }
    
    fun createProfile(profile: Profile): Profile {
        val profiles = getAllProfiles().toMutableList()
        profiles.add(profile)
        saveProfiles(profiles)
        return profile
    }
    
    fun updateProfile(profile: Profile): Profile {
        val profiles = getAllProfiles().toMutableList()
        val index = profiles.indexOfFirst { it.id == profile.id }
        if (index != -1) {
            profiles[index] = profile
            saveProfiles(profiles)
        }
        return profile
    }
    
    fun deleteProfile(profileId: String) {
        val profiles = getAllProfiles().toMutableList()
        profiles.removeAll { it.id == profileId }
        saveProfiles(profiles)
        
        val progressList = getAllProgress().toMutableList()
        progressList.removeAll { it.profileId == profileId }
        saveProgress(progressList)
    }
    
    private fun saveProfiles(profiles: List<Profile>) {
        val json = gson.toJson(profiles)
        profilesFile.writeText(json)
    }
    
    fun getAllProgress(): List<WatchProgress> {
        return try {
            if (progressFile.exists()) {
                val json = progressFile.readText()
                val type = object : TypeToken<List<WatchProgress>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getProgressForProfile(profileId: String): List<WatchProgress> {
        return getAllProgress().filter { it.profileId == profileId }
    }
    
    fun getProgressForContent(profileId: String, contentId: String): WatchProgress? {
        return getAllProgress().find { 
            it.profileId == profileId && it.contentId == contentId 
        }
    }
    
    fun saveWatchProgress(progress: WatchProgress) {
        val progressList = getAllProgress().toMutableList()
        val index = progressList.indexOfFirst { 
            it.profileId == progress.profileId && it.contentId == progress.contentId 
        }
        
        if (index != -1) {
            progressList[index] = progress
        } else {
            progressList.add(progress)
        }
        
        saveProgress(progressList)
    }
    
    fun deleteProgress(profileId: String, contentId: String) {
        val progressList = getAllProgress().toMutableList()
        progressList.removeAll { 
            it.profileId == profileId && it.contentId == contentId 
        }
        saveProgress(progressList)
    }
    
    private fun saveProgress(progressList: List<WatchProgress>) {
        val json = gson.toJson(progressList)
        progressFile.writeText(json)
    }
    
    fun getContinueWatching(profileId: String): List<WatchProgress> {
        return getProgressForProfile(profileId)
            .filter { !it.completed && it.progress > 0 }
            .sortedByDescending { it.lastWatched }
            .take(10)
    }
    
    fun getWatchHistory(profileId: String): List<WatchProgress> {
        return getProgressForProfile(profileId)
            .sortedByDescending { it.lastWatched }
    }
    
    fun clearAllData() {
        profilesFile.delete()
        progressFile.delete()
    }
}
