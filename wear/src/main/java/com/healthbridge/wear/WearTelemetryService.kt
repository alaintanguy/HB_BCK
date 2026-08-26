package com.healthbridge.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.healthbridge.wear.health.WearLogTags
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import java.nio.ByteBuffer

class WearTelemetryService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private val binder = LocalBinder()
    private lateinit var messageClient: MessageClient

    @Volatile
    private var telemetryRunning = false
    private var telemetryThread: Thread? = null
    private var latestHeartRate: Int = 0

    companion object {
        private const val WATCH_DATA_UPDATE_INTERVAL = 60000L
        private const val WATCH_DATA_MESSAGE_PATH = "/healthbridge/watch/telemetry"
        private const val NOTIFICATION_CHANNEL_ID = "healthbridge_watch_telemetry"
        private const val NOTIFICATION_ID = 1001
    }

    inner class LocalBinder : Binder() {
        fun getService(): WearTelemetryService = this@WearTelemetryService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(WearLogTags.API, "WearTelemetryService created")

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        messageClient = Wearable.getMessageClient(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        if (heartRateSensor == null) {
            Log.w(WearLogTags.API, "Heart rate sensor not available on this device")
        }

        startTelemetryCollection()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(WearLogTags.API, "WearTelemetryService started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "HealthBridge Watch telemetry",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
        Log.d(WearLogTags.API, "Telemetry notification channel created")
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("HealthBridge")
            .setContentText("Watch monitoring active")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun startTelemetryCollection() {
        if (heartRateSensor != null) {
            sensorManager.registerListener(
                this,
                heartRateSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Log.d(WearLogTags.API, "Heart rate sensor registered for telemetry")
        }

        if (telemetryRunning) return

        telemetryRunning = true
        telemetryThread = Thread {
            while (telemetryRunning && !Thread.currentThread().isInterrupted) {
                try {
                    val batteryLevel = getBatteryLevel()
                    val currentHeartRate = getLatestHeartRate()
                    val timestamp = System.currentTimeMillis()

                    Log.d(
                        WearLogTags.HEALTH,
                        "Collected watch telemetry: heartRate=$currentHeartRate, battery=$batteryLevel"
                    )
                    sendDataToPhone(currentHeartRate, batteryLevel, timestamp)

                    Thread.sleep(WATCH_DATA_UPDATE_INTERVAL)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    Log.e(WearLogTags.HEALTH, "Error in watch data collection", e)
                }
            }
            Log.d(WearLogTags.API, "Telemetry loop stopped")
        }.also { it.start() }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_HEART_RATE) {
            val reading = event.values.firstOrNull()?.toInt() ?: 0
            if (reading > 0) {
                latestHeartRate = reading
                Log.d(WearLogTags.HEALTH, "Heart rate = $latestHeartRate BPM")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action required.
    }

    private fun getLatestHeartRate(): Int = latestHeartRate

    private fun getBatteryLevel(): Int {
        return try {
            val batteryIntent =
                registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

            if (level >= 0 && scale > 0) {
                (level * 100) / scale
            } else {
                -1
            }
        } catch (e: Exception) {
            Log.e(WearLogTags.HEALTH, "Error getting battery level", e)
            -1
        }
    }

    private fun sendDataToPhone(heartRate: Int, battery: Int, timestamp: Long) {
        try {
            val payload = ByteBuffer.allocate(16)
                .putInt(heartRate)
                .putInt(battery)
                .putLong(timestamp)
                .array()

            Log.d(
                WearLogTags.API,
                "Sending watch telemetry to phone: HR=$heartRate, battery=$battery"
            )

            Wearable.getNodeClient(this).connectedNodes
                .addOnSuccessListener { nodes ->
                    if (nodes.isEmpty()) {
                        Log.w(WearLogTags.API, "No connected phone node found")
                    }
                    for (node in nodes) {
                        messageClient.sendMessage(node.id, WATCH_DATA_MESSAGE_PATH, payload)
                            .addOnSuccessListener {
                                Log.d(
                                    WearLogTags.API,
                                    "Watch telemetry sent to phone: HR=$heartRate, battery=$battery"
                                )
                            }
                            .addOnFailureListener { error ->
                                Log.e(WearLogTags.API, "Failed to send watch data to phone", error)
                            }
                    }
                }
                .addOnFailureListener { error ->
                    Log.e(WearLogTags.API, "Failed to get connected nodes", error)
                }
        } catch (e: Exception) {
            Log.e(WearLogTags.API, "Error sending data to phone", e)
        }
    }

    override fun onDestroy() {
        Log.d(WearLogTags.API, "WearTelemetryService destroyed")
        telemetryRunning = false
        telemetryThread?.interrupt()
        telemetryThread = null

        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
        }

        super.onDestroy()
    }
}