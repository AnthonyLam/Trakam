package com.trakam.trakam.util

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

object ServerUtil {
    const val BASE_URL = "http://192.168.0.151:8080/%s"

    private val sOkHttpClient = OkHttpClient()

    fun <T> makeRequest(req: Request, responseHandler: ((Response) -> T)? = null): Result<T> {
        try {
            val response = sOkHttpClient.newCall(req).execute()
            if (response.isSuccessful) {
                return Result(true, responseHandler?.invoke(response))
            }
        } catch (e: IOException) {
            MyLogger.logError(this::class, "call failed: ${e.message}")
        }
        return Result(false, null)
    }

    data class Result<out T>(val success: Boolean, val data: T?)
}