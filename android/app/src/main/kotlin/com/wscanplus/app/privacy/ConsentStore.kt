package com.wscanplus.app.privacy

import android.content.Context
import android.content.SharedPreferences

interface ConsentReader {
    fun isConsentGiven(): Boolean
}

class ConsentStore(
    context: Context,
) : ConsentReader {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    override fun isConsentGiven(): Boolean = prefs.getBoolean(KEY_CONSENT, false)

    fun setConsentGiven(given: Boolean) {
        prefs.edit().putBoolean(KEY_CONSENT, given).apply()
    }

    companion object {
        private const val PREFS_NAME = "privacy_consent"
        private const val KEY_CONSENT = "consent_given"
    }
}
