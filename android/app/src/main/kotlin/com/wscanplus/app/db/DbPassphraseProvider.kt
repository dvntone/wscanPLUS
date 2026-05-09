package com.wscanplus.app.db

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.SecureRandom

/**
 * Manages the SQLCipher passphrase for WscanDatabase.
 *
 * On first call to [getOrCreate]:
 * - Deletes any existing plaintext wscan.db files (one-time migration to encrypted storage)
 * - Generates a cryptographically random 32-byte passphrase
 * - Stores it in EncryptedSharedPreferences (Keystore-backed AES-256-GCM)
 *
 * Subsequent calls return the stored passphrase unchanged.
 *
 * Returns null if EncryptedSharedPreferences is unavailable. Callers must
 * treat a null result as a hard failure — the database must not be opened
 * without encryption (fail-closed).
 */
class DbPassphraseProvider(
    private val context: Context,
) {
    fun getOrCreate(): ByteArray? {
        val prefs = createEncryptedPrefs() ?: return null
        val stored = prefs.getString(KEY_PASSPHRASE, null)
        if (stored != null) {
            return hexToBytes(stored)
        }
        deletePlaintextDb()
        val passphrase = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(KEY_PASSPHRASE, bytesToHex(passphrase)).apply()
        Log.i(TAG, "DB passphrase generated; existing plaintext DB removed if present")
        return passphrase
    }

    private fun deletePlaintextDb() {
        listOf(DB_NAME, "$DB_NAME-shm", "$DB_NAME-wal").forEach { name ->
            val f = context.getDatabasePath(name)
            if (f.exists() && f.delete()) {
                Log.i(TAG, "Deleted plaintext DB file: $name")
            }
        }
    }

    private fun createEncryptedPrefs() =
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            Log.e(TAG, "EncryptedSharedPreferences unavailable — refusing plaintext fallback for DB passphrase", e)
            null
        }

    companion object {
        private const val TAG = "DbPassphraseProvider"
        private const val PREFS_NAME = "wscan_db_key"
        private const val KEY_PASSPHRASE = "db_passphrase"
        private const val PASSPHRASE_BYTES = 32
        internal const val DB_NAME = "wscan.db"

        private fun bytesToHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

        private fun hexToBytes(hex: String): ByteArray =
            ByteArray(hex.length / 2) {
                hex.substring(it * 2, it * 2 + 2).toInt(16).toByte()
            }
    }
}
