package com.wscanplus.app.collection.cell

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

class CellCollector(
    private val telephonyManager: TelephonyManager,
    private val context: Context,
) {
    fun collect(): List<CellObservation> =
        try {
            collectInternal()
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied collecting cell info: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to collect cell info: ${e.message}")
            emptyList()
        }

    @SuppressLint("MissingPermission")
    private fun collectInternal(): List<CellObservation> {
        val hasFine = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val hasReadPhoneState = hasPermission(Manifest.permission.READ_PHONE_STATE)
        if (!hasReadPhoneStateRequiredForCellInfo(Build.VERSION.SDK_INT, hasFine, hasReadPhoneState)) {
            return emptyList()
        }

        @Suppress("DEPRECATION")
        val cellInfoList = telephonyManager.allCellInfo ?: return emptyList()
        val now = System.currentTimeMillis()

        return cellInfoList.mapNotNull { cellInfo ->
            when {
                cellInfo is CellInfoLte -> mapLte(cellInfo, now)
                cellInfo is CellInfoGsm -> mapGsm(cellInfo, now)
                cellInfo is CellInfoWcdma -> mapWcdma(cellInfo, now)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> mapNr(cellInfo, now)
                else -> null
            }
        }
    }

    private fun mapLte(
        cellInfo: CellInfoLte,
        now: Long,
    ): CellObservation {
        val id = cellInfo.cellIdentity
        val sig = cellInfo.cellSignalStrength
        return CellObservation(
            mcc =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    id.mccString?.toIntOrNull()
                } else {
                    @Suppress("DEPRECATION")
                    id.mcc.takeUnless { it == Int.MAX_VALUE }
                },
            mnc =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    id.mncString?.toIntOrNull()
                } else {
                    @Suppress("DEPRECATION")
                    id.mnc.takeUnless { it == Int.MAX_VALUE }
                },
            tac = id.tac.takeUnless { it == Int.MAX_VALUE },
            ci = id.ci.takeUnless { it == Int.MAX_VALUE }?.toLong(),
            pci = id.pci.takeUnless { it == Int.MAX_VALUE },
            type = CellType.LTE,
            rsrp = sig.rsrp.takeUnless { it == Int.MAX_VALUE },
            rsrq = sig.rsrq.takeUnless { it == Int.MAX_VALUE },
            sinr = sig.rssnr.takeUnless { it == Int.MAX_VALUE },
            observedAtMs = now,
        )
    }

    private fun mapGsm(
        cellInfo: CellInfoGsm,
        now: Long,
    ): CellObservation {
        val id = cellInfo.cellIdentity
        return CellObservation(
            mcc =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    id.mccString?.toIntOrNull()
                } else {
                    @Suppress("DEPRECATION")
                    id.mcc.takeUnless { it == Int.MAX_VALUE }
                },
            mnc =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    id.mncString?.toIntOrNull()
                } else {
                    @Suppress("DEPRECATION")
                    id.mnc.takeUnless { it == Int.MAX_VALUE }
                },
            tac = null,
            ci = id.cid.takeUnless { it == Int.MAX_VALUE }?.toLong(),
            pci = null,
            type = CellType.GSM,
            rsrp = null,
            rsrq = null,
            sinr = null,
            observedAtMs = now,
        )
    }

    private fun mapWcdma(
        cellInfo: CellInfoWcdma,
        now: Long,
    ): CellObservation {
        val id = cellInfo.cellIdentity
        return CellObservation(
            mcc =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    id.mccString?.toIntOrNull()
                } else {
                    @Suppress("DEPRECATION")
                    id.mcc.takeUnless { it == Int.MAX_VALUE }
                },
            mnc =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    id.mncString?.toIntOrNull()
                } else {
                    @Suppress("DEPRECATION")
                    id.mnc.takeUnless { it == Int.MAX_VALUE }
                },
            tac = null,
            ci = id.cid.takeUnless { it == Int.MAX_VALUE }?.toLong(),
            pci = id.psc.takeUnless { it == Int.MAX_VALUE },
            type = CellType.WCDMA,
            rsrp = null,
            rsrq = null,
            sinr = null,
            observedAtMs = now,
        )
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val TAG = "CellCollector"

        internal fun hasReadPhoneStateRequiredForCellInfo(
            sdkInt: Int,
            hasFine: Boolean,
            hasReadPhoneState: Boolean,
        ): Boolean {
            if (!hasFine) return false
            return if (sdkInt < Build.VERSION_CODES.Q) true else hasReadPhoneState
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun mapNr(
    cellInfo: android.telephony.CellInfo,
    now: Long,
): CellObservation? {
    val nrInfo = cellInfo as? android.telephony.CellInfoNr ?: return null
    val id = nrInfo.cellIdentity as? android.telephony.CellIdentityNr ?: return null
    val sig = nrInfo.cellSignalStrength as? android.telephony.CellSignalStrengthNr ?: return null
    return CellObservation(
        mcc = id.mccString?.toIntOrNull(),
        mnc = id.mncString?.toIntOrNull(),
        tac = id.tac.takeUnless { it == Int.MAX_VALUE },
        ci = id.nci.takeUnless { it == Long.MAX_VALUE },
        pci = id.pci.takeUnless { it == Int.MAX_VALUE },
        type = CellType.NR,
        rsrp = sig.ssRsrp.takeUnless { it == Int.MAX_VALUE },
        rsrq = sig.ssRsrq.takeUnless { it == Int.MAX_VALUE },
        sinr = sig.ssSinr.takeUnless { it == Int.MAX_VALUE },
        observedAtMs = now,
    )
}
