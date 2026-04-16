package com.wscanplus.app

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Typeface
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiagnosticActivity : Activity() {
    private var boundBinder: WatchdogService.LocalBinder? = null
    private var serviceBound = false
    private lateinit var diagnosticText: TextView

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                binder: IBinder?,
            ) {
                boundBinder = binder as? WatchdogService.LocalBinder
                refreshDiagnostics()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                boundBinder = null
                refreshDiagnostics()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 48, 48, 48)
            }
        val title =
            TextView(this).apply {
                text = "WatchdogService Diagnostics"
                textSize = 18f
            }
        val refreshButton =
            Button(this).apply {
                text = "Refresh"
                setOnClickListener { refreshDiagnostics() }
            }
        diagnosticText =
            TextView(this).apply {
                textSize = 13f
                typeface = Typeface.MONOSPACE
                text = "Connecting…"
            }
        val scroll = ScrollView(this)
        scroll.addView(diagnosticText)

        root.addView(title)
        root.addView(refreshButton)
        root.addView(scroll)
        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        serviceBound = bindService(Intent(this, WatchdogService::class.java), serviceConnection, 0)
        if (!serviceBound) {
            refreshDiagnostics()
        }
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        boundBinder = null
    }

    private fun refreshDiagnostics() {
        val binder = boundBinder
        if (binder == null) {
            diagnosticText.text = "Service: NOT RUNNING"
            return
        }

        val svc = binder.getService()
        val sb = StringBuilder()

        sb.append("Service: RUNNING")
        if (binder.isDegraded) sb.append(" (degraded)")
        sb.append("\n\n")

        sb.append("── Capabilities ──\n")
        val caps = binder.capabilityManifest
        if (caps != null) {
            sb.append("Device:       ${caps.deviceModel}\n")
            sb.append("WiFi scan:    ${caps.wifiScan}\n")
            sb.append("WiFi RTT:     ${caps.wifiRtt}  now=${caps.wifiRttAvailableNow}\n")
            sb.append("WiFi Aware:   ${caps.wifiAware}\n")
            sb.append("UWB:          ${caps.uwb}\n")
            sb.append("Barometer:    ${caps.barometer}\n")
            sb.append("BLE:          ${caps.bluetoothLe}\n")
            sb.append("Camera IR:    ${caps.cameraIrCapable}\n")
            sb.append("Acoustic:     ${caps.acousticSonarCapable}\n")
        } else {
            sb.append("Not probed yet\n")
        }

        sb.append("\n── Floor Estimate ──\n")
        val fe = svc.currentFloorEstimate
        if (fe != null) {
            sb.append("Relative floor:  ${fe.relativeFloor}\n")
            sb.append("Delta hPa:       ${"%.2f".format(fe.deltaHpa)}\n")
            sb.append("Confidence:      ${"%.1f".format(fe.confidenceMeters)} m\n")
            sb.append("observedAt:      ${formatMs(fe.observedAt)}\n")
        } else {
            sb.append("Absent\n")
        }

        sb.append("\n── Desktop Link ──\n")
        val ackSeq = svc.lastDesktopAckSeq
        if (ackSeq == -1) {
            sb.append("No ack received (desktop not connected)\n")
        } else {
            sb.append("Last ack seq: $ackSeq\n")
        }

        diagnosticText.text = sb.toString()
    }

    private fun formatMs(epochMs: Long): String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(epochMs))

    companion object {
        @Suppress("unused")
        private const val TAG = "DiagnosticActivity"
    }
}
