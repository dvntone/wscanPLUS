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
}
