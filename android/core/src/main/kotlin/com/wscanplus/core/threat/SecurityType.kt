package com.wscanplus.core.threat

enum class SecurityType(
    val rank: Int,
) {
    WPA3(4),
    OWE(3),
    WPA2(3),
    WPA(2),
    WEP(1),
    OPEN(0),
    ;

    companion object {
        fun parse(capabilities: String): SecurityType {
            val caps = capabilities.uppercase()
            return when {
                caps.contains("SAE") -> WPA3
                caps.contains("OWE") -> OWE
                caps.contains("RSN") || caps.contains("WPA2") -> WPA2
                caps.contains("WPA") -> WPA
                caps.contains("WEP") -> WEP
                else -> OPEN
            }
        }
    }
}
