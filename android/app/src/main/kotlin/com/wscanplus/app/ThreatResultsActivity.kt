package com.wscanplus.app

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.wscanplus.app.db.DbPassphraseProvider
import com.wscanplus.app.export.ScanDataExporter
import com.wscanplus.core.db.WscanDatabase
import com.wscanplus.core.db.entity.GeminiNarrativeEntity
import com.wscanplus.core.db.entity.ScanResultEntity
import com.wscanplus.core.db.entity.ScanSessionEntity
import com.wscanplus.core.db.entity.ThreatSignalEntity
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.util.concurrent.Executors

class ThreatResultsActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var contentLayout: LinearLayout
    private lateinit var exportButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Threat Results"

        val root = WscanUi.shell(this)
        WscanUi.header(
            root,
            "Threat Results",
            "Recent scan sessions with persisted Gemini narratives and local threat context",
        )
        val scrollView = ScrollView(this).apply { isFillViewport = true }
        contentLayout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

        exportButton = WscanUi.actionButton(contentLayout, "Export JSON") { exportJson() }

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

        loadThreatResults()
    }

    private fun loadThreatResults() {
        executor.execute {
            try {
                val passphrase = DbPassphraseProvider(applicationContext).getOrCreate()
                if (passphrase == null) {
                    showLoadFailure("Encrypted database unavailable.")
                    return@execute
                }
                SQLiteDatabase.loadLibs(applicationContext)
                val db = WscanDatabase.getInstance(applicationContext, SupportFactory(passphrase))
                val summaries =
                    db
                        .scanSessionDao()
                        .getRecent(20)
                        .map { session ->
                            val results = db.scanResultDao().getBySession(session.id)
                            val signals = db.threatSignalDao().getBySession(session.id)
                            val latestNarrative = db.geminiNarrativeDao().getBySession(session.id).firstOrNull()
                            SessionThreatSummary(
                                session = session,
                                results = results,
                                signals = signals,
                                narrative = latestNarrative,
                            )
                        }

                if (isDestroyed) return@execute
                runOnUiThread { renderSummaries(summaries) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load threat results", e)
                showLoadFailure("Failed to load threat results.")
            }
        }
    }

    private fun renderSummaries(summaries: List<SessionThreatSummary>) {
        if (summaries.isEmpty()) {
            val card = WscanUi.card(contentLayout)
            WscanUi.body(card, "No scan sessions found yet.", muted = true)
            return
        }

        summaries.forEach { summary ->
            contentLayout.addView(buildSessionCard(summary))
        }
    }

    private fun buildSessionCard(summary: SessionThreatSummary): LinearLayout {
        val card =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
                background =
                    GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(16).toFloat()
                        setColor(WscanUi.COLOR_CARD)
                        setStroke(dp(1), WscanUi.COLOR_CARD_ALT)
                    }
            }

        val layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        layoutParams.bottomMargin = dp(12)
        card.layoutParams = layoutParams

        val sessionWindow =
            buildString {
                append("Started ")
                append(formatTimestamp(summary.session.startedAt))
                summary.session.endedAt?.let {
                    append("  •  Ended ")
                    append(formatTimestamp(it))
                }
            }

        card.addView(sectionTitle("Session #${summary.session.id}"))
        card.addView(bodyText(sessionWindow))
        card.addView(
            bodyText(
                "${summary.results.size} scan results  •  ${summary.signals.size} stored threat signals  •  ${summary.session.environmentType.name.lowercase()}",
            ),
        )

        val ssids =
            summary.results
                .map { it.ssid.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(4)
        if (ssids.isNotEmpty()) {
            card.addView(labelText("Observed SSIDs"))
            card.addView(chipRow(ssids))
        }

        card.addView(labelText("Gemini Narrative"))
        val narrative = summary.narrative
        if (narrative != null) {
            card.addView(bodyText(narrative.narrative))
            card.addView(
                mutedText(
                    "Generated ${formatTimestamp(
                        narrative.generatedAt,
                    )}  •  ${narrative.signalCount} signals  •  ${narrative.modelName}",
                ),
            )
        } else {
            val emptyText =
                if (summary.signals.isEmpty()) {
                    "No persisted Gemini narrative yet. This session does not have stored threat signals."
                } else {
                    "Threat signals exist for this session, but no persisted Gemini narrative is available yet."
                }
            card.addView(bodyText(emptyText))
        }

        if (summary.signals.isNotEmpty()) {
            card.addView(labelText("Top Signals"))
            summary.signals
                .sortedByDescending { it.confidence }
                .take(3)
                .forEach { signal ->
                    val type = signal.heuristicType?.name?.replace('_', ' ') ?: signal.source.name
                    val reasons = signal.reasons.joinToString("; ")
                    card.addView(
                        bodyText(
                            "${"%.0f".format(signal.confidence * 100)}% $type: $reasons",
                        ),
                    )
                }
        }

        return card
    }

    private fun sectionTitle(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 18f
            setTextColor(WscanUi.COLOR_TEXT)
            setTypeface(typeface, Typeface.BOLD)
        }

    private fun labelText(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(WscanUi.COLOR_ACCENT)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(12), 0, dp(6))
        }

    private fun bodyText(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(WscanUi.COLOR_TEXT)
            setLineSpacing(0f, 1.15f)
            setPadding(0, 0, 0, dp(6))
        }

    private fun mutedText(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(WscanUi.COLOR_MUTED)
            setPadding(0, 0, 0, dp(4))
        }

    private fun chipRow(labels: List<String>): HorizontalScrollView {
        val row =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.START
            }
        labels.forEachIndexed { index, label ->
            row.addView(
                TextView(this).apply {
                    text = label
                    textSize = 12f
                    setTextColor(WscanUi.COLOR_TEXT)
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                    background =
                        GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = dp(999).toFloat()
                            setColor(WscanUi.COLOR_CARD_ALT)
                            setStroke(dp(1), WscanUi.COLOR_ACCENT)
                        }
                    if (index < labels.lastIndex) {
                        val lp =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                            )
                        lp.rightMargin = dp(8)
                        layoutParams = lp
                    }
                },
            )
        }
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }
    }

    private fun formatTimestamp(timestamp: Long): String = DateFormat.format("yyyy-MM-dd HH:mm", timestamp).toString()

    private fun showLoadFailure(message: String) {
        if (isDestroyed) return
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            contentLayout.addView(bodyText(message))
        }
    }

    private fun exportJson() {
        exportButton.isEnabled = false
        exportButton.text = "Exporting…"
        executor.execute {
            try {
                val file = ScanDataExporter(applicationContext).export()
                val uri: Uri =
                    FileProvider.getUriForFile(
                        applicationContext,
                        "${applicationContext.packageName}.fileprovider",
                        file,
                    )
                if (isDestroyed) return@execute
                runOnUiThread {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "application/json"
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(Intent.createChooser(intent, "Export scan data"))
                    exportButton.text = "Export JSON"
                    exportButton.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Export failed", e)
                if (isDestroyed) return@execute
                runOnUiThread {
                    Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                    exportButton.text = "Export JSON"
                    exportButton.isEnabled = true
                }
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }

    companion object {
        private const val TAG = "ThreatResultsActivity"
    }
}

private data class SessionThreatSummary(
    val session: ScanSessionEntity,
    val results: List<ScanResultEntity>,
    val signals: List<ThreatSignalEntity>,
    val narrative: GeminiNarrativeEntity?,
)
