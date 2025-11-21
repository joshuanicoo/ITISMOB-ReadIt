package com.mobdeve.s17.group39.itismob_mco.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPrefsManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "book_app_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PROFILE_PICTURE = "user_profile_picture" // Add this
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    // Update to include profile picture
    fun saveUserInfo(userId: String, email: String, name: String, profilePicture: String? = null) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_PROFILE_PICTURE, profilePicture) // Save profile picture
        }.apply()
    }

    // Update to return profile picture
    fun getUserInfo(): Quadruple<String?, String?, String?, String?> {
        return Quadruple(
            prefs.getString(KEY_USER_ID, null),
            prefs.getString(KEY_USER_EMAIL, null),
            prefs.getString(KEY_USER_NAME, null),
            prefs.getString(KEY_USER_PROFILE_PICTURE, null) // Include profile picture
        )
    }

    // Add method to update just the profile picture
    fun updateUserProfilePicture(profilePicture: String?) {
        prefs.edit().putString(KEY_USER_PROFILE_PICTURE, profilePicture).apply()
    }

    // Add method to get just the profile picture
    fun getUserProfilePicture(): String? {
        return prefs.getString(KEY_USER_PROFILE_PICTURE, null)
    }

    fun clearUserData() {
        prefs.edit().clear().apply()
    }

    fun isUserLoggedIn(): Boolean {
        val token = getAuthToken()
        return token != null && JwtUtils.verifyToken(token)
    }

    // Helper methods to get user info from token
    fun getCurrentUserId(): String? {
        val token = getAuthToken()
        return token?.let { JwtUtils.getUserIdFromToken(it) }
    }

    fun getCurrentUserEmail(): String? {
        val token = getAuthToken()
        return token?.let { JwtUtils.getEmailFromToken(it) }
    }

    fun getCurrentUsername(): String? {
        val token = getAuthToken()
        return token?.let { JwtUtils.getUsernameFromToken(it) }
    }
}

// Add this data class for quadruple return type
data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)