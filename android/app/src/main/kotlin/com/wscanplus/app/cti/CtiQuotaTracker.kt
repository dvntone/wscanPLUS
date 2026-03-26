package com.wscanplus.app.cti

import android.content.Context
import java.util.Calendar

interface CtiQuotaTracker {
    fun canMakeRequest(): Boolean

    fun recordRequest()
}

/**
 * SharedPreferences-backed quota tracker enforcing [DAILY_LIMIT] CrowdSec CTI
 * requests per calendar day. Counter resets automatically when the day changes.
 */
class SharedPrefsQuotaTracker(
    context: Context,
) : CtiQuotaTracker {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun canMakeRequest(): Boolean {
        resetIfNewDay()
        return prefs.getInt(KEY_COUNT, 0) < DAILY_LIMIT
    }

    override fun recordRequest() {
        resetIfNewDay()
        val next = prefs.getInt(KEY_COUNT, 0) + 1
        prefs.edit().putInt(KEY_COUNT, next).apply()
    }

    private fun resetIfNewDay() {
        val today = todayKey()
        if (prefs.getString(KEY_DAY, null) != today) {
            prefs
                .edit()
                .putString(KEY_DAY, today)
                .putInt(KEY_COUNT, 0)
                .apply()
        }
    }

    private fun todayKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }

    companion object {
        private const val PREFS_NAME = "cti_quota"
        private const val KEY_COUNT = "request_count"
        private const val KEY_DAY = "day_key"
        const val DAILY_LIMIT = 50
    }
}
