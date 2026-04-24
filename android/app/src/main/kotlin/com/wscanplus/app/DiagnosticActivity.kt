package com.wscanplus.app

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.LinearLayout
import android.widget.ScrollView
import com.wscanplus.app.capabilities.AcousticStatus
import com.wscanplus.app.capabilities.CameraIrStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiagnosticActivity : Activity() {
    private var boundBinder: WatchdogService.LocalBinder? = null
    private var serviceBound = false
    private lateinit var content: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private var lastRefreshedAt: Long = 0L

    private val refreshRunnable: Runnable =
        object : Runnable {
            override fun run() {
                refreshDiagnostics()
                handler.postDelayed(this, REFRESH_INTERVAL_MS)
            }
        }

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                binder: IBinder?,
            ) {
                boundBinder = binder as? WatchdogService.LocalBinder
                refreshDiagnostics()
                scheduleRefresh()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                boundBinder = null
                serviceBound = false
                handler.removeCallbacks(refreshRunnable)
                refreshDiagnostics()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = WscanUi.shell(this)
        WscanUi.header(root, "Diagnostics", "Runtime scanner, capability, floor, and desktop-link state")
        val scroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )
        setContentView(root)
        renderServicePlaceholder("CONNECTING", "Connecting to the scanner service…", WscanUi.COLOR_WARN)
    }

    override fun onStart() {
        super.onStart()
        serviceBound = bindService(Intent(this, WatchdogService::class.java), serviceConnection, 0)
        refreshDiagnostics()
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(refreshRunnable)
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        boundBinder = null
    }

    private fun scheduleRefresh() {
        handler.removeCallbacks(refreshRunnable)
        handler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS)
    }

    private fun refreshDiagnostics() {
        if (!serviceBound) {
            serviceBound = bindService(Intent(this, WatchdogService::class.java), serviceConnection, 0)
        }
        lastRefreshedAt = System.currentTimeMillis()
        content.removeAllViews()
        val binder = boundBinder
        if (binder == null) {
            if (serviceBound) {
                renderServicePlaceholder("CONNECTING", "Waiting for WatchdogService binder callback…", WscanUi.COLOR_WARN)
            } else {
                renderServicePlaceholder("NOT RUNNING", "Start scanning from the main screen, then return here.", WscanUi.COLOR_BAD)
            }
            return
        }

        val svc = binder.getService()
        val serviceCard = WscanUi.card(content)
        WscanUi.sectionTitle(serviceCard, "Service")
        WscanUi.metricRow(
            serviceCard,
            "WatchdogService",
            if (binder.isDegraded) "RUNNING · DEGRADED" else "RUNNING",
            if (binder.isDegraded) WscanUi.COLOR_WARN else WscanUi.COLOR_OK,
        )
        WscanUi.metricRow(serviceCard, "Last refreshed", formatMs(lastRefreshedAt), WscanUi.COLOR_MUTED)
        WscanUi.actionButton(serviceCard, "Refresh Diagnostics") { refreshDiagnostics() }

        val capsCard = WscanUi.card(content)
        WscanUi.sectionTitle(capsCard, "Capabilities")
        val caps = binder.capabilityManifest
        if (caps != null) {
            WscanUi.metricRow(capsCard, "Device", caps.deviceModel)
            WscanUi.metricRow(capsCard, "Wi-Fi scan", caps.wifiScan.toReadyLabel(), WscanUi.statusColor(caps.wifiScan))
            WscanUi.metricRow(
                capsCard,
                "Wi-Fi RTT",
                "${caps.wifiRtt.toReadyLabel()} · now=${caps.wifiRttAvailableNow}",
                WscanUi.statusColor(caps.wifiRtt),
            )
            WscanUi.metricRow(capsCard, "Wi-Fi Aware", caps.wifiAware.toReadyLabel(), WscanUi.statusColor(caps.wifiAware))
            WscanUi.metricRow(capsCard, "UWB", caps.uwb.toReadyLabel(), WscanUi.statusColor(caps.uwb))
            WscanUi.metricRow(capsCard, "Barometer", caps.barometer.toReadyLabel(), WscanUi.statusColor(caps.barometer))
            WscanUi.metricRow(capsCard, "BLE", caps.bluetoothLe.toReadyLabel(), WscanUi.statusColor(caps.bluetoothLe))
            WscanUi.metricRow(capsCard, "Camera depth/IR", caps.cameraIrCapable.toDisplayLabel(), caps.cameraIrCapable.statusColor())
            WscanUi.metricRow(capsCard, "Acoustic", caps.acousticSonarCapable.toDisplayLabel(), caps.acousticSonarCapable.statusColor())
        } else {
            WscanUi.body(capsCard, "Capability probe has not completed yet.", muted = true)
        }

        val floorCard = WscanUi.card(content)
        WscanUi.sectionTitle(floorCard, "Floor estimate")
        val fe = svc.currentFloorEstimate
        if (fe != null) {
            WscanUi.metricRow(floorCard, "Relative floor", fe.relativeFloor.toString())
            WscanUi.metricRow(floorCard, "Delta hPa", "%.2f".format(fe.deltaHpa))
            WscanUi.metricRow(floorCard, "Confidence", "%.1f m".format(fe.confidenceMeters))
            WscanUi.metricRow(floorCard, "Observed", formatMs(fe.observedAt))
        } else {
            WscanUi.body(floorCard, "No barometer-backed floor estimate available yet.", muted = true)
        }

        val linkCard = WscanUi.card(content)
        WscanUi.sectionTitle(linkCard, "Desktop link")
        val ackSeq = svc.lastDesktopAckSeq
        if (ackSeq == -1) {
            WscanUi.metricRow(linkCard, "ADB desktop", "NO ACK", WscanUi.COLOR_WARN)
            WscanUi.body(linkCard, "Desktop is not connected or has not acknowledged this device.", muted = true)
        } else {
            WscanUi.metricRow(linkCard, "Last ack seq", ackSeq.toString(), WscanUi.COLOR_OK)
        }
    }

    private fun renderServicePlaceholder(
        state: String,
        message: String,
        color: Int,
    ) {
        content.removeAllViews()
        val card = WscanUi.card(content)
        WscanUi.sectionTitle(card, "Service")
        WscanUi.metricRow(card, "WatchdogService", state, color)
        WscanUi.metricRow(card, "Last refreshed", formatMs(lastRefreshedAt), WscanUi.COLOR_MUTED)
        WscanUi.body(card, message, muted = true)
        WscanUi.actionButton(card, "Refresh Diagnostics") { refreshDiagnostics() }
    }

    private fun Boolean.toReadyLabel(): String = if (this) "READY" else "UNAVAILABLE"

    private fun CameraIrStatus.statusColor(): Int =
        when (this) {
            CameraIrStatus.CAPABLE -> WscanUi.COLOR_OK
            CameraIrStatus.PARTIAL,
            CameraIrStatus.UNTESTED,
            -> WscanUi.COLOR_WARN
            CameraIrStatus.NOT_CAPABLE -> WscanUi.COLOR_BAD
        }

    private fun AcousticStatus.statusColor(): Int =
        when (this) {
            AcousticStatus.CAPABLE -> WscanUi.COLOR_OK
            AcousticStatus.UNTESTED,
            AcousticStatus.DEVICE_VARIABLE,
            -> WscanUi.COLOR_WARN
            AcousticStatus.NOT_CAPABLE -> WscanUi.COLOR_BAD
        }

    private fun CameraIrStatus.toDisplayLabel(): String =
        when (this) {
            CameraIrStatus.UNTESTED -> "UNTESTED"
            CameraIrStatus.CAPABLE -> "CAPABLE"
            CameraIrStatus.NOT_CAPABLE -> "NOT CAPABLE"
            CameraIrStatus.PARTIAL -> "PARTIAL"
        }

    private fun AcousticStatus.toDisplayLabel(): String =
        when (this) {
            AcousticStatus.UNTESTED -> "UNTESTED"
            AcousticStatus.CAPABLE -> "CAPABLE"
            AcousticStatus.NOT_CAPABLE -> "NOT CAPABLE"
            AcousticStatus.DEVICE_VARIABLE -> "DEVICE VARIABLE"
        }

    private fun formatMs(epochMs: Long): String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(epochMs))

    companion object {
        @Suppress("unused")
        private const val TAG = "DiagnosticActivity"
        private const val REFRESH_INTERVAL_MS = 2_000L
    }
}
