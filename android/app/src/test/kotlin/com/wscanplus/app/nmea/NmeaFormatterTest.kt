package com.wscanplus.app.nmea

import com.wscanplus.app.location.LocationSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NmeaFormatterTest {
    @Test
    fun `toSentences emits GGA and RMC with checksums`() {
        val sample =
            LocationSample(
                latitude = 47.6205,
                longitude = -122.3493,
                accuracyMeters = 4.4f,
                altitudeMeters = 52.7,
                speedKph = 18.5f,
                capturedAt = 1_714_600_000_000L,
                provider = "fused",
                isMock = false,
            )

        val sentences = NmeaFormatter.toSentences(sample)

        assertEquals(2, sentences.size)
        assertTrue(sentences[0].startsWith("\$GPGGA,"))
        assertTrue(sentences[0].endsWith("\r\n"))
        assertTrue(sentences[1].startsWith("\$GPRMC,"))
        assertTrue(sentences[1].endsWith("\r\n"))
        assertTrue(sentences.all { it.contains('*') })
    }

    @Test
    fun `toGga formats coordinates and altitude`() {
        val sample =
            LocationSample(
                latitude = 37.7749,
                longitude = -122.4194,
                accuracyMeters = 5.2f,
                altitudeMeters = 16.4,
                speedKph = null,
                capturedAt = 1_714_600_000_000L,
                provider = "gps",
                isMock = false,
            )

        val sentence = NmeaFormatter.toGga(sample)

        assertTrue(sentence.contains(",3746.494,N,12225.164,W,"))
        assertTrue(sentence.contains(",5.2,16.4,M,,M,,"))
    }

    @Test
    fun `toRmc converts speed to knots`() {
        val sample =
            LocationSample(
                latitude = 37.7749,
                longitude = -122.4194,
                accuracyMeters = null,
                altitudeMeters = null,
                speedKph = 18.52f,
                capturedAt = 1_714_600_000_000L,
                provider = "gps",
                isMock = false,
            )

        val sentence = NmeaFormatter.toRmc(sample)

        assertTrue(sentence.contains(",A,3746.494,N,12225.164,W,10.0,"))
    }

    @Test
    fun `checksum matches known sample`() {
        assertEquals("47", NmeaFormatter.checksum("GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,"))
    }

    @Test
    fun `toNmeaCoordinate carries minute rollover at degree boundary`() {
        // 47.9999999 raw minutes = 59.99999... → rounds to 60.000 without carry fix
        val (coord, hemi) = NmeaFormatter.toNmeaCoordinate(47.9999999, isLatitude = true)
        assertTrue("minutes field must not be 60.000, got: $coord", !coord.contains("60.000"))
        assertEquals("N", hemi)
    }
}
