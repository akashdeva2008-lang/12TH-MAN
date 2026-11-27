package com.example.twelveman.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.twelveman.data.model.User

class SessionManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREF_NAME = "12thman_session"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHONE = "phone"
        private const val KEY_ROLE = "role"
        private const val KEY_CUSTOM_SERVER_URL = "custom_server_url"
        private const val KEY_REGISTERED_TEAM_IDS = "registered_team_ids"
        private const val KEY_REGISTERED_TEAM_NAMES = "registered_team_names"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun saveCustomServerUrl(url: String?) {
        prefs.edit().putString(KEY_CUSTOM_SERVER_URL, url).apply()
    }

    fun getCustomServerUrl(): String? {
        return prefs.getString(KEY_CUSTOM_SERVER_URL, null)
    }

    fun saveAuthToken(token: String?) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun saveUser(user: User, token: String? = null) {
        prefs.edit()
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_FULL_NAME, user.name)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_PHONE, user.phone)
            .putString(KEY_ROLE, user.role)
            .apply()

        if (!token.isNullOrBlank()) {
            saveAuthToken(token)
        }
    }

    fun getCurrentUser(): User? {
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        val id = prefs.getString(KEY_USER_ID, "0") ?: "0"
        val name = prefs.getString(KEY_FULL_NAME, "User") ?: "User"
        val phone = prefs.getString(KEY_PHONE, "") ?: ""
        val role = prefs.getString(KEY_ROLE, "PLAYER") ?: "PLAYER"
        return User(id = id, name = name, email = email, phone = phone, role = role)
    }

    fun isLoggedIn(): Boolean {
        return !getAuthToken().isNullOrBlank() && prefs.getString(KEY_EMAIL, null) != null
    }

    fun addRegisteredTeam(teamId: String, teamName: String) {
        val ids = getRegisteredTeamIds().toMutableSet()
        if (teamId.isNotBlank()) ids.add(teamId)
        val names = getRegisteredTeamNames().toMutableSet()
        if (teamName.isNotBlank()) names.add(teamName.trim().lowercase())
        prefs.edit()
            .putStringSet(KEY_REGISTERED_TEAM_IDS, ids)
            .putStringSet(KEY_REGISTERED_TEAM_NAMES, names)
            .apply()
    }

    fun getRegisteredTeamIds(): Set<String> {
        return prefs.getStringSet(KEY_REGISTERED_TEAM_IDS, emptySet()) ?: emptySet()
    }

    fun getRegisteredTeamNames(): Set<String> {
        return prefs.getStringSet(KEY_REGISTERED_TEAM_NAMES, emptySet()) ?: emptySet()
    }

    fun clearSession() {
        val savedServerUrl = getCustomServerUrl()
        prefs.edit().clear().apply()
        if (savedServerUrl != null) {
            saveCustomServerUrl(savedServerUrl)
        }
    }
}
