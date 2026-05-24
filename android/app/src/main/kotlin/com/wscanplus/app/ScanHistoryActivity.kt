package com.wscanplus.app

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.wscanplus.app.db.DbPassphraseProvider
import com.wscanplus.core.db.WscanDatabase
import com.wscanplus.core.db.entity.ScanSessionEntity
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.util.concurrent.Executors

class ScanHistoryActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var contentLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Scan History"

        val root = WscanUi.shell(this)
        WscanUi.header(
            root,
            "Scan History",
            "Recent scanner sessions, AP observations, and threat signal counts",
        )
        val scrollView = ScrollView(this).apply { isFillViewport = true }
        contentLayout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

        scrollView.addView(contentLayout)
        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )
        setContentView(root)
        loadHistory()
    }

    private fun loadHistory() {
        executor.execute {
            try {
                val passphrase = DbPassphraseProvider(applicationContext).getOrCreate()
                if (passphrase == null) {
                    showFailure("Encrypted database unavailable.")
                    return@execute
                }
                SQLiteDatabase.loadLibs(applicationContext)
                val db = WscanDatabase.getInstance(applicationContext, SupportFactory(passphrase))
                val sessions = db.scanSessionDao().getRecent(HISTORY_LIMIT)
                val rows =
                    sessions.map { session ->
                        val resultCount = db.scanResultDao().getBySession(session.id).size
                        val signalCount = db.threatSignalDao().getBySession(session.id).size
                        SessionHistoryRow(
                            session = session,
                            resultCount = resultCount,
                            signalCount = signalCount,
                        )
                    }
                if (isDestroyed) return@execute
                runOnUiThread { renderHistory(rows) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load scan history", e)
                showFailure("Failed to load scan history.")
            }
        }
    }

    private fun renderHistory(rows: List<SessionHistoryRow>) {
        if (rows.isEmpty()) {
            val card = WscanUi.card(contentLayout)
            WscanUi.body(card, "No scan sessions recorded yet.", muted = true)
            return
        }

        val first = rows.minByOrNull { it.session.startedAt }
        val last = rows.maxByOrNull { it.session.startedAt }
        val totalAps = rows.sumOf { it.resultCount }
        val totalThreats = rows.sumOf { it.signalCount }

        val summaryText =
            buildString {
                append("${rows.size} sessions")
                if (first != null && last != null && first.session.id != last.session.id) {
                    append("  \u2022  ")
                    append(formatDate(first.session.startedAt))
                    append(" \u2013 ")
                    append(formatDate(last.session.startedAt))
                }
                appendLine()
                append("$totalAps AP observations  \u2022  $totalThreats threat signals")
            }

        val summaryCard = WscanUi.card(contentLayout)
        WscanUi.sectionTitle(summaryCard, "Summary")
        WscanUi.body(summaryCard, summaryText)

        rows.forEach { row -> contentLayout.addView(buildRow(row)) }
    }

    private fun buildRow(row: SessionHistoryRow): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(dp(14), dp(12), dp(14), dp(12))
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.RECTANGLE
        bg.cornerRadius = dp(12).toFloat()
        bg.setColor(WscanUi.COLOR_CARD)
        bg.setStroke(dp(1), WscanUi.COLOR_CARD_ALT)
        card.background = bg

        val lp =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        lp.bottomMargin = dp(10)
        card.layoutParams = lp

        val duration =
            row.session.endedAt?.let { formatDuration(it - row.session.startedAt) }
                ?: "in progress"

        card.addView(
            TextView(this).apply {
                text = formatDateTime(row.session.startedAt)
                textSize = 15f
                setTextColor(WscanUi.COLOR_TEXT)
                setTypeface(typeface, Typeface.BOLD)
            },
        )

        val meta =
            buildString {
                append(duration)
                append("  \u2022  ${row.resultCount} APs")
                if (row.signalCount > 0) append("  \u2022  ${row.signalCount} threats")
            }

        card.addView(
            TextView(this).apply {
                text = meta
                textSize = 13f
                setTextColor(WscanUi.COLOR_MUTED)
                setPadding(0, dp(4), 0, 0)
            },
        )

        return card
    }

    private fun formatDate(ts: Long): String = DateFormat.format("MMM d", ts).toString()

    private fun formatDateTime(ts: Long): String = DateFormat.format("yyyy-MM-dd  HH:mm", ts).toString()

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return if (min > 0) "${min}m ${sec}s" else "${sec}s"
    }

    private fun showFailure(message: String) {
        if (isDestroyed) return
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }

    companion object {
        private const val TAG = "ScanHistoryActivity"
        private const val HISTORY_LIMIT = 50
    }
}

private data class SessionHistoryRow(
    val session: ScanSessionEntity,
    val resultCount: Int,
    val signalCount: Int,
)
