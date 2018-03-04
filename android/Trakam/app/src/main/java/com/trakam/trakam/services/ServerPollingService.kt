package com.trakam.trakam.services

import android.content.Intent
import android.os.Binder
import android.os.ConditionVariable
import android.os.Handler
import android.os.Looper
import android.support.annotation.UiThread
import com.trakam.trakam.data.Log
import com.trakam.trakam.util.MyLogger
import com.trakam.trakam.util.ServerUtil
import com.trakam.trakam.util.StringSplitter
import java.util.*
import kotlin.concurrent.thread

class ServerPollingService : BaseService() {

    companion object {
        // poll every 10 seconds
        private const val POLL_INTERVAL = 5 * 1000L
        private const val URL = "http://192.168.0.151:8080/logs"
    }

    @Volatile
    private var mPaused = false
    private val mPauseCondition = ConditionVariable()

    private val mLogsLock = Any()
    private val mLogs = mutableListOf<Log>()
    private val mBinder = LocalBinder()
    private lateinit var mThread: Thread
    private val mHandler = Handler(Looper.getMainLooper())

    private var mOnLogEventListener: OnLogEventListener? = null

    @Volatile
    private var mRunning = true

    override fun onCreate() {
        super.onCreate()
        mThread = thread {
            continuouslyPoll()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun continuouslyPoll() {
        val logs = mutableListOf<Log>()

        while (mRunning) {
            if (mPaused) {
                mPauseCondition.block()
                mPauseCondition.close()
            }

            val res = ServerUtil.newCall(URL)
            if (res != null) {
                logs.clear()

                for (line in res) {
                    val tokens = StringSplitter.split(line, ",")
                    if (tokens.size == 3) {
                        try {
                            val id = tokens[0].trim()
                            val name = tokens[1].trim()

                            val firstNameLastName = StringSplitter.splitOnEmptySequence(name,
                                    2)
                            val firstName: String
                            val lastName: String
                            if (firstNameLastName.size == 2) {
                                firstName = firstNameLastName[0]
                                lastName = firstNameLastName[1]
                            } else {
                                firstName = firstNameLastName[0]
                                lastName = ""
                            }

                            val time = tokens[2].trim().toLong()

                            val log = Log(id, firstName, lastName, Date(time))
                            logs += log

                            MyLogger.logDebug(ServerPollingService::class, "log: $log")
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                        }
                    } else {
                        MyLogger.logDebug(ServerPollingService::class,
                                "invalid tokens: ${Arrays.toString(tokens)}")
                    }
                }

                val logsCopy = logs.toList()
                mHandler.post {
                    mOnLogEventListener?.onLogsEvent(logsCopy)
                }

                synchronized(mLogsLock) {
                    mLogs.clear()
                    mLogs += logs
                }
            }
            try {
                Thread.sleep(POLL_INTERVAL)
            } catch (e: InterruptedException) {
            }
        }
    }

    fun pause() {
        mPaused = true
    }

    fun resume() {
        mPaused = false
        mPauseCondition.open()
    }

    override fun onDestroy() {
        super.onDestroy()
        mRunning = false
        mThread.interrupt()
        mThread.join()
        mOnLogEventListener = null
    }

    fun setOnLogEventListener(onLogEventListener: OnLogEventListener?) {
        mOnLogEventListener = onLogEventListener
    }

    fun getLogs() = synchronized(mLogsLock) {
        mLogs.toList()
    }

    override fun onBind(intent: Intent?) = mBinder

    inner class LocalBinder : Binder() {
        fun getService() = this@ServerPollingService
    }
}

interface OnLogEventListener {

    @UiThread
    fun onLogsEvent(logs: List<Log>)
}