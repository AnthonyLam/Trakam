package com.trakam.trakam.util

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import java.io.Closeable
import kotlin.reflect.KClass

class ServiceBinder(private val mClass: KClass<*>,
                    serviceConnection: ServiceConnection) : Closeable {

    private var mServiceConnection: ServiceConnection? = serviceConnection
    private var mBound: Boolean = false

    fun bind(context: Context) {
        if (mServiceConnection == null) {
            throw IllegalStateException("This ServiceBinder has already been closed.")
        }

        context.bindService(Intent(context, mClass.java), mServiceConnection,
                Context.BIND_ABOVE_CLIENT)
        mBound = true
    }

    fun unbind(context: Context) {
        if (mBound) {
            if (mServiceConnection != null) {
                context.unbindService(mServiceConnection!!)
            }
            mBound = false
        }
    }

    fun isBound() = mBound

    override fun close() {
        mServiceConnection = null
    }
}