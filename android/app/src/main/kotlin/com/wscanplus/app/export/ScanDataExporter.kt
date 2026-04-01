package com.wscanplus.app.export

import android.content.Context
import android.util.Log
import com.wscanplus.app.db.DbPassphraseProvider
import com.wscanplus.core.db.WscanDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Serializes the 20 most recent scan sessions — including scan results, threat signals,
 * and Gemini narratives — to a JSON file in the app's cache directory.
 *
 * Must be called off the main thread.
 */
class ScanDataExporter(private val context: Context) {
    fun export(): File {
        val passphrase =
            DbPassphraseProvider(context).getOrCreate()
                ?: error("Encrypted database unavailable — cannot export.")

        SQLiteDatabase.loadLibs(context)
        val db = WscanDatabase.getInstance(context, SupportFactory(passphrase))
        val sessions = db.scanSessionDao().getRecent(EXPORT_SESSION_LIMIT)

        val sessionsArray = JSONArray()
        for (session in sessions) {
            val results = db.scanResultDao().getBySession(session.id)
            val signals = db.threatSignalDao().getBySession(session.id)
            val narratives = db.geminiNarrativeDao().getBySession(session.id)

            val resultsArray = JSONArray()
            for (r in results) {
                val obj = JSONObject()
                obj.put("id", r.id)
                obj.put("bssid", r.bssid)
                obj.put("ssid", r.ssid)
                obj.put("capabilities", r.capabilities)
                obj.put("rssiDbm", r.rssiDbm)
                obj.put("frequencyMhz", r.frequencyMhz)
                obj.put("channelWidth", r.channelWidth)
                obj.put("timestamp", r.timestamp)
                obj.put("isHidden", r.isHidden)
                resultsArray.put(obj)
            }

            val signalsArray = JSONArray()
            for (s in signals) {
                val reasonsArray = JSONArray()
                for (reason in s.reasons) reasonsArray.put(reason)
                val obj = JSONObject()
                obj.put("id", s.id)
                obj.put("bssid", s.bssid)
                obj.put("confidence", s.confidence)
                obj.put("source", s.source.name)
                obj.put("heuristicType", s.heuristicType?.name)
                obj.put("reasons", reasonsArray)
                obj.put("detectedAt", s.detectedAt)
                signalsArray.put(obj)
            }

            val narrativesArray = JSONArray()
            for (n in narratives) {
                val obj = JSONObject()
                obj.put("id", n.id)
                obj.put("narrative", n.narrative)
                obj.put("generatedAt", n.generatedAt)
                obj.put("signalCount", n.signalCount)
                obj.put("modelName", n.modelName)
                narrativesArray.put(obj)
            }

            val sessionObj = JSONObject()
            sessionObj.put("id", session.id)
            sessionObj.put("startedAt", session.startedAt)
            sessionObj.put("endedAt", session.endedAt)
            sessionObj.put("environmentType", session.environmentType.name)
            sessionObj.put("deviceSerial", session.deviceSerial)
            sessionObj.put("scanResults", resultsArray)
            sessionObj.put("threatSignals", signalsArray)
            sessionObj.put("geminiNarratives", narrativesArray)
            sessionsArray.put(sessionObj)
        }

        val root = JSONObject()
        root.put("exportedAt", ISO_FORMAT.format(Date()))
        root.put("sessionCount", sessions.size)
        root.put("sessions", sessionsArray)

        val exportsDir = File(context.cacheDir, "exports")
        exportsDir.mkdirs()
        val timestamp = FILE_TIMESTAMP_FORMAT.format(Date())
        val file = File(exportsDir, "wscanplus_$timestamp.json")
        file.writeText(root.toString(2))
        Log.i(TAG, "Exported ${sessions.size} sessions to ${file.name}")
        return file
    }

    companion object {
        private const val TAG = "ScanDataExporter"
        private const val EXPORT_SESSION_LIMIT = 20
        private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        private val FILE_TIMESTAMP_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    }
}
