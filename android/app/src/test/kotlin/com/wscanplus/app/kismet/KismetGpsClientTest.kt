package com.wscanplus.app.kismet

import com.wscanplus.app.location.LocationSample
import org.junit.Assert.assertEquals
import org.junit.Test

class KismetGpsClientTest {
    @Test
    fun `buildJsonPayload omits optional fields when absent`() {
        val client = KismetGpsClient(FakeStore())

        val payload =
            client.buildJsonPayload(
                LocationSample(
                    latitude = 1.23,
                    longitude = 4.56,
                    accuracyMeters = null,
                    altitudeMeters = null,
                    speedKph = null,
                    capturedAt = 1L,
                    provider = "fused",
                    isMock = false,
                ),
            )

        assertEquals("{\"lat\":1.23,\"lon\":4.56}", payload)
    }

    @Test
    fun `buildJsonPayload includes altitude and speed when present`() {
        val client = KismetGpsClient(FakeStore())

        val payload =
            client.buildJsonPayload(
                LocationSample(
                    latitude = 1.23,
                    longitude = 4.56,
                    accuracyMeters = 5f,
                    altitudeMeters = 7.89,
                    speedKph = 12.3f,
                    capturedAt = 1L,
                    provider = "fused",
                    isMock = false,
                ),
            )

        assertEquals("{\"lat\":1.23,\"lon\":4.56,\"alt\":7.89,\"spd\":12.3}", payload)
    }

    @Test
    fun `buildEndpointUrl appends kismet token when present`() {
        val client = KismetGpsClient(FakeStore())

        val endpoint = client.buildEndpointUrl("http://127.0.0.1:2501", "abc 123")

        assertEquals("http://127.0.0.1:2501/gps/web/update.cmd?KISMET=abc+123", endpoint)
    }

    private class FakeStore : KismetConfigReader {
        override fun load(): KismetConfig = KismetConfig()
    }
}
