package com.wscanplus.core.scanner

import android.content.Context

/**
 * UsbScanner detects and uses an external USB OTG WiFi adapter for enhanced scanning.
 * No root required — uses the Android USB Host API (android.hardware.usb.*).
 *
 * Detection: scan connected USB devices by vendor/product ID against known adapter list.
 * Availability: only when a supported OTG adapter is physically connected.
 *
 * Threading rule: start() and stop() MUST be called from a background thread.
 *
 * TODO (Phase 1): implement USB device detection via UsbManager.
 * TODO (Phase 1): build vendor/product ID list for common USB WiFi adapters.
 * TODO (Phase 1): implement scan using the detected adapter.
 */
class UsbScanner(private val context: Context) {

    /**
     * Returns true if a supported USB OTG WiFi adapter is connected.
     * Always returns false until USB detection is implemented.
     */
    fun isAvailable(): Boolean {
        // TODO: query UsbManager for connected devices matching known adapter IDs
        return false
    }

    fun start() {
        // TODO: begin scanning via connected USB adapter
    }

    fun stop() {
        // TODO: release USB adapter resources
    }
}
