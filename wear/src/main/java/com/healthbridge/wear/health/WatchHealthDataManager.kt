package com.healthbridge.wear.health

import com.healthbridge.wear.health.model.WatchMeasurementStatus

interface WatchHealthDataManager {
    fun start(onUpdate: (() -> Unit)? = null) {}
    fun refresh() {}
    fun stop() {}
    fun statuses(): List<WatchMeasurementStatus>
}
