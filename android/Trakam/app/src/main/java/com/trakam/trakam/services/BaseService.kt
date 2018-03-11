package com.trakam.trakam.services

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder

abstract class BaseService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
    }
}