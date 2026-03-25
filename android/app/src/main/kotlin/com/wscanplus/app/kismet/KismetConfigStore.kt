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
    private val prefs: SharedPreferences? = createEncryptedPrefs(context.applicationContext)

    override fun load(): KismetConfig {
        val localPrefs = prefs ?: return KismetConfig()
        return KismetConfig(
            enabled = localPrefs.getBoolean(KEY_ENABLED, false),
            baseUrl = localPrefs.getString(KEY_BASE_URL, "") ?: "",
            apiToken = localPrefs.getString(KEY_API_TOKEN, "") ?: "",
        )
    }

    fun save(config: KismetConfig): Boolean {
        val localPrefs = prefs
        if (localPrefs == null) {
            Log.e(TAG, "EncryptedSharedPreferences unavailable; refusing to persist Kismet config")
            return false
        }
        return localPrefs
            .edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putString(KEY_BASE_URL, config.baseUrl)
            .putString(KEY_API_TOKEN, config.apiToken)
            .commit()
    }

    companion object {
        private const val TAG = "KismetConfigStore"
        private const val PREFS_NAME = "kismet_config_enc"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_TOKEN = "api_token"

        private fun createEncryptedPrefs(context: Context): SharedPreferences? =
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
                Log.e(TAG, "EncryptedSharedPreferences unavailable; refusing plaintext fallback for Kismet config", e)
                null
            }
    }
}
