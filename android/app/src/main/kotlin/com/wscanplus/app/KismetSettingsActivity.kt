package com.wscanplus.app

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.wscanplus.app.kismet.KismetConfig
import com.wscanplus.app.kismet.KismetConfigStore

class KismetSettingsActivity : Activity() {
    private lateinit var enabledCheckbox: CheckBox
    private lateinit var baseUrlInput: EditText
    private lateinit var tokenInput: EditText
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = KismetConfigStore(this).load()

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 48, 48, 48)
            }

        enabledCheckbox =
            CheckBox(this).apply {
                text = "Enable Kismet Web GPS"
                isChecked = config.enabled
            }
        baseUrlInput = buildInput("Kismet Base URL", config.baseUrl)
        tokenInput =
            buildInput("Kismet API Token (optional)", config.apiToken).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
        statusView =
            TextView(this).apply {
                text =
                    "Use a direct Kismet URL when the device can reach the host. Use 127.0.0.1 only when adb reverse is active on the host port."
            }
        val saveButton =
            Button(this).apply {
                text = "Save"
                setOnClickListener {
                    saveConfig()
                }
            }

        root.addView(enabledCheckbox)
        root.addView(baseUrlInput)
        root.addView(tokenInput)
        root.addView(statusView)
        root.addView(saveButton)
        setContentView(root)
    }

    private fun buildInput(
        hint: String,
        value: String,
    ): EditText =
        EditText(this).apply {
            this.hint = hint
            setText(value)
            layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
        }

    private fun saveConfig() {
        val config =
            KismetConfig(
                enabled = enabledCheckbox.isChecked,
                baseUrl = baseUrlInput.text.toString().trim(),
                apiToken = tokenInput.text.toString().trim(),
            )
        KismetConfigStore(this).save(config)
        statusView.text = "Saved. Restart field mode if it is already running."
    }
}
