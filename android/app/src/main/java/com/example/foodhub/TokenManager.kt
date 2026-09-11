package com.example.foodhub

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import android.util.Base64

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("jwt_token", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("jwt_token", null)
    }

    fun clearToken() {
        prefs.edit().remove("jwt_token").apply()
    }

    fun saveUsername(username: String) {
        prefs.edit().putString("username", username).apply()
    }

    fun getUsername(): String? {
        return prefs.getString("username", null)
    }

    fun clearUsername() {
        prefs.edit().remove("username").apply()
    }

    fun getUserIdFromToken(token: String): String? {
        try {
            val parts = token.split(".")
            if (parts.size < 2) return null

            val payload = String(Base64.decode(parts[1],Base64.URL_SAFE))
            val json = JSONObject(payload)

            return json.getString("userid")
        } catch (e: Exception) {
            return null
        }
    }
}