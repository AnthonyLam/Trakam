package com.trakam.trakam.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

abstract class BaseService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

}