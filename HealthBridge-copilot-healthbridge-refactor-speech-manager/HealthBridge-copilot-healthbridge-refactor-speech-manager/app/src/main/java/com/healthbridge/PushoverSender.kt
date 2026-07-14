package com.healthbridge

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.FormBody

object PushoverSender {

    private const val TAG       = "PushoverSender"
    private const val API_URL   = "https://api.pushover.net/1/messages.json"
    private const val APP_TOKEN = "YOUR_APP_TOKEN"  // 🔑 replace this
    private const val USER_KEY  = "YOUR_USER_KEY"   // 🔑 replace this

    private val client = OkHttpClient()

    suspend fun send(
        message : String,
        title   : String = "HealthBridge"
    ) = withContext(Dispatchers.IO) {

        try {
            val body = FormBody.Builder()
                .add("token",   APP_TOKEN)
                .add("user",    USER_KEY)
                .add("message", message)
                .add("title",   title)
                .build()

            val request = Request.Builder()
                .url(API_URL)
                .post(body)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                Log.d(TAG, "Notification sent: $message")
            } else {
                Log.e(TAG, "Failed to send: ${response.code} ${response.message}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification: ${e.message}")
        }
    }
}
