package com.wscanplus.app.cti

/** Abstraction over the CrowdSec CTI network layer — allows fake injection in tests. */
interface CtiClient {
    suspend fun lookupSmoke(ip: String): CrowdSecSmokeResult?
}
