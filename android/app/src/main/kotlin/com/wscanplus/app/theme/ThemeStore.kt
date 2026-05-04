package com.wscanplus.app.theme

import android.content.Context

class ThemeStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(): ThemePreference =
        prefs.getString(KEY_THEME, null)?.let(::decodePreference) ?: ThemePreference.SYSTEM

    fun write(preference: ThemePreference) {
        prefs.edit().putString(KEY_THEME, preference.name).apply()
    }

    private fun decodePreference(raw: String): ThemePreference =
        ThemePreference.entries.firstOrNull { it.name == raw } ?: ThemePreference.SYSTEM

    private companion object {
        private const val PREFS_NAME = "wscan_theme"
        private const val KEY_THEME = "theme_preference"
    }
}

