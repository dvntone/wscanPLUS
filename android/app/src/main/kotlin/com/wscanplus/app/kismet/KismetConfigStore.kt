package com.wscanplus.app.kismet

import android.content.Context

interface KismetConfigReader {
    fun load(): KismetConfig
}

class KismetConfigStore(
    context: Context,
) : KismetConfigReader {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

    companion object {
        private const val PREFS_NAME = "kismet_config"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_TOKEN = "api_token"
    }
}
