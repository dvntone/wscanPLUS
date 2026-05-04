package com.wscanplus.app.theme

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import com.wscanplus.app.WscanUi

class ThemeSettingsActivity : Activity() {
    private lateinit var themeStore: ThemeStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeStore = ThemeStore(this)

        val rootLayout = WscanUi.shell(this)
        WscanUi.header(
            rootLayout,
            title = "Theme",
            subtitle = "Choose Light, Dark, or follow the system",
        )

        val card = WscanUi.card(rootLayout)
        WscanUi.sectionTitle(card, "Preference")

        val radioGroup =
            RadioGroup(this).apply {
                orientation = LinearLayout.VERTICAL
            }

        val systemOption = makeOption("System default", ThemePreference.SYSTEM)
        val lightOption = makeOption("Light", ThemePreference.LIGHT)
        val darkOption = makeOption("Dark", ThemePreference.DARK)
        radioGroup.addView(systemOption)
        radioGroup.addView(lightOption)
        radioGroup.addView(darkOption)
        card.addView(radioGroup)

        setContentView(rootLayout)

        when (themeStore.read()) {
            ThemePreference.SYSTEM -> systemOption.isChecked = true
            ThemePreference.LIGHT -> lightOption.isChecked = true
            ThemePreference.DARK -> darkOption.isChecked = true
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selected =
                when (checkedId) {
                    systemOption.id -> ThemePreference.SYSTEM
                    lightOption.id -> ThemePreference.LIGHT
                    darkOption.id -> ThemePreference.DARK
                    else -> ThemePreference.SYSTEM
                }
            themeStore.write(selected)
            ThemeController.apply(selected)
        }
    }

    private fun makeOption(
        label: String,
        preference: ThemePreference,
    ): RadioButton =
        RadioButton(this).apply {
            text = label
            setTextColor(WscanUi.COLOR_TEXT)
            textSize = 14f
            tag = preference
        }
}

