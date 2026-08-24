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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.util.Calendar

class WearTelemetryService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private val binder = LocalBinder()

    companion object {
        private const val TAG = "WearTelemetry"
        private const val HEART_RATE_UPDATE_INTERVAL = 10000L // 10 seconds
    }

    inner class LocalBinder : Binder() {
        fun getService(): WearTelemetryService = this@WearTelemetryService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "WearTelemetryService created")

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

                    sendDataToFirebase(currentHeartRate, batteryLevel, timestamp)

                    Thread.sleep(HEART_RATE_UPDATE_INTERVAL)
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

    private fun sendDataToFirebase(heartRate: Int, battery: Int, timestamp: Long) {
        try {
            val database = FirebaseDatabase.getInstance()
            val watchDataRef = database.reference
                .child("watch_data")
                .child("latest")

            val data = hashMapOf(
                "heart_rate" to heartRate,
                "battery" to battery,
                "timestamp" to timestamp
            )

            watchDataRef.setValue(data)
                .addOnSuccessListener {
                    Log.d(TAG, "Watch data sent to Firebase: HR=$heartRate, Battery=$battery")
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "Failed to send watch data to Firebase", error)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending data to Firebase", e)
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
