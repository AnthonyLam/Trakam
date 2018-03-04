package com.trakam.trakam.util

import android.graphics.Bitmap
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.IOException

object ServerUtil {

    private val mOkHttpClient = OkHttpClient()

    fun newCall(url: String): List<String>? {
        val parsedUrl = HttpUrl.parse(url)
                ?: throw NullPointerException("failed to parse url: $url")
        val req: Request = Request.Builder()
                .url(parsedUrl)
                .build()

        try {
            val response = mOkHttpClient.newCall(req).execute()
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    val reader = BufferedReader(responseBody.charStream())

                    val list = mutableListOf<String>()
                    reader.forEachLine {
                        list += it
                    }

                    responseBody.close()
                    return list
                }
            }
        } catch (e: IOException) {
            MyLogger.logError(this::class, "call failed: ${e.message}")
        }
        return null
    }
}