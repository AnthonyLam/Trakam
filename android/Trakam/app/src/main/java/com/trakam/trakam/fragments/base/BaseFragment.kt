package com.trakam.trakam.fragments.base

import android.app.Fragment
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import com.trakam.trakam.services.ServerPollingService
import com.trakam.trakam.util.ServiceBinder

abstract class BaseFragment : Fragment(), ServiceConnection {
    private lateinit var mServiceBinder: ServiceBinder

    private var mServerPollingService: ServerPollingService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mServiceBinder = ServiceBinder(ServerPollingService::class, this)
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        mServerPollingService = (service as ServerPollingService.LocalBinder).getService()
        onServerPollingServiceBound(mServerPollingService!!)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        mServerPollingService = null
    }

    override fun onStart() {
        super.onStart()
        mServiceBinder.bind(activity!!)
    }

    override fun onStop() {
        super.onStop()
        mServiceBinder.unbind(activity!!)
    }

    protected fun getServerPollingService() = mServerPollingService

    abstract fun onServerPollingServiceBound(serverPollingService: ServerPollingService)
}