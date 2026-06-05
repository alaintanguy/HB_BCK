package com.healthbridge.telemetry

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

import androidx.core.app.NotificationCompat

import com.healthbridge.R

class TelemetryForegroundService : Service() {

    private lateinit var telemetryEngine:
            TelemetryEngine

    override fun onCreate() {

        super.onCreate()

        createNotificationChannel()

        val notification: Notification =

            NotificationCompat.Builder(
                this,
                "healthbridge_channel"
            )
                .setContentTitle(
                    "HealthBridge Running"
                )
                .setContentText(
                    "Tracking location..."
                )
                .setSmallIcon(
                    R.mipmap.ic_launcher
                )
                .build()

        startForeground(
            1,
            notification
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val memberId =
            intent?.getStringExtra(
                "MEMBER_ID"
            ) ?: "unknown"

        telemetryEngine =
            TelemetryEngine(
                this,
                memberId
            )

        telemetryEngine.start()

        return START_STICKY
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    "healthbridge_channel",
                    "HealthBridge Tracking",
                    NotificationManager.IMPORTANCE_LOW
                )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }
}