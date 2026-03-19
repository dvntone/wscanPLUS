package com.wscanplus.core.threat

class OuiLookup(
    private val ouiMap: Map<String, String>,
) {
    private val suspiciousOuis =
        setOf(
            "B827EB",
            "DCA632",
            "E45F01",
            "2CCF67",
            "D83ADD",
            "240AC4",
            "30AEA4",
            "A4CF12",
            "CC50E3",
            "0CFA22",
            "00C0CA",
        )

    fun lookup(bssid: String): String? {
        val prefix = normalizePrefix(bssid) ?: return null
        return ouiMap[prefix]
    }

    fun isSuspiciousVendor(bssid: String): Boolean {
        val prefix = normalizePrefix(bssid) ?: return false
        return prefix in suspiciousOuis
    }

    fun isLocallyAdministered(bssid: String): Boolean {
        val normalized = normalizeBssid(bssid) ?: return false
        if (normalized.length < 2) return false
        val firstOctet = normalized.substring(0, 2).toIntOrNull(16) ?: return false
        return (firstOctet and 0x02) != 0
    }

    private fun normalizePrefix(bssid: String): String? {
        val normalized = normalizeBssid(bssid) ?: return null
        if (normalized.length < 6) return null
        return normalized.substring(0, 6)
    }

    private fun normalizeBssid(bssid: String): String? {
        val cleaned =
            bssid
                .trim()
                .uppercase()
                .replace(":", "")
                .replace("-", "")
        if (cleaned.length < 6) return null
        if (cleaned.any { it !in '0'..'9' && it !in 'A'..'F' }) return null
        return cleaned
    }
}
