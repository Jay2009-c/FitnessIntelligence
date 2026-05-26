package com.example.fitnessanalyzer.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.fitnessanalyzer.domain.model.UserProfile

class UserPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun saveUserProfile(profile: UserProfile) {
        sharedPreferences.edit().apply {
            profile.age?.let { putInt(KEY_AGE, it) } ?: remove(KEY_AGE)
            profile.weightKg?.let { putFloat(KEY_WEIGHT, it) } ?: remove(KEY_WEIGHT)
            profile.heightCm?.let { putFloat(KEY_HEIGHT, it) } ?: remove(KEY_HEIGHT)
            profile.isMale?.let { putBoolean(KEY_IS_MALE, it) } ?: remove(KEY_IS_MALE)
            apply()
        }
    }

    fun getUserProfile(): UserProfile {
        val age = if (sharedPreferences.contains(KEY_AGE)) sharedPreferences.getInt(KEY_AGE, 0) else null
        val weight = if (sharedPreferences.contains(KEY_WEIGHT)) sharedPreferences.getFloat(KEY_WEIGHT, 0f) else null
        val height = if (sharedPreferences.contains(KEY_HEIGHT)) sharedPreferences.getFloat(KEY_HEIGHT, 0f) else null
        val isMale = if (sharedPreferences.contains(KEY_IS_MALE)) sharedPreferences.getBoolean(KEY_IS_MALE, true) else null
        
        return UserProfile(age, weight, height, isMale)
    }

    fun saveCloudToken(token: String?) {
        sharedPreferences.edit().putString(KEY_CLOUD_TOKEN, token).apply()
    }

    fun getCloudToken(): String? {
        return sharedPreferences.getString(KEY_CLOUD_TOKEN, null)
    }

    companion object {
        private const val KEY_AGE = "age"
        private const val KEY_WEIGHT = "weight"
        private const val KEY_HEIGHT = "height"
        private const val KEY_IS_MALE = "is_male"
        private const val KEY_CLOUD_TOKEN = "cloud_token"
    }
}
