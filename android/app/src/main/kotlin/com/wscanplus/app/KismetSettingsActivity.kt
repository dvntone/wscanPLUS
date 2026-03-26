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
import com.wscanplus.app.privacy.ConsentStore

class KismetSettingsActivity : Activity() {
    private lateinit var enabledCheckbox: CheckBox
    private lateinit var baseUrlInput: EditText
    private lateinit var tokenInput: EditText
    private lateinit var consentCheckbox: CheckBox
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = KismetConfigStore(this).load()
        val consentGiven = ConsentStore(this).isConsentGiven()

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
        consentCheckbox =
            CheckBox(this).apply {
                text = "Allow external threat intelligence API calls (CrowdSec CTI)"
                isChecked = consentGiven
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
        root.addView(consentCheckbox)
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
        ConsentStore(this).setConsentGiven(consentCheckbox.isChecked)

        val config =
            KismetConfig(
                enabled = enabledCheckbox.isChecked,
                baseUrl = baseUrlInput.text.toString().trim(),
                apiToken = tokenInput.text.toString().trim(),
            )
        val saved = KismetConfigStore(this).save(config)
        statusView.text =
            if (saved) {
                "Saved. Changes apply the next time field mode starts."
            } else {
                "Save failed. Encrypted storage is unavailable on this device."
            }
    }
}
