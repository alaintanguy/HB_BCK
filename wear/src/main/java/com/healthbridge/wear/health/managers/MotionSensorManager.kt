package com.healthbridge.wear.health.managers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.healthbridge.wear.health.WatchCapabilityCatalog
import com.healthbridge.wear.health.WatchHealthDataManager
import com.healthbridge.wear.health.WearLogTags
import com.healthbridge.wear.health.model.PermissionState
import com.healthbridge.wear.health.model.WatchMeasurementStatus
import com.healthbridge.wear.health.model.WatchMeasurementType
import java.util.Locale

class MotionSensorManager(context: Context) : WatchHealthDataManager, SensorEventListener {

    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private var latestAccelerometer = "-- waiting"
    private var latestGyroscope = "-- waiting"
    private var accelerometerUpdatedAt: Long? = null
    private var gyroscopeUpdatedAt: Long? = null
    private var registered = false
    private var onUpdate: (() -> Unit)? = null

    override fun start(onUpdate: (() -> Unit)?) {
        this.onUpdate = onUpdate
        refresh()
    }

    override fun refresh() {
        if (registered) return
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        registered = accelerometer != null || gyroscope != null
        if (registered) {
            Log.d(WearLogTags.API, "MotionSensorManager registered accelerometer/gyroscope listeners")
        }
    }

    override fun stop() {
        if (registered) {
            sensorManager.unregisterListener(this)
            registered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val values = event?.values ?: return
        val formatted = String.format(Locale.US, "x=%.2f y=%.2f z=%.2f", values[0], values[1], values[2])
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                latestAccelerometer = formatted
                accelerometerUpdatedAt = System.currentTimeMillis()
                Log.d(WearLogTags.HEALTH, "Accelerometer = $formatted")
            }
            Sensor.TYPE_GYROSCOPE -> {
                latestGyroscope = formatted
                gyroscopeUpdatedAt = System.currentTimeMillis()
                Log.d(WearLogTags.HEALTH, "Gyroscope = $formatted")
            }
        }
        onUpdate?.invoke()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun statuses(): List<WatchMeasurementStatus> {
        val accelerometerDefinition = WatchCapabilityCatalog.definitionFor(WatchMeasurementType.ACCELEROMETER)
        val gyroscopeDefinition = WatchCapabilityCatalog.definitionFor(WatchMeasurementType.GYROSCOPE)
        return listOf(
            WatchMeasurementStatus(
                definition = accelerometerDefinition,
                permissionState = PermissionState.NOT_REQUIRED,
                currentValue = if (accelerometer != null) latestAccelerometer else "-- unavailable",
                lastUpdatedAt = accelerometerUpdatedAt,
                statusNotes = if (accelerometer != null) {
                    accelerometerDefinition.notes
                } else {
                    "TYPE_ACCELEROMETER is not present on this watch."
                }
            ),
            WatchMeasurementStatus(
                definition = gyroscopeDefinition,
                permissionState = PermissionState.NOT_REQUIRED,
                currentValue = if (gyroscope != null) latestGyroscope else "-- unavailable",
                lastUpdatedAt = gyroscopeUpdatedAt,
                statusNotes = if (gyroscope != null) {
                    gyroscopeDefinition.notes
                } else {
                    "TYPE_GYROSCOPE is not present on this watch."
                }
            )
        )
    }
}
