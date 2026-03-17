package com.wscanplus.core.scanner

import android.content.Context

/**
 * RootScanner provides enhanced scanning on custom-kernel devices with root access.
 *
 * HARD RULE: This scanner MUST NEVER run without an explicit developer opt-in flag.
 * It is NOT part of the ScannerChain and is NEVER a silent fallback.
 * It is only instantiated when the user has explicitly enabled developer mode
 * AND confirmed the root scanner opt-in.
 *
 * Rationale: root scanning works only on custom-kernel devices (a small minority).
 * Running it silently would be unexpected behaviour for end users and creates
 * a security surface that must remain under explicit user control.
 *
 * Threading rule: scan operations MUST be called from a background thread.
 *
 * TODO (Phase 1): implement developer opt-in flag check (BuildConfig or prefs).
 * TODO (Phase 1): implement root-based scan using su + iw/wpa_cli.
 */
class RootScanner(
    private val context: Context,
) {
    /**
     * Guard: always verify the developer opt-in flag before any scan operation.
     * Throws IllegalStateException if called without explicit opt-in.
     */
    fun start(developerOptIn: Boolean) {
        check(developerOptIn) {
            "RootScanner requires explicit developer opt-in. Never call without user consent."
        }
        // TODO: perform root-based scan — only reaches here with explicit opt-in
    }

    fun stop() {
        // TODO: clean up root scan resources
    }
}
