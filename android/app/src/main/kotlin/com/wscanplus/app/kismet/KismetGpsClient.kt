package com.wscanplus.app.kismet

import android.util.Log
import com.wscanplus.app.location.LocationSample
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class KismetGpsClient(
    private val configStore: KismetConfigReader,
) {
    fun send(sample: LocationSample): Boolean {
        val config = configStore.load()
        if (!config.enabled) return false
        val normalizedBaseUrl = config.normalizedBaseUrl()
        if (normalizedBaseUrl.isBlank()) return false

        val body = buildJsonPayload(sample)
        val connection =
            (URL(buildEndpointUrl(normalizedBaseUrl, config.apiToken)).openConnection() as HttpURLConnection)
                .apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                }

        return try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body)
            }
            val responseCode = connection.responseCode
            responseCode in 200..299
        } catch (error: Exception) {
            Log.w(TAG, "Kismet GPS update failed", error)
            false
        } finally {
            connection.disconnect()
        }
    }

    internal fun buildJsonPayload(sample: LocationSample): String =
        buildString {
            append('{')
            append("\"lat\":")
            append(sample.latitude)
            append(",\"lon\":")
            append(sample.longitude)
            sample.altitudeMeters?.let {
                append(",\"alt\":")
                append(it)
            }
            sample.speedKph?.let {
                append(",\"spd\":")
                append(it)
            }
            append('}')
        }

    internal fun buildEndpointUrl(
        normalizedBaseUrl: String,
        apiToken: String,
    ): String =
        if (apiToken.isBlank()) {
            "$normalizedBaseUrl/gps/web/update.cmd"
        } else {
            val encodedToken = URLEncoder.encode(apiToken, Charsets.UTF_8.name())
            "$normalizedBaseUrl/gps/web/update.cmd?KISMET=$encodedToken"
        }

    companion object {
        private const val TAG = "KismetGpsClient"
    }
}
