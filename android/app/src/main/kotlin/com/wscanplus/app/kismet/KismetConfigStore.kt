package com.wscanplus.app.kismet

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

interface KismetConfigReader {
    fun load(): KismetConfig
}

class KismetConfigStore(
    context: Context,
) : KismetConfigReader {
    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    companion object {
        private const val TAG = "KismetConfigStore"
        private const val PREFS_NAME = "kismet_config_enc"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_TOKEN = "api_token"

        private fun createEncryptedPrefs(context: Context): SharedPreferences =
            try {
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            } catch (e: Exception) {
                Log.e(TAG, "EncryptedSharedPreferences unavailable; Kismet config will not persist", e)
                context.getSharedPreferences(PREFS_NAME + "_fallback", Context.MODE_PRIVATE)
            }
    }

    override fun load(): KismetConfig =
        KismetConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            baseUrl = prefs.getString(KEY_BASE_URL, "") ?: "",
            apiToken = prefs.getString(KEY_API_TOKEN, "") ?: "",
        )

    fun save(config: KismetConfig) {
        prefs
            .edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putString(KEY_BASE_URL, config.baseUrl)
            .putString(KEY_API_TOKEN, config.apiToken)
            .apply()
    }

}
