package com.wscanplus.app.cti

import com.wscanplus.app.privacy.ConsentReader
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class CrowdSecCtiClientTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `returns null when consent not given`() =
        runTest {
            val client = makeClient(consentGiven = false)
            assertNull(client.lookupSmoke("1.2.3.4"))
        }

    @Test
    fun `returns null when API key is blank`() =
        runTest {
            val client = makeClient(apiKey = "")
            assertNull(client.lookupSmoke("1.2.3.4"))
        }

    @Test
    fun `returns null when API key is placeholder`() =
        runTest {
            val client = makeClient(apiKey = "REPLACE_ME")
            assertNull(client.lookupSmoke("1.2.3.4"))
        }

    @Test
    fun `returns null on non-2xx response`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(403))
            assertNull(makeClient().lookupSmoke("1.2.3.4"))
        }

    @Test
    fun `returns null on empty body`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody(""))
            assertNull(makeClient().lookupSmoke("1.2.3.4"))
        }

    @Test
    fun `returns result on 200 with non-empty body`() =
        runTest {
            val fakeJson = """{"ip":"1.2.3.4","scores":{}}"""
            server.enqueue(MockResponse().setResponseCode(200).setBody(fakeJson))

            val result = makeClient().lookupSmoke("1.2.3.4")

            assertNotNull(result)
            assertEquals("1.2.3.4", result!!.ip)
            assertEquals(fakeJson, result.rawJson)
        }

    @Test
    fun `request includes x-api-key header`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
            makeClient(apiKey = "my-secret-key").lookupSmoke("1.2.3.4")

            val recorded = checkNotNull(server.takeRequest(5, TimeUnit.SECONDS))
            assertEquals("my-secret-key", recorded.getHeader("x-api-key"))
        }

    @Test
    fun `request targets correct path`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
            makeClient().lookupSmoke("5.6.7.8")

            val recorded = checkNotNull(server.takeRequest(5, TimeUnit.SECONDS))
            assertEquals("/v2/smoke/5.6.7.8", recorded.path)
        }

    // ── helpers ─────────────────────────────────────────────────────────────

    private fun makeClient(
        apiKey: String = "test-api-key",
        consentGiven: Boolean = true,
    ): CrowdSecCtiClient =
        CrowdSecCtiClient(
            keyProvider = FakeKeyProvider(apiKey),
            consentReader = FakeConsent(consentGiven),
            httpClient = OkHttpClient(),
            baseUrl = server.url("/").toString().trimEnd('/'),
        )

    private class FakeKeyProvider(
        private val key: String,
    ) : CrowdSecCtiKeyProvider {
        override fun getApiKey(): String = key
    }

    private class FakeConsent(
        private val given: Boolean,
    ) : ConsentReader {
        override fun isConsentGiven(): Boolean = given
    }
}
