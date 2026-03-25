package com.wscanplus.app.kismet

import android.util.Log
import com.wscanplus.app.location.LocationSample
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class KismetGpsClient(
    private val configStore: KismetConfigReader,
) {
    fun send(sample: LocationSample): Boolean {
        val config = configStore.load()
        if (!config.enabled) return false
        val normalizedBaseUrl = config.normalizedBaseUrl()
        if (normalizedBaseUrl.isBlank()) return false

        if (normalizedBaseUrl.startsWith("http://")) {
            Log.w(TAG, "Kismet base URL uses plain HTTP — token and GPS data are unencrypted in transit")
        }

        val body = buildJsonPayload(sample)
        val connection =
            (URL(buildEndpointUrl(normalizedBaseUrl)).openConnection() as HttpURLConnection)
                .apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    if (config.apiToken.isNotBlank()) {
                        setRequestProperty("Authorization", "KISMET ${config.apiToken}")
                    }
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

    internal fun buildEndpointUrl(normalizedBaseUrl: String): String = "$normalizedBaseUrl/gps/web/update.cmd"

    companion object {
        private const val TAG = "KismetGpsClient"
    }
}
