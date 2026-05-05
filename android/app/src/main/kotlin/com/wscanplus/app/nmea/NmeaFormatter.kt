package com.wscanplus.app.nmea

import com.wscanplus.app.location.LocationSample
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

private val UTC_TIME_FORMAT =
    SimpleDateFormat("HHmmss.SSS", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

private val UTC_DATE_FORMAT =
    SimpleDateFormat("ddMMyy", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

object NmeaFormatter {
    fun toSentences(sample: LocationSample): List<String> = listOf(toGga(sample), toRmc(sample))

    fun toGga(sample: LocationSample): String {
        val utcTime = UTC_TIME_FORMAT.format(Date(sample.capturedAt))
        val (lat, latHemisphere) = toNmeaCoordinate(sample.latitude, isLatitude = true)
        val (lon, lonHemisphere) = toNmeaCoordinate(sample.longitude, isLatitude = false)
        val altitude = sample.altitudeMeters?.let { formatDecimal(it, 1) } ?: ""
        val hdop = sample.accuracyMeters?.takeIf { it > 0f }?.let { formatDecimal(it.toDouble(), 1) } ?: ""
        val body =
            listOf(
                "GPGGA",
                utcTime,
                lat,
                latHemisphere,
                lon,
                lonHemisphere,
                "1",
                "",
                hdop,
                altitude,
                "M",
                "",
                "M",
                "",
                "",
            ).joinToString(",")
        return sentence(body)
    }

    fun toRmc(sample: LocationSample): String {
        val utcTime = UTC_TIME_FORMAT.format(Date(sample.capturedAt))
        val utcDate = UTC_DATE_FORMAT.format(Date(sample.capturedAt))
        val (lat, latHemisphere) = toNmeaCoordinate(sample.latitude, isLatitude = true)
        val (lon, lonHemisphere) = toNmeaCoordinate(sample.longitude, isLatitude = false)
        val speedKnots = sample.speedKph?.let { formatDecimal(it / 1.852, 1) } ?: ""
        val body =
            listOf(
                "GPRMC",
                utcTime,
                "A",
                lat,
                latHemisphere,
                lon,
                lonHemisphere,
                speedKnots,
                "",
                utcDate,
                "",
                "",
            ).joinToString(",")
        return sentence(body)
    }

    internal fun sentence(body: String): String = "\$$body*${checksum(body)}\r\n"

    internal fun checksum(body: String): String {
        var value = 0
        for (ch in body) {
            value = value xor ch.code
        }
        return value.toString(16).uppercase(Locale.US).padStart(2, '0')
    }

    internal fun toNmeaCoordinate(
        coordinate: Double,
        isLatitude: Boolean,
    ): Pair<String, String> {
        val absValue = abs(coordinate)
        val degrees = absValue.toInt()
        val minutes = (absValue - degrees) * 60.0
        val degreeWidth = if (isLatitude) 2 else 3
        val formatted =
            String.format(
                Locale.US,
                "%0${degreeWidth}d%06.3f",
                degrees,
                minutes,
            )
        val hemisphere =
            if (isLatitude) {
                if (coordinate >= 0.0) "N" else "S"
            } else {
                if (coordinate >= 0.0) "E" else "W"
            }
        return formatted to hemisphere
    }

    private fun formatDecimal(
        value: Double,
        fractionDigits: Int,
    ): String = String.format(Locale.US, "%.${fractionDigits}f", value)
}
