package com.wscanplus.app.collection.cell

data class CellObservation(
    val mcc: Int?,
    val mnc: Int?,
    val tac: Int?,
    val ci: Long?,
    val pci: Int?,
    val type: CellType,
    val rsrp: Int?,
    val rsrq: Int?,
    val sinr: Int?,
    val observedAtMs: Long,
)

enum class CellType { LTE, NR, GSM, WCDMA, UNKNOWN }
