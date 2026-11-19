package com.example.twelveman

import android.app.Application
import com.example.twelveman.utils.ThemeManager

class TwelveManApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Initialize and apply saved theme immediately on app startup
        ThemeManager.initTheme(this)
    }

    companion object {
        lateinit var instance: TwelveManApplication
            private set
    }
}
