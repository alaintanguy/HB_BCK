package com.healthbridge.wear

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import java.nio.ByteBuffer

class WearTelemetryService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private val binder = LocalBinder()
    private lateinit var messageClient: MessageClient

    companion object {
        private const val TAG = "WearTelemetry"
        private const val WATCH_DATA_UPDATE_INTERVAL = 60000L // 60 seconds
        private const val WATCH_DATA_MESSAGE_PATH = "/healthbridge/watch/telemetry"
    }

    inner class LocalBinder : Binder() {
        fun getService(): WearTelemetryService = this@WearTelemetryService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "WearTelemetryService created")

        messageClient = Wearable.getMessageClient(this)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        if (heartRateSensor == null) {
            Log.w(TAG, "Heart rate sensor not available on this device")
        }

        startTelemetryCollection()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "WearTelemetryService started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun startTelemetryCollection() {
        // Register heart rate sensor listener
        if (heartRateSensor != null) {
            sensorManager.registerListener(
                this,
                heartRateSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Log.d(TAG, "Heart rate sensor registered")
        }

        // Start periodic battery and data collection
        collectAndSendData()
    }

    private fun collectAndSendData() {
        Thread {
            while (true) {
                try {
                    val batteryLevel = getBatteryLevel()
                    val currentHeartRate = getLatestHeartRate()
                    val timestamp = System.currentTimeMillis()

                    Log.d(TAG, "Collected - Heart Rate: $currentHeartRate, Battery: $batteryLevel")

                    sendDataToPhone(currentHeartRate, batteryLevel, timestamp)

                    Thread.sleep(WATCH_DATA_UPDATE_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in data collection", e)
                }
            }
        }.start()
    }

    private var latestHeartRate: Int = 0

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_HEART_RATE) {
            latestHeartRate = event.values[0].toInt()
            Log.d(TAG, "Heart rate update: $latestHeartRate BPM")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    private fun getLatestHeartRate(): Int {
        return latestHeartRate
    }

    private fun getBatteryLevel(): Int {
        return try {
            val batteryIntent = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1

            if (level > 0 && scale > 0) {
                (level * 100) / scale
            } else {
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery level", e)
            0
        }
    }

    private fun sendDataToPhone(heartRate: Int, battery: Int, timestamp: Long) {
        try {
            // Prepare message payload: heartRate (4 bytes) + battery (4 bytes) + timestamp (8 bytes)
            val payload = ByteBuffer.allocate(16)
                .putInt(heartRate)
                .putInt(battery)
                .putLong(timestamp)
                .array()

            Log.d(TAG, "Sending watch data to phone: HR=$heartRate, Battery=$battery")

            // Send to all connected nodes (should be the phone)
            Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
                for (node in nodes) {
                    messageClient.sendMessage(node.id, WATCH_DATA_MESSAGE_PATH, payload)
                        .addOnSuccessListener {
                            Log.d(TAG, "Watch data sent to phone: HR=$heartRate, Battery=$battery")
                        }
                        .addOnFailureListener { error ->
                            Log.e(TAG, "Failed to send watch data to phone", error)
                        }
                }
            }.addOnFailureListener { error ->
                Log.e(TAG, "Failed to get connected nodes", error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending data to phone", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "WearTelemetryService destroyed")

        if (heartRateSensor != null) {
            sensorManager.unregisterListener(this)
        }
    }
}

