package com.healthbridge.wear.health.model

import com.healthbridge.wear.health.WatchCapabilityCatalog

enum class CapabilityAccess(val label: String) {
    ACCESSIBLE("Yes"),
    RESTRICTED("Restricted"),
    UNAVAILABLE("No")
}

enum class WatchAccessType(val label: String) {
    REAL_TIME("Real-time"),
    HISTORICAL("Historical"),
    BOTH("Both"),
    UNAVAILABLE("Unavailable")
}

enum class PermissionState {
    GRANTED,
    DENIED,
    NOT_REQUIRED,
    RESTRICTED
}

enum class BatteryImpact(val label: String) {
    VERY_LOW("Very low"),
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    NOT_APPLICABLE("N/A")
}

enum class WatchMeasurementType(val displayName: String) {
    HEART_RATE("Heart Rate"),
    RESTING_HEART_RATE("Resting Heart Rate"),
    HEART_RATE_VARIABILITY("Heart Rate Variability (HRV)"),
    ECG("ECG"),
    IRREGULAR_RHYTHM("Irregular Rhythm / AF"),
    SPO2("Blood Oxygen (SpO2)"),
    SLEEP("Sleep"),
    STEPS("Steps"),
    DISTANCE("Distance"),
    CALORIES("Calories Burned"),
    ACTIVITY_CLASSIFICATION("Activity Classification"),
    WORKOUT_DATA("Exercise / Workout Data"),
    ACCELEROMETER("Accelerometer"),
    GYROSCOPE("Gyroscope"),
    FALL_SIGNALS("Fall Detection Signals"),
    BATTERY_LEVEL("Watch Battery"),
    CHARGING_STATE("Charging State"),
    CONNECTIVITY("Watch / Phone Connectivity"),
    BLOOD_PRESSURE("Blood Pressure")
}

data class WatchCapabilityDefinition(
    val type: WatchMeasurementType,
    val displayName: String,
    val watchSupports: Boolean,
    val hbAccess: CapabilityAccess,
    val api: String,
    val permissions: List<String>,
    val accessType: WatchAccessType,
    val restrictions: String,
    val canTransferToPhone: Boolean,
    val samplingFrequency: String,
    val batteryImpact: BatteryImpact,
    val notes: String
)

data class WatchMeasurementStatus(
    val definition: WatchCapabilityDefinition,
    val permissionState: PermissionState,
    val currentValue: String,
    val lastUpdatedAt: Long?,
    val statusNotes: String
)

data class WatchHealthData(
    val collectedAt: Long,
    val measurements: Map<WatchMeasurementType, WatchMeasurementStatus>
) {
    fun orderedMeasurements(): List<WatchMeasurementStatus> {
        return WatchCapabilityCatalog.orderedDefinitions.mapNotNull { measurements[it.type] }
    }
}
