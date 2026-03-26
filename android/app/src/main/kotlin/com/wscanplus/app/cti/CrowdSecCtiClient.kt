package com.wscanplus.app.cti

import android.util.Log
import com.wscanplus.app.privacy.ConsentReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * CrowdSec CTI client for /v2/smoke IP reputation lookups.
 *
 * - All network I/O is dispatched to Dispatchers.IO — callers on any dispatcher are safe.
 * - Returns null on consent absent, missing key, non-2xx response, or I/O failure.
 * - Quota: 50 requests/day on the free tier. Callers must apply cache + quota guards
 *   before invoking lookupSmoke() to stay within budget.
 */
class CrowdSecCtiClient(
    private val keyProvider: CrowdSecCtiKeyProvider,
    private val consentReader: ConsentReader,
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val baseUrl: String = BASE_URL,
) : CtiClient {
    override suspend fun lookupSmoke(ip: String): CrowdSecSmokeResult? =
        withContext(Dispatchers.IO) {
            if (!consentReader.isConsentGiven()) {
                Log.w(TAG, "CrowdSec CTI blocked: user consent not granted")
                return@withContext null
            }

            val apiKey = keyProvider.getApiKey().trim()
            if (apiKey.isBlank() || apiKey == "REPLACE_ME") {
                Log.w(TAG, "CrowdSec CTI blocked: API key not configured")
                return@withContext null
            }

            val url =
                baseUrl
                    .toHttpUrl()
                    .newBuilder()
                    .addPathSegment("v2")
                    .addPathSegment("smoke")
                    .addPathSegment(ip)
                    .build()

            val requestBuilder = Request.Builder()
            requestBuilder.url(url)
            requestBuilder.get()
            requestBuilder.addHeader("x-api-key", apiKey)
            val request = requestBuilder.build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "CrowdSec CTI lookup failed: HTTP ${response.code} for $ip")
                        return@withContext null
                    }
                    val body = response.body?.string()
                    if (body.isNullOrBlank()) {
                        Log.w(TAG, "CrowdSec CTI lookup returned empty body for $ip")
                        return@withContext null
                    }
                    CrowdSecSmokeResult(ip = ip, rawJson = body)
                }
            } catch (e: IOException) {
                Log.w(TAG, "CrowdSec CTI lookup failed for $ip", e)
                null
            }
        }

    companion object {
        private const val TAG = "CrowdSecCtiClient"
        private const val BASE_URL = "https://cti.api.crowdsec.net"
    }
}
