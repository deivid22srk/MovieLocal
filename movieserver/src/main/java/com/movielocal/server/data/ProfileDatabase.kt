package com.movielocal.server.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.movielocal.server.models.Profile
import java.io.File

class ProfileDatabase(private val context: Context) {

    private val gson = Gson()
    private val profilesFile: File
        get() = File(context.filesDir, "profiles.json")

    fun getProfiles(): List<Profile> {
        if (!profilesFile.exists()) {
            return emptyList()
        }
        val json = profilesFile.readText()
        val type = object : TypeToken<List<Profile>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveProfiles(profiles: List<Profile>) {
        val json = gson.toJson(profiles)
        profilesFile.writeText(json)
    }
}
