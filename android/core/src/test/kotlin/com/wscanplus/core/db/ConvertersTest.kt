package com.wscanplus.core.db

import com.wscanplus.core.threat.EnvironmentType
import com.wscanplus.core.threat.HeuristicType
import com.wscanplus.core.threat.ThreatSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {
    private val converters =
        Converters()

    @Test
    fun `string set round trip`() {
        val input = setOf("a", "b", "c")
        val encoded = converters.fromStringSet(input)
        val decoded = converters.toStringSet(encoded)
        assertEquals(input, decoded)
    }

    @Test
    fun `string list round trip`() {
        val input = listOf("one", "two", "three")
        val encoded = converters.fromStringList(input)
        val decoded = converters.toStringList(encoded)
        assertEquals(input, decoded)
    }

    @Test
    fun `threat source round trip`() {
        val encoded = converters.fromThreatSource(ThreatSource.GEMINI)
        val decoded = converters.toThreatSource(encoded)
        assertEquals(ThreatSource.GEMINI, decoded)
    }

    @Test
    fun `heuristic type round trip`() {
        val encoded = converters.fromHeuristicType(HeuristicType.SSID_FLOODING)
        val decoded = converters.toHeuristicType(encoded)
        assertEquals(HeuristicType.SSID_FLOODING, decoded)
    }

    @Test
    fun `heuristic type handles null`() {
        val encoded = converters.fromHeuristicType(null)
        val decoded = converters.toHeuristicType(encoded)
        assertNull(decoded)
    }

    @Test
    fun `environment type round trip`() {
        val encoded = converters.fromEnvironmentType(EnvironmentType.OFFICE)
        val decoded = converters.toEnvironmentType(encoded)
        assertEquals(EnvironmentType.OFFICE, decoded)
    }

    @Test
    fun `environment type unknown string decodes to UNKNOWN`() {
        assertEquals(EnvironmentType.UNKNOWN, converters.toEnvironmentType("LEGACY_VALUE"))
    }

    @Test
    fun `environment type blank string decodes to UNKNOWN`() {
        assertEquals(EnvironmentType.UNKNOWN, converters.toEnvironmentType(""))
    }
}
