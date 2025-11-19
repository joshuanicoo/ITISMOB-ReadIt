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

    fun saveUserInfo(userId: String, email: String, name: String) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
        }.apply()
    }

    fun getUserInfo(): Triple<String?, String?, String?> {
        return Triple(
            prefs.getString(KEY_USER_ID, null),
            prefs.getString(KEY_USER_EMAIL, null),
            prefs.getString(KEY_USER_NAME, null)
        )
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