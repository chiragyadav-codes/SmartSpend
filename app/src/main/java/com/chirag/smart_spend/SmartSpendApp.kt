package com.chirag.smart_spend

import android.app.Application

class SmartSpendApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.applySavedMode(this)
    }
}