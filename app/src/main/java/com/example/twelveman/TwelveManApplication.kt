package com.example.twelveman

import android.app.Application

class TwelveManApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: TwelveManApplication
            private set
    }
}

