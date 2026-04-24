package com.wscanplus.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

object WscanUi {
    const val COLOR_NAVY: Int = Color.rgb(2, 28, 55)
    const val COLOR_BG: Int = Color.rgb(11, 18, 32)
    const val COLOR_CARD: Int = Color.rgb(17, 28, 47)
    const val COLOR_CARD_ALT: Int = Color.rgb(21, 35, 58)
    const val COLOR_TEXT: Int = Color.rgb(232, 240, 255)
    const val COLOR_MUTED: Int = Color.rgb(147, 163, 184)
    const val COLOR_ACCENT: Int = Color.rgb(64, 196, 255)
    const val COLOR_WARN: Int = Color.rgb(255, 193, 7)
    const val COLOR_OK: Int = Color.rgb(55, 214, 122)
    const val COLOR_BAD: Int = Color.rgb(255, 94, 94)

    fun prepareWindow(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        activity.window.statusBarColor = COLOR_NAVY
        activity.window.navigationBarColor = COLOR_BG
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            activity.window.navigationBarDividerColor = COLOR_BG
        }
    }

    fun applySystemInsets(
        root: View,
        horizontalPadding: Int = root.dp(20),
        topPadding: Int = root.dp(18),
        bottomPadding: Int = root.dp(18),
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                bars.left + horizontalPadding,
                bars.top + topPadding,
                bars.right + horizontalPadding,
                bars.bottom + bottomPadding,
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    fun shell(activity: Activity): LinearLayout {
        prepareWindow(activity)
        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_BG)
            applySystemInsets(this)
        }
    }

    fun header(parent: LinearLayout, title: String, subtitle: String? = null) {
        parent.addView(
            TextView(parent.context).apply {
                text = title
                textSize = 24f
                setTextColor(COLOR_TEXT)
                typeface = Typeface.DEFAULT_BOLD
            },
        )
        if (subtitle != null) {
            parent.addView(
                TextView(parent.context).apply {
                    text = subtitle
                    textSize = 13f
                    setTextColor(COLOR_MUTED)
                    setPadding(0, parent.dp(6), 0, parent.dp(14))
                },
            )
        }
    }

    fun card(parent: LinearLayout): LinearLayout =
        LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(parent.dp(16), parent.dp(14), parent.dp(16), parent.dp(14))
            background = rounded(COLOR_CARD, parent.dp(16), strokeColor = Color.rgb(38, 57, 87))
            parent.addView(
                this,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = parent.dp(12)
                },
            )
        }

    fun sectionTitle(parent: LinearLayout, text: String) {
        parent.addView(
            TextView(parent.context).apply {
                this.text = text.uppercase()
                textSize = 11f
                letterSpacing = 0.12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(COLOR_ACCENT)
                setPadding(0, 0, 0, parent.dp(8))
            },
        )
    }

    fun body(parent: LinearLayout, text: String, muted: Boolean = false): TextView =
        TextView(parent.context).apply {
            this.text = text
            textSize = 14f
            setTextColor(if (muted) COLOR_MUTED else COLOR_TEXT)
            setLineSpacing(0f, 1.12f)
            parent.addView(this)
        }

    fun metricRow(parent: LinearLayout, label: String, value: String, valueColor: Int = COLOR_TEXT) {
        val row =
            LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, parent.dp(4), 0, parent.dp(4))
            }
        row.addView(
            TextView(parent.context).apply {
                text = label
                textSize = 13f
                setTextColor(COLOR_MUTED)
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        row.addView(
            TextView(parent.context).apply {
                text = value
                textSize = 13f
                typeface = Typeface.MONOSPACE
                setTextColor(valueColor)
            },
        )
        parent.addView(row)
    }

    fun actionButton(parent: LinearLayout, text: String, onClick: () -> Unit): Button =
        Button(parent.context).apply {
            this.text = text
            setTextColor(COLOR_TEXT)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(COLOR_CARD_ALT, parent.dp(14), strokeColor = COLOR_ACCENT)
            setPadding(parent.dp(12), parent.dp(10), parent.dp(12), parent.dp(10))
            setOnClickListener { onClick() }
            parent.addView(
                this,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    parent.dp(52),
                ).apply {
                    bottomMargin = parent.dp(10)
                },
            )
        }

    fun statusColor(value: Boolean): Int = if (value) COLOR_OK else COLOR_BAD

    fun rounded(color: Int, radius: Int, strokeColor: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            if (strokeColor != null) {
                setStroke(1, strokeColor)
            }
        }

    fun View.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
