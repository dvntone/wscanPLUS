package com.wscanplus.app.cti

/**
 * Parses CrowdSec CTI /v2/smoke response JSON.
 *
 * Intentionally minimal — the full response schema will be mapped during Phase 4
 * threat-narrative integration once the exact fields in use are verified against
 * the live API. Until then, presence of a non-blank body is the signal.
 */
object CrowdSecSmokeParser {
    /** Returns true if the response body contains any CTI signal worth recording. */
    fun hasSignal(rawJson: String): Boolean = rawJson.isNotBlank()
}
