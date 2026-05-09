package com.wscanplus.core.cellular

import com.wscanplus.core.evidence.EvidenceProvenance
import com.wscanplus.core.evidence.EvidenceSourceType
import org.junit.Assert.assertEquals
import org.junit.Test

class CellObservationTest {
    @Test
    fun preserves_valid_observation() {
        val observation =
            CellObservation(
                observationId = "cell-1",
                radioType = CellularRadioType.LTE,
                mcc = "310",
                mnc = "260",
                trackingAreaCode = 123,
                cellId = 456L,
                physicalCellId = 22,
                arfcn = 5230,
                bandwidthKhz = 15000,
                signalDbm = -91,
                timestampMs = 1000L,
                provenance =
                    EvidenceProvenance(
                        sourceType = EvidenceSourceType.ANDROID_CELLULAR,
                        collectorId = "android-primary",
                        ingestionPath = "android/cellular",
                        observationTimestampMs = 1000L,
                        receivedTimestampMs = 1010L,
                    ),
            )

        assertEquals(CellularRadioType.LTE, observation.radioType)
        assertEquals("310", observation.mcc)
        assertEquals("260", observation.mnc)
        assertEquals(-91, observation.signalDbm)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_blank_observation_id() {
        CellObservation(
            observationId = " ",
            radioType = CellularRadioType.UNKNOWN,
            mcc = null,
            mnc = null,
            trackingAreaCode = null,
            cellId = null,
            physicalCellId = null,
            arfcn = null,
            bandwidthKhz = null,
            signalDbm = null,
            timestampMs = 1000L,
            provenance = null,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_invalid_mcc() {
        CellObservation(
            observationId = "cell-2",
            radioType = CellularRadioType.GSM,
            mcc = "31",
            mnc = "260",
            trackingAreaCode = null,
            cellId = null,
            physicalCellId = null,
            arfcn = null,
            bandwidthKhz = null,
            signalDbm = null,
            timestampMs = 1000L,
            provenance = null,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_invalid_mnc() {
        CellObservation(
            observationId = "cell-3",
            radioType = CellularRadioType.GSM,
            mcc = "310",
            mnc = "2",
            trackingAreaCode = null,
            cellId = null,
            physicalCellId = null,
            arfcn = null,
            bandwidthKhz = null,
            signalDbm = null,
            timestampMs = 1000L,
            provenance = null,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_negative_timestamp() {
        CellObservation(
            observationId = "cell-4",
            radioType = CellularRadioType.NR,
            mcc = null,
            mnc = null,
            trackingAreaCode = null,
            cellId = null,
            physicalCellId = null,
            arfcn = null,
            bandwidthKhz = null,
            signalDbm = null,
            timestampMs = -1L,
            provenance = null,
        )
    }
}
