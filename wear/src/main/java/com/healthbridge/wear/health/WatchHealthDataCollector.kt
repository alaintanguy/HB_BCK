package com.healthbridge.wear.health

import android.content.Context
import com.healthbridge.wear.health.managers.BatteryStatusManager
import com.healthbridge.wear.health.managers.CardiovascularHistoryManager
import com.healthbridge.wear.health.managers.ExerciseSummaryManager
import com.healthbridge.wear.health.managers.HeartRateManager
import com.healthbridge.wear.health.managers.MotionSensorManager
import com.healthbridge.wear.health.managers.SleepManager
import com.healthbridge.wear.health.managers.SpO2Manager
import com.healthbridge.wear.health.managers.StepsManager
import com.healthbridge.wear.health.managers.WearConnectivityManager
import com.healthbridge.wear.health.model.WatchHealthData

class WatchHealthDataCollector(
    private val managers: List<WatchHealthDataManager>
) {

    constructor(context: Context) : this(defaultManagers(context.applicationContext))

    fun start(onUpdate: (() -> Unit)? = null) {
        managers.forEach { it.start(onUpdate) }
    }

    fun refresh() {
        managers.forEach { it.refresh() }
    }

    fun stop() {
        managers.forEach { it.stop() }
    }

    fun snapshot(): WatchHealthData {
        val orderedStatuses = managers
            .flatMap { it.statuses() }
            .associateBy { it.definition.type }
        return WatchHealthData(
            collectedAt = System.currentTimeMillis(),
            measurements = orderedStatuses
        )
    }

    companion object {
        private fun defaultManagers(context: Context): List<WatchHealthDataManager> = listOf(
            HeartRateManager(context),
            StepsManager(context),
            MotionSensorManager(context),
            BatteryStatusManager(context),
            WearConnectivityManager(context),
            SpO2Manager(),
            CardiovascularHistoryManager(),
            SleepManager(),
            ExerciseSummaryManager()
        )
    }
}
