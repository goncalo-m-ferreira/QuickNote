package pt.goncalomferreira.quicknote.auth

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        val cleanToken = if (token.startsWith("Bearer ", ignoreCase = true)) {
            token.substring(7).trim()
        } else {
            token.trim()
        }
        prefs.edit().putString(KEY_JWT_TOKEN, cleanToken).apply()
    }

    fun getToken(): String? {
        val token = prefs.getString(KEY_JWT_TOKEN, null)
        return if (!token.isNullOrBlank()) token else null
    }

    fun getAuthorizationHeader(): String? {
        val token = getToken()
        return if (!token.isNullOrBlank()) "Bearer $token" else null
    }

    fun saveUserEmail(email: String) {
        prefs.edit().putString(KEY_USER_EMAIL, email.trim()).apply()
    }

    fun getUserEmail(): String? {
        val email = prefs.getString(KEY_USER_EMAIL, null)
        return if (!email.isNullOrBlank()) email else null
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_JWT_TOKEN)
            .remove(KEY_USER_EMAIL)
            .apply()
    }

    companion object {
        private const val PREF_NAME = "quicknote_session"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_EMAIL = "user_email"
    }
}
