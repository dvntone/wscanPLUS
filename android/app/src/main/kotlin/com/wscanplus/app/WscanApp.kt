package com.wscanplus.app

import android.app.Application
import com.wscanplus.app.theme.ThemeController
import com.wscanplus.app.theme.ThemeStore

class WscanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeController.apply(ThemeStore(this).read())
    }
}
