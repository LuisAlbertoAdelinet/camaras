package com.rtsp.cctv.data

import android.content.Context
import com.rtsp.cctv.network.ApiClientSingleton

class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("token", token).apply()
    }

    fun getToken(): String? = prefs.getString("token", null)

    fun hasToken(): Boolean = !getToken().isNullOrEmpty()

    fun clear() {
        prefs.edit().remove("token").apply()
        // Also clear ApiClient singleton to force recreation with new token state
        ApiClientSingleton.clear()
    }

    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", false)

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }
}
