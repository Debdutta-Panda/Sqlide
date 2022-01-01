package com

import android.app.Application
import com.debdutta.sqlide.Sqlide

class App: Application() {
    override fun onCreate() {
        super.onCreate()

        Sqlide.initialize(this)
    }
}