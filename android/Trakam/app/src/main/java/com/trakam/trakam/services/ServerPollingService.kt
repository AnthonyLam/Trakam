package com.trakam.trakam.services

import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.ConditionVariable
import android.os.Handler
import android.os.Looper
import android.support.annotation.UiThread
import com.trakam.trakam.data.Log
import com.trakam.trakam.util.*
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedReader
import java.util.*
import kotlin.concurrent.thread

class ServerPollingService : BaseService() {

    companion object {
        // poll every 10 seconds
        private const val POLL_INTERVAL = 5 * 1000L
    }

    @Volatile
    private var mPaused = false
    private val mPauseCondition = ConditionVariable()

    private val mLogsLock = Any()
    private val mLogs = mutableListOf<Log>()
    private val mBinder = LocalBinder()
    private var mThread: Thread? = null
    private val mHandler = Handler(Looper.getMainLooper())

    private var mOnLogEventListener: OnLogEventListener? = null

    @Volatile
    private var mRunning = true

    private val mRequestLock = Any()
    private lateinit var mRequest: Request

    override fun onCreate() {
        super.onCreate()
        setupRequest()
    }

    private fun setupRequest() {
        val host = getDefaultSharedPreferences().getString(PrefKeys.Server.KEY_SERVER_HOST,
                PrefKeys.Server.Default.SERVER_HOST)
        val port = getDefaultSharedPreferences().getString(PrefKeys.Server.KEY_SERVER_PORT,
                PrefKeys.Server.Default.SERVER_PORT)
        val url = HttpUrl.parse("http://$host:$port/logs")
                ?: throw RuntimeException("Failed to parse url")
        synchronized(mRequestLock) {
            mRequest = Request.Builder()
                    .url(url)
                    .build()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            PrefKeys.Server.KEY_SERVER_HOST, PrefKeys.Server.KEY_SERVER_PORT -> {
                setupRequest()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (mThread == null) {
            mThread = thread {
                continuouslyPoll()
            }
        }
        return START_STICKY
    }

    private fun continuouslyPoll() {
        val logs = mutableListOf<Log>()

        while (mRunning) {
            if (mPaused) {
                mPauseCondition.block()
                mPauseCondition.close()
            }

            val req = synchronized(mRequestLock) {
                mRequest
            }

            val res = ServerUtil.makeRequest(req) {
                handleResponse(it)
            }
            if (res.success && res.data != null) {
                MyLogger.logInfo(this::class, "Polled server")
                logs.clear()

                for (line in res.data) {
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
            } else {
                mHandler.post {
                    mOnLogEventListener?.onServerError()
                }
            }
            try {
                Thread.sleep(POLL_INTERVAL)
            } catch (e: InterruptedException) {
            }
        }
    }

    private fun handleResponse(response: Response): List<String>? {
        val body = response.body() ?: return null

        return try {
            val reader = BufferedReader(body.charStream())
            val list = mutableListOf<String>()
            reader.forEachLine {
                list += it
            }
            list
        } catch (e: Exception) {
            null
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
        mThread?.interrupt()
        mThread?.join()
        mThread = null
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

    @UiThread
    fun onServerError()
}