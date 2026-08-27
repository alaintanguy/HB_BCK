package com.healthbridge.wear.health

import com.healthbridge.wear.health.model.BatteryImpact
import com.healthbridge.wear.health.model.CapabilityAccess
import com.healthbridge.wear.health.model.WatchAccessType
import com.healthbridge.wear.health.model.WatchCapabilityDefinition
import com.healthbridge.wear.health.model.WatchMeasurementType

/**
 * Phase 2A capability table for Samsung Galaxy Watch 4 Classic / Wear OS.
 *
 * Each entry documents whether the watch hardware supports the measurement,
 * whether HealthBridge can access it programmatically with official public APIs,
 * which API family is involved, the expected access mode, restrictions, and
 * recommended polling/sampling guidance for diagnostics.
 */
object WatchCapabilityCatalog {

    val orderedDefinitions: List<WatchCapabilityDefinition> = listOf(
        capability(
            type = WatchMeasurementType.HEART_RATE,
            watchSupports = true,
            hbAccess = CapabilityAccess.ACCESSIBLE,
            api = "Wear OS Health Services / Android SensorManager",
            accessType = WatchAccessType.BOTH,
            permissions = listOf(
                "android.permission.BODY_SENSORS",
                "android.permission.BODY_SENSORS_BACKGROUND",
                "android.permission.health.READ_HEART_RATE"
            ),
            restrictions = "Runtime sensor permission is required; API 33+ passive background monitoring also needs BODY_SENSORS_BACKGROUND, which is a Settings-only grant.",
            samplingFrequency = "1 sec active / passive batched",
            batteryImpact = BatteryImpact.LOW,
            canTransferToPhone = true,
            notes = "Public live heart-rate access is supported and remains the primary watch telemetry signal."
        ),
        capability(
            type = WatchMeasurementType.RESTING_HEART_RATE,
            watchSupports = true,
            hbAccess = CapabilityAccess.RESTRICTED,
            api = "Health Connect / Samsung Health",
            accessType = WatchAccessType.HISTORICAL,
            permissions = listOf("android.permission.health.READ_RESTING_HEART_RATE"),
            restrictions = "Historical-only when another platform syncs derived resting-HR data; no public live watch sensor API.",
            samplingFrequency = "Daily",
            batteryImpact = BatteryImpact.NOT_APPLICABLE,
            canTransferToPhone = true,
            notes = "Treat as a derived summary metric, not as a raw live measurement."
        ),
        capability(
            type = WatchMeasurementType.HEART_RATE_VARIABILITY,
            watchSupports = true,
            hbAccess = CapabilityAccess.RESTRICTED,
            api = "Health Connect / Samsung Health",
            accessType = WatchAccessType.HISTORICAL,
            permissions = listOf("android.permission.health.READ_HEART_RATE_VARIABILITY"),
            restrictions = "No public real-time Galaxy Watch 4 API for third-party apps; historical sync availability depends on Samsung Health or another platform writing RMSSD records.",
            samplingFrequency = "After sleep / workout",
            batteryImpact = BatteryImpact.NOT_APPLICABLE,
            canTransferToPhone = true,
            notes = "Use only when a connected health platform contributes HRV records."
        ),
        capability(
            type = WatchMeasurementType.ECG,
            watchSupports = true,
            hbAccess = CapabilityAccess.RESTRICTED,
            api = "Samsung Health Monitor / Health Connect history",
            accessType = WatchAccessType.HISTORICAL,
            permissions = listOf("android.permission.health.READ_ELECTROCARDIOGRAM"),
            restrictions = "Real-time third-party ECG capture is unavailable; Samsung Health Monitor is region-gated and any readable ECG is historical only after user-initiated measurements.",
            samplingFrequency = "User-initiated / post-sync",
            batteryImpact = BatteryImpact.NOT_APPLICABLE,
            canTransferToPhone = true,
            notes = "Do not simulate ECG data; only historical records may become visible through Samsung/Health Connect pathways."
        ),
        capability(
            type = WatchMeasurementType.IRREGULAR_RHYTHM,
            watchSupports = true,
            hbAccess = CapabilityAccess.UNAVAILABLE,
            api = "Samsung Health Monitor",
            accessType = WatchAccessType.UNAVAILABLE,
            restrictions = "Regional feature availability and Samsung regulatory workflows apply; no public AF/IHRN API for third-party watch apps.",
            samplingFrequency = "System-managed",
            batteryImpact = BatteryImpact.NOT_APPLICABLE,
            canTransferToPhone = false,
            notes = "The watch may detect irregular rhythm for Samsung apps, but HealthBridge cannot read those results directly."
        ),
        capability(
            type = WatchMeasurementType.SPO2,
            watchSupports = true,
            hbAccess = CapabilityAccess.RESTRICTED,
            api = "Health Connect / Samsung Health",
            accessType = WatchAccessType.HISTORICAL,
            permissions = listOf(
                "android.permission.BODY_SENSORS",
                "android.permission.health.READ_OXYGEN_SATURATION"
            ),
            restrictions = "Public live SpO2 streaming is not exposed by Wear OS Health Services on Galaxy Watch 4; only historical records are available through Samsung Health / Health Connect.",
            samplingFrequency = "Post-measurement / sleep sync",
            batteryImpact = BatteryImpact.MEDIUM,
            canTransferToPhone = true,
            notes = "Show as restricted until a historical Health Connect or Samsung data reader is integrated."
        ),
        capability(
            type = WatchMeasurementType.SLEEP,
            watchSupports = true,
            hbAccess = CapabilityAccess.RESTRICTED,
            api = "Wear OS Health Services / Health Connect",
            accessType = WatchAccessType.BOTH,
            permissions = listOf(
                "android.permission.ACTIVITY_RECOGNITION",
                "android.permission.health.READ_SLEEP"
            ),
            restrictions = "Only binary awake/asleep state is available in real time; sleep stages and quality metrics arrive historically after Samsung Health writes completed sessions.",
            samplingFrequency = "Event-driven asleep/awake; post-sleep sync",
            batteryImpact = BatteryImpact.NOT_APPLICABLE,
            canTransferToPhone = true,
            notes = "Keep real-time user-activity state separate from historical sleep-stage summaries."
        ),
        capability(
            type = WatchMeasurementType.STEPS,
            watchSupports = true,
            hbAccess = CapabilityAccess.ACCESSIBLE,
            api = "Wear OS Health Services / Android SensorManager",
            accessType = WatchAccessType.BOTH,
            permissions = listOf(
                "android.permission.ACTIVITY_RECOGNITION",
                "android.permission.health.READ_STEPS"
            ),
            restrictions = "Health Services passive updates are batched; raw SensorManager counters are cumulative since boot.",
            samplingFrequency = "Passive batched / 30-60 sec",
            batteryImpact = BatteryImpact.VERY_LOW,
            canTransferToPhone = true,
            notes = "This is the lowest-cost movement metric to expand after heart rate."
        ),
        capability(
            type = WatchMeasurementType.DISTANCE,
            watchSupports = true,
            hbAccess = CapabilityAccess.ACCESSIBLE,
            api = "Wear OS Health Services / Health Connect",
            accessType = WatchAccessType.BOTH,
            permissions = listOf(
                "android.permission.ACTIVITY_RECOGNITION",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.health.READ_DISTANCE"
            ),
            restrictions = "Passive distance is usually step-based; GPS-enhanced distance requires an exercise session and location permission.",
            samplingFrequency = "Passive batched / continuous in exercise",
            batteryImpact = BatteryImpact.LOW,
            canTransferToPhone = true,
            notes = "Distance should be treated as an interpreted activity metric."
        ),
        capability(
            type = WatchMeasurementType.CALORIES,
            watchSupports = true,
            hbAccess = CapabilityAccess.ACCESSIBLE,
            api = "Wear OS Health Services / Health Connect",
            accessType = WatchAccessType.BOTH,
            permissions = listOf(
                "android.permission.ACTIVITY_RECOGNITION",
                "android.permission.health.READ_TOTAL_CALORIES_BURNED"
            ),
            restrictions = "Calories are derived estimates and may be surfaced as deltas/totals depending on passive versus exercise mode.",
            samplingFrequency = "Passive batched / continuous in exercise",
            batteryImpact = BatteryImpact.LOW,
            canTransferToPhone = true,
            notes = "Use cautiously because calculations vary by platform."
        ),
        capability(
            type = WatchMeasurementType.ACTIVITY_CLASSIFICATION,
            watchSupports = true,
            hbAccess = CapabilityAccess.RESTRICTED,
            api = "Health Services / Activity Recognition APIs",
            accessType = WatchAccessType.BOTH,
            permissions = listOf(
                "android.permission.ACTIVITY_RECOGNITION",
                "android.permission.health.READ_EXERCISE"
            ),
            restrictions = "Wear OS can surface awake/asleep and exercise/activity context, but not every Samsung proprietary classifier is public.",
            samplingFrequency = "On change / 1 min",
            batteryImpact = BatteryImpact.LOW,
            canTransferToPhone = true,
            notes = "Keep separate from raw sensors so clinical interpretation stays configurable."
        ),
        capability(
            type = WatchMeasurementType.WORKOUT_DATA,
            watchSupports = true,
            hbAccess = CapabilityAccess.ACCESSIBLE,
            api = "Wear OS Health Services ExerciseClient / Health Connect",
            accessType = WatchAccessType.BOTH,
            permissions = listOf(
                "android.permission.BODY_SENSORS",
                "android.permission.ACTIVITY_RECOGNITION",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.health.READ_EXERCISE"
            ),
            restrictions = "Only one exercise session can be active at a time; Samsung Health or another app may already own the device exercise session.",
            samplingFrequency = "During session / post-session",
            batteryImpact = BatteryImpact.MEDIUM,
            canTransferToPhone = true,
            notes = "Future-proof the protocol for session metadata instead of hardcoding one workout schema."
        ),
        capability(
            type = WatchMeasurementType.ACCELEROMETER,
            watchSupports = true,
            hbAccess = CapabilityAccess.ACCESSIBLE,
            api = "Android SensorManager",
            accessType = WatchAccessType.REAL_TIME,
            restrictions = "Raw motion is public, but interpreted fall detection remains a separate problem.",
            samplingFrequency = "1-10 sec windows",
            batteryImpact = BatteryImpact.MEDIUM,
            canTransferToPhone = true,
            notes = "Sample sparingly outside of explicit diagnostics or future fall algorithms."
        ),
        capability(
            type = WatchMeasurementType.GYROSCOPE,
            watchSupports = true,
            hbAccess = CapabilityAccess.ACCESSIBLE,
            api = "Android SensorManager",
            accessType = WatchAccessType.REAL_TIME,
            restrictions = "No extra runtime permission is required, but continuous collection has a noticeable battery cost.",
            samplingFrequency = "1-10 sec windows",
            batteryImpact = BatteryImpact.MEDIUM,
            canTransferToPhone = true,
            notes = "Useful for motion context, not as a stand-alone clinical signal."
        ),
        capability(
            type = WatchMeasurementType.FALL_SIGNALS,
            watchSupports = true,
            hbAccess = CapabilityAccess.RESTRICTED,
            api = "Samsung fall detection private API / Android raw sensors",
            accessType = WatchAccessType.REAL_TIME,
            restrictions = "Samsung fall-detection events are private; HealthBridge can only read raw accelerometer/gyroscope data and must implement its own algorithm with foreground-service power tradeoffs.",
            samplingFrequency = "High-frequency windows only",
            batteryImpact = BatteryImpact.HIGH,
            canTransferToPhone = true,
            notes = "Implement fall logic later from raw sensors, not from hidden Samsung event feeds."
        ),
        capability(
            type = WatchMeasurementType.BATTERY_LEVEL,
            watchSupports = true,
            hbAccess = CapabilityAccess.ACCESSIBLE,
            api = "BatteryManager / ACTION_BATTERY_CHANGED",
            accessType = WatchAccessType.REAL_TIME,
            restrictions = "None",
            samplingFrequency = "1-5 min",
            batteryImpact = BatteryImpact.VERY_LOW,
            canTransferToPhone = true,
            notes = "Existing telemetry already reports this field and remains unchanged."
        ),
        capability(
            type = WatchMeasurementType.CHARGING_STATE,
            watchSupports = true,
            hbAccess = CapabilityAccess.ACCESSIBLE,
            api = "BatteryManager / ACTION_BATTERY_CHANGED",
            accessType = WatchAccessType.REAL_TIME,
            restrictions = "None",
            samplingFrequency = "On change / 1 min",
            batteryImpact = BatteryImpact.VERY_LOW,
            canTransferToPhone = true,
            notes = "Helpful for diagnosing why telemetry pauses or battery trends change."
        ),
        capability(
            type = WatchMeasurementType.CONNECTIVITY,
            watchSupports = true,
            hbAccess = CapabilityAccess.ACCESSIBLE,
            api = "Wear OS Data Layer NodeClient / ConnectivityManager",
            accessType = WatchAccessType.REAL_TIME,
            restrictions = "Phone reachability reflects transport state only and may differ from general Wi-Fi/cellular availability.",
            samplingFrequency = "On demand / 1 min",
            batteryImpact = BatteryImpact.VERY_LOW,
            canTransferToPhone = false,
            notes = "Useful for operational diagnostics while preserving the existing telemetry payload.",
            permissions = listOf("android.permission.ACCESS_NETWORK_STATE")
        ),
        capability(
            type = WatchMeasurementType.BLOOD_PRESSURE,
            watchSupports = true,
            hbAccess = CapabilityAccess.RESTRICTED,
            api = "Samsung Health Monitor / Health Connect",
            accessType = WatchAccessType.HISTORICAL,
            permissions = listOf("android.permission.health.READ_BLOOD_PRESSURE"),
            restrictions = "Samsung Health Monitor availability is region-limited, requires cuff calibration, and does not expose a public live third-party watch API.",
            samplingFrequency = "User-initiated / post-sync",
            batteryImpact = BatteryImpact.NOT_APPLICABLE,
            canTransferToPhone = true,
            notes = "Treat blood-pressure access as historical-only unless Samsung publishes a public integration path."
        )
    )
    private val definitionsByType: Map<WatchMeasurementType, WatchCapabilityDefinition> =
        orderedDefinitions.associateBy { it.type }

    fun definitionFor(type: WatchMeasurementType): WatchCapabilityDefinition {
        return definitionsByType[type]
            ?: throw IllegalArgumentException("Missing watch capability definition for ${type.name}")
    }

    private fun capability(
        type: WatchMeasurementType,
        watchSupports: Boolean,
        hbAccess: CapabilityAccess,
        api: String,
        accessType: WatchAccessType,
        restrictions: String,
        samplingFrequency: String,
        batteryImpact: BatteryImpact,
        canTransferToPhone: Boolean,
        notes: String,
        permissions: List<String> = emptyList()
    ): WatchCapabilityDefinition {
        return WatchCapabilityDefinition(
            type = type,
            displayName = type.displayName,
            watchSupports = watchSupports,
            hbAccess = hbAccess,
            api = api,
            permissions = permissions,
            accessType = accessType,
            restrictions = restrictions,
            canTransferToPhone = canTransferToPhone,
            samplingFrequency = samplingFrequency,
            batteryImpact = batteryImpact,
            notes = notes
        )
    }
}
