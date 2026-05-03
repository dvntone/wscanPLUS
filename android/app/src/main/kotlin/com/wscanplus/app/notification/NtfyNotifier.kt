package com.wscanplus.app.notification

import android.content.Context
import android.content.Intent

class NtfyNotifier(
    private val context: Context,
) {
    fun notify(
        title: String,
        message: String,
        priority: String = "high",
    ) {
        val topic = loadTopic() ?: return
        val intent =
            Intent("io.heckel.ntfy.SEND_MESSAGE").apply {
                putExtra("topic", topic)
                putExtra("title", title)
                putExtra("message", message)
                putExtra("priority", priority)
                putExtra("tags", "warning")
            }
        context.sendBroadcast(intent)
    }

    fun loadTopic(): String? =
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOPIC, null)

    fun saveTopic(topic: String) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOPIC, topic)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "wscanplus_ntfy"
        private const val KEY_TOPIC = "topic"
    }
}
