package com.example.foodhub

import android.content.Context
import android.content.SharedPreferences
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ApiQuotaManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("api_quota_prefs", Context.MODE_PRIVATE)

    private val utcDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun getTodayUtcString(): String {
        return utcDateFormatter.format(Date())
    }

    private fun isDataValidToday(): Boolean {
        val savedDate = prefs.getString("LAST_UPDATE_DATE", "")
        return savedDate == getTodayUtcString()
    }

    fun saveQuota(requestPoints: Float, usedPoints: Float, leftPoints: Float) {
        prefs.edit().apply {
            putFloat("QUOTA_REQUEST", requestPoints)
            putFloat("QUOTA_USED", usedPoints)
            putFloat("QUOTA_LEFT", leftPoints)
            putString("LAST_UPDATE_DATE", getTodayUtcString())
            apply()
        }
    }

    fun getQuotaRequest(): Float {
        return if (isDataValidToday()) prefs.getFloat("QUOTA_REQUEST", 0f) else 0f
    }

    fun getQuotaUsed(): Float {
        return if (isDataValidToday()) prefs.getFloat("QUOTA_USED", 0f) else 0f
    }

    fun getQuotaLeft(): Float {
        return if (isDataValidToday()) prefs.getFloat("QUOTA_LEFT", -1f) else 50f
    }
}
