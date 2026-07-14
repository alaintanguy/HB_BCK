package com.healthbridge.telemetry

import android.content.Context
import android.os.Build
import android.util.Log
import com.healthbridge.firebase.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TelemetryEngine(private val context: Context) {

    private val TAG = "TelemetryEngine"
    private val gpsCollector = GpsCollector(context)
    private val batteryCollector = BatteryCollector(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start(memberId: String) {
        scope.launch {
            while (isActive) {
                try {
                    push(memberId)
                } catch (e: Exception) {
                    Log.e(TAG, "Telemetry push error: ${e.message}")
                }
                delay(INTERVAL_MS)
            }
        }
        Log.d(TAG, "TelemetryEngine started for $memberId")
    }

    fun stop() {
        scope.cancel()
        gpsCollector.stop()
        Log.d(TAG, "TelemetryEngine stopped")
    }

    private fun push(memberId: String) {
        val now = System.currentTimeMillis()
        val battery = batteryCollector.collect()
        val location = gpsCollector.getLocation()

        val deviceUpdates: Map<String, Any> = buildMap {
            put("lastSeen", now)
            put("status", "online")
            put("phoneBattery", battery.level)
            put("charging", battery.charging)
            put("phoneBrand", Build.MANUFACTURER)
            put("phoneModel", Build.MODEL)
        }
        FirebaseManager.updateDevice(memberId, deviceUpdates)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val stf = SimpleDateFormat("HH:mm:ss", Locale.US)
        val telemetryUpdates: MutableMap<String, Any> = mutableMapOf(
            "timestamp" to now,
            "readable/date" to sdf.format(Date(now)),
            "readable/time" to stf.format(Date(now))
        )
        location?.let {
            telemetryUpdates["location/lat"] = it.lat
            telemetryUpdates["location/lng"] = it.lng
        }
        FirebaseManager.updateTelemetry(memberId, telemetryUpdates)

        Log.d(TAG, "Telemetry pushed for $memberId (battery=${battery.level}%)")
    }

    companion object {
        private const val INTERVAL_MS = 300_000L
    }
}
