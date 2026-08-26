package com.healthbridge.wear.health.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.healthbridge.wear.health.WatchCapabilityCatalog
import com.healthbridge.wear.health.WatchHealthDataManager
import com.healthbridge.wear.health.WearLogTags
import com.healthbridge.wear.health.model.PermissionState
import com.healthbridge.wear.health.model.WatchMeasurementStatus
import com.healthbridge.wear.health.model.WatchMeasurementType

class HeartRateManager(context: Context) : WatchHealthDataManager, SensorEventListener {

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    private var latestHeartRate = 0
    private var lastUpdatedAt: Long? = null
    private var registered = false
    private var onUpdate: (() -> Unit)? = null

    override fun start(onUpdate: (() -> Unit)?) {
        this.onUpdate = onUpdate
        refresh()
    }

    override fun refresh() {
        if (heartRateSensor == null) {
            Log.w(WearLogTags.API, "HeartRateManager: TYPE_HEART_RATE unavailable")
            return
        }
        if (hasBodySensorsPermission() && !registered) {
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
            registered = true
            Log.d(WearLogTags.API, "HeartRateManager registered TYPE_HEART_RATE listener")
        }
    }

    override fun stop() {
        if (registered) {
            sensorManager.unregisterListener(this, heartRateSensor)
            registered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_HEART_RATE) {
            val reading = event.values.firstOrNull()?.toInt() ?: 0
            if (reading > 0) {
                latestHeartRate = reading
                lastUpdatedAt = System.currentTimeMillis()
                Log.d(WearLogTags.HEALTH, "Heart rate = $latestHeartRate BPM")
                onUpdate?.invoke()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun statuses(): List<WatchMeasurementStatus> {
        val definition = WatchCapabilityCatalog.definitionFor(WatchMeasurementType.HEART_RATE)
        val permissionState = if (hasBodySensorsPermission()) {
            PermissionState.GRANTED
        } else {
            PermissionState.DENIED
        }
        val statusNotes = when {
            heartRateSensor == null -> "TYPE_HEART_RATE is not present on this watch."
            permissionState == PermissionState.DENIED -> "Grant BODY_SENSORS to read live heart rate."
            latestHeartRate > 0 -> definition.notes
            else -> "Waiting for a valid live heart-rate sample."
        }
        return listOf(
            WatchMeasurementStatus(
                definition = definition,
                permissionState = permissionState,
                currentValue = if (latestHeartRate > 0) "$latestHeartRate BPM" else "-- waiting",
                lastUpdatedAt = lastUpdatedAt,
                statusNotes = statusNotes
            )
        )
    }

    private fun hasBodySensorsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.BODY_SENSORS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
