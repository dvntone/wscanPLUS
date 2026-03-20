package com.wscanplus.app.kismet

data class KismetConfig(
    val enabled: Boolean = false,
    val baseUrl: String = "",
    val apiToken: String = "",
) {
    fun normalizedBaseUrl(): String = baseUrl.trim().trimEnd('/')
}
