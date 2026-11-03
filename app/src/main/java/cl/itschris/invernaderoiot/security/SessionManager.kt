package cl.itschris.invernaderoiot.security

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor de Sesiones de Usuario.
 * Permite mantener la sesión activa de forma segura y controlar el acceso a las funciones IoT.
 * Cumple con ISO 27400: Control de Acceso y Gestión de Identidades en entornos OT.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "InvernaderoIoTSession"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_ROLE = "role"
    }

    fun saveUserSession(username: String, fullName: String, role: String) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USERNAME, username)
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getCurrentUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    fun getCurrentFullName(): String? {
        return prefs.getString(KEY_FULL_NAME, "Agricultor")
    }

    fun getCurrentRole(): String? {
        return prefs.getString(KEY_ROLE, "Operador")
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
