package com.healthbridge.wear.health.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.healthbridge.wear.health.WatchCapabilityCatalog
import com.healthbridge.wear.health.WatchHealthDataManager
import com.healthbridge.wear.health.WearLogTags
import com.healthbridge.wear.health.model.PermissionState
import com.healthbridge.wear.health.model.WatchMeasurementStatus
import com.healthbridge.wear.health.model.WatchMeasurementType

class StepsManager(context: Context) : WatchHealthDataManager, SensorEventListener {

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var latestSteps: Long? = null
    private var lastUpdatedAt: Long? = null
    private var registered = false
    private var onUpdate: (() -> Unit)? = null

    override fun start(onUpdate: (() -> Unit)?) {
        this.onUpdate = onUpdate
        refresh()
    }

    override fun refresh() {
        if (stepCounter == null) {
            Log.w(WearLogTags.API, "StepsManager: TYPE_STEP_COUNTER unavailable")
            return
        }
        if (hasActivityPermission() && !registered) {
            sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL)
            registered = true
            Log.d(WearLogTags.API, "StepsManager registered TYPE_STEP_COUNTER listener")
        }
    }

    override fun stop() {
        if (registered) {
            sensorManager.unregisterListener(this, stepCounter)
            registered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            latestSteps = event.values.firstOrNull()?.toLong()
            lastUpdatedAt = System.currentTimeMillis()
            Log.d(WearLogTags.HEALTH, "Steps = ${latestSteps ?: 0}")
            onUpdate?.invoke()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun statuses(): List<WatchMeasurementStatus> {
        val definition = WatchCapabilityCatalog.definitionFor(WatchMeasurementType.STEPS)
        val permissionState = if (hasActivityPermission()) {
            PermissionState.GRANTED
        } else {
            PermissionState.DENIED
        }
        val statusNotes = when {
            stepCounter == null -> "TYPE_STEP_COUNTER is not present on this watch."
            permissionState == PermissionState.DENIED -> "Grant ACTIVITY_RECOGNITION to read steps."
            latestSteps != null -> "Cumulative step count since boot."
            else -> "Waiting for a step-counter event."
        }
        return listOf(
            WatchMeasurementStatus(
                definition = definition,
                permissionState = permissionState,
                currentValue = latestSteps?.toString() ?: "-- waiting",
                lastUpdatedAt = lastUpdatedAt,
                statusNotes = statusNotes
            )
        )
    }

    private fun hasActivityPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
