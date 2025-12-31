package com.rtsp.cctv.data

import android.content.Context

class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("token", token).apply()
    }

    fun getToken(): String? = prefs.getString("token", null)

    fun hasToken(): Boolean = !getToken().isNullOrEmpty()

    fun clear() {
        prefs.edit().remove("token").apply()
    }
}
