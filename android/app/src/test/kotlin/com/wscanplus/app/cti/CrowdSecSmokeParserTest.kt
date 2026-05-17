package com.wscanplus.app.cti

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrowdSecSmokeParserTest {
    @Test
    fun `blank json returns no signal`() {
        val result = CrowdSecSmokeParser.parse("")
        assertFalse(result.hasSignal)
        assertNull(result.confidence)
        assertTrue(result.reasons().isEmpty())
    }

    @Test
    fun `malformed json returns no signal`() {
        val result = CrowdSecSmokeParser.parse("not json at all")
        assertFalse(result.hasSignal)
        assertNull(result.confidence)
    }

    @Test
    fun `zero scores return no signal`() {
        val json = """{"aggressive_score":0,"background_noise_score":0}"""
        val result = CrowdSecSmokeParser.parse(json)
        assertFalse(result.hasSignal)
        assertNull(result.confidence)
    }

    @Test
    fun `aggressive score only produces signal`() {
        val json = """{"aggressive_score":80}"""
        val result = CrowdSecSmokeParser.parse(json)
        assertTrue(result.hasSignal)
        assertEquals(80, result.aggressiveScore)
        assertNull(result.backgroundNoiseScore)
        assertEquals(0.80f, result.confidence!!, 0.001f)
        assertEquals(1, result.reasons().size)
        assertTrue(result.reasons()[0].contains("aggressive score: 80"))
    }

    @Test
    fun `background noise score only produces signal`() {
        val json = """{"background_noise_score":50}"""
        val result = CrowdSecSmokeParser.parse(json)
        assertTrue(result.hasSignal)
        assertEquals(50, result.backgroundNoiseScore)
        assertEquals(0.50f, result.confidence!!, 0.001f)
    }

    @Test
    fun `confidence uses higher of two scores`() {
        val json = """{"aggressive_score":40,"background_noise_score":70}"""
        val result = CrowdSecSmokeParser.parse(json)
        assertEquals(0.70f, result.confidence!!, 0.001f)
    }

    @Test
    fun `confidence capped at 0_9`() {
        val json = """{"aggressive_score":100}"""
        val result = CrowdSecSmokeParser.parse(json)
        assertEquals(0.90f, result.confidence!!, 0.001f)
    }

    @Test
    fun `reasons contains both scores when both present`() {
        val json = """{"aggressive_score":60,"background_noise_score":30}"""
        val result = CrowdSecSmokeParser.parse(json)
        val reasons = result.reasons()
        assertEquals(2, reasons.size)
        assertTrue(reasons[0].contains("aggressive score: 60"))
        assertTrue(reasons[1].contains("background noise score: 30"))
    }

    @Test
    fun `extra json fields are ignored`() {
        val json = """{"ip":"1.2.3.4","aggressive_score":55,"unknown_field":"ignored"}"""
        val result = CrowdSecSmokeParser.parse(json)
        assertTrue(result.hasSignal)
        assertEquals(55, result.aggressiveScore)
    }

    @Test
    fun `missing score fields treated as absent`() {
        val json = """{"ip":"1.2.3.4","classifications":[]}"""
        val result = CrowdSecSmokeParser.parse(json)
        assertFalse(result.hasSignal)
        assertNull(result.aggressiveScore)
        assertNull(result.backgroundNoiseScore)
    }
}
