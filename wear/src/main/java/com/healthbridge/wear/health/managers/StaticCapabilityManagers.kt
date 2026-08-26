package com.healthbridge.wear.health.managers

import com.healthbridge.wear.health.WatchCapabilityCatalog
import com.healthbridge.wear.health.WatchHealthDataManager
import com.healthbridge.wear.health.model.CapabilityAccess
import com.healthbridge.wear.health.model.PermissionState
import com.healthbridge.wear.health.model.WatchMeasurementStatus
import com.healthbridge.wear.health.model.WatchMeasurementType

private abstract class CapabilityOnlyManager(
    private val measurementTypes: List<WatchMeasurementType>
) : WatchHealthDataManager {

    override fun statuses(): List<WatchMeasurementStatus> {
        return measurementTypes.map { measurementType ->
            val definition = WatchCapabilityCatalog.definitionFor(measurementType)
            WatchMeasurementStatus(
                definition = definition,
                permissionState = if (definition.permissions.isEmpty()) {
                    PermissionState.NOT_REQUIRED
                } else {
                    PermissionState.RESTRICTED
                },
                currentValue = when (definition.hbAccess) {
                    CapabilityAccess.ACCESSIBLE -> "-- pending integration"
                    CapabilityAccess.RESTRICTED -> "-- restricted"
                    CapabilityAccess.UNAVAILABLE -> "-- unavailable"
                },
                lastUpdatedAt = null,
                statusNotes = definition.notes
            )
        }
    }
}

class SpO2Manager : CapabilityOnlyManager(
    listOf(WatchMeasurementType.SPO2)
)

class CardiovascularHistoryManager : CapabilityOnlyManager(
    listOf(
        WatchMeasurementType.RESTING_HEART_RATE,
        WatchMeasurementType.HEART_RATE_VARIABILITY,
        WatchMeasurementType.ECG,
        WatchMeasurementType.IRREGULAR_RHYTHM,
        WatchMeasurementType.BLOOD_PRESSURE
    )
)

class SleepManager : CapabilityOnlyManager(
    listOf(WatchMeasurementType.SLEEP)
)

class ExerciseSummaryManager : CapabilityOnlyManager(
    listOf(
        WatchMeasurementType.DISTANCE,
        WatchMeasurementType.CALORIES,
        WatchMeasurementType.ACTIVITY_CLASSIFICATION,
        WatchMeasurementType.WORKOUT_DATA,
        WatchMeasurementType.FALL_SIGNALS
    )
)
