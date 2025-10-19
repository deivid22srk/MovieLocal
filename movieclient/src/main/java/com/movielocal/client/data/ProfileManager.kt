package com.movielocal.client.data

import android.content.Context
import com.movielocal.client.data.models.Profile

class ProfileManager(context: Context) {
    private val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
    
    fun saveCurrentProfile(profile: Profile) {
        prefs.edit().apply {
            putString("current_profile_id", profile.id)
            putString("current_profile_name", profile.name)
            putString("current_profile_avatar", profile.avatarIcon)
            putBoolean("current_profile_kids_mode", profile.isKidsMode)
            apply()
        }
    }
    
    fun getCurrentProfileId(): String? {
        return prefs.getString("current_profile_id", null)
    }
    
    fun getCurrentProfileName(): String? {
        return prefs.getString("current_profile_name", null)
    }
    
    fun getCurrentProfileAvatar(): String? {
        return prefs.getString("current_profile_avatar", null)
    }
    
    fun isKidsMode(): Boolean {
        return prefs.getBoolean("current_profile_kids_mode", false)
    }
    
    fun clearCurrentProfile() {
        prefs.edit().clear().apply()
    }
    
    fun hasProfile(): Boolean {
        return getCurrentProfileId() != null
    }
}
