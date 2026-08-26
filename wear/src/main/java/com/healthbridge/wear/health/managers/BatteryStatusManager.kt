package com.healthbridge.wear.health.managers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.healthbridge.wear.health.WatchCapabilityCatalog
import com.healthbridge.wear.health.WatchHealthDataManager
import com.healthbridge.wear.health.WearLogTags
import com.healthbridge.wear.health.model.PermissionState
import com.healthbridge.wear.health.model.WatchMeasurementStatus
import com.healthbridge.wear.health.model.WatchMeasurementType

class BatteryStatusManager(private val context: Context) : WatchHealthDataManager {

    private var batteryLevel = -1
    private var chargingState = "Unknown"
    private var lastUpdatedAt: Long? = null
    private var onUpdate: (() -> Unit)? = null

    override fun start(onUpdate: (() -> Unit)?) {
        this.onUpdate = onUpdate
        refresh()
    }

    override fun refresh() {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        batteryLevel = if (level >= 0 && scale > 0) (level * 100) / scale else -1
        chargingState = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
            else -> "Unknown"
        }
        lastUpdatedAt = System.currentTimeMillis()
        Log.d(WearLogTags.HEALTH, "Battery = $batteryLevel%, chargingState=$chargingState")
        onUpdate?.invoke()
    }

    override fun statuses(): List<WatchMeasurementStatus> {
        val batteryDefinition = WatchCapabilityCatalog.definitionFor(WatchMeasurementType.BATTERY_LEVEL)
        val chargingDefinition = WatchCapabilityCatalog.definitionFor(WatchMeasurementType.CHARGING_STATE)
        return listOf(
            WatchMeasurementStatus(
                definition = batteryDefinition,
                permissionState = PermissionState.NOT_REQUIRED,
                currentValue = if (batteryLevel >= 0) "$batteryLevel %" else "-- unavailable",
                lastUpdatedAt = lastUpdatedAt,
                statusNotes = batteryDefinition.notes
            ),
            WatchMeasurementStatus(
                definition = chargingDefinition,
                permissionState = PermissionState.NOT_REQUIRED,
                currentValue = chargingState,
                lastUpdatedAt = lastUpdatedAt,
                statusNotes = chargingDefinition.notes
            )
        )
    }
}
