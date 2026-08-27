package com.healthbridge.wear.health

import com.healthbridge.wear.health.model.PermissionState
import com.healthbridge.wear.health.model.WatchMeasurementStatus
import com.healthbridge.wear.health.model.WatchMeasurementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchHealthDataCollectorTest {

    @Test
    fun snapshot_merges_manager_measurements_by_type() {
        val heartRateStatus = WatchMeasurementStatus(
            definition = WatchCapabilityCatalog.definitionFor(WatchMeasurementType.HEART_RATE),
            permissionState = PermissionState.GRANTED,
            currentValue = "72 BPM",
            lastUpdatedAt = 1000L,
            statusNotes = "ok"
        )
        val batteryStatus = WatchMeasurementStatus(
            definition = WatchCapabilityCatalog.definitionFor(WatchMeasurementType.BATTERY_LEVEL),
            permissionState = PermissionState.NOT_REQUIRED,
            currentValue = "88 %",
            lastUpdatedAt = 2000L,
            statusNotes = "ok"
        )

        val collector = WatchHealthDataCollector(
            listOf(
                FakeManager(listOf(heartRateStatus)),
                FakeManager(listOf(batteryStatus))
            )
        )

        val snapshot = collector.snapshot()

        assertEquals("72 BPM", snapshot.measurements[WatchMeasurementType.HEART_RATE]?.currentValue)
        assertEquals("88 %", snapshot.measurements[WatchMeasurementType.BATTERY_LEVEL]?.currentValue)
    }

    @Test
    fun capability_catalog_covers_requested_measurements() {
        val measurements = WatchCapabilityCatalog.orderedDefinitions.map { it.type }.toSet()

        assertTrue(measurements.contains(WatchMeasurementType.HEART_RATE))
        assertTrue(measurements.contains(WatchMeasurementType.SPO2))
        assertTrue(measurements.contains(WatchMeasurementType.SLEEP))
        assertTrue(measurements.contains(WatchMeasurementType.STEPS))
        assertTrue(measurements.contains(WatchMeasurementType.ECG))
        assertTrue(measurements.contains(WatchMeasurementType.CONNECTIVITY))
    }

    private class FakeManager(
        private val returnedStatuses: List<WatchMeasurementStatus>
    ) : WatchHealthDataManager {
        override fun statuses(): List<WatchMeasurementStatus> = returnedStatuses
    }
}
