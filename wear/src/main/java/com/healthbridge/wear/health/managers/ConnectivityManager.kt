package com.healthbridge.wear.health.managers

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import com.healthbridge.wear.health.WatchCapabilityCatalog
import com.healthbridge.wear.health.WatchHealthDataManager
import com.healthbridge.wear.health.WearLogTags
import com.healthbridge.wear.health.model.PermissionState
import com.healthbridge.wear.health.model.WatchMeasurementStatus
import com.healthbridge.wear.health.model.WatchMeasurementType

class WearConnectivityManager(context: Context) : WatchHealthDataManager {

    private val appContext = context.applicationContext
    private var currentValue = "Checking..."
    private var lastUpdatedAt: Long? = null
    private var notes = "Checking paired phone connectivity."
    private var onUpdate: (() -> Unit)? = null

    override fun start(onUpdate: (() -> Unit)?) {
        this.onUpdate = onUpdate
        refresh()
    }

    override fun refresh() {
        Wearable.getNodeClient(appContext).connectedNodes
            .addOnSuccessListener { nodes ->
                currentValue = if (nodes.isEmpty()) {
                    "No connected phone"
                } else {
                    "${nodes.size} connected node(s)"
                }
                notes = if (nodes.isEmpty()) {
                    "Wear Data Layer cannot currently reach a paired phone."
                } else {
                    "Wear Data Layer reports an active path to a paired phone."
                }
                lastUpdatedAt = System.currentTimeMillis()
                Log.d(WearLogTags.API, "Connectivity diagnostic: $currentValue")
                onUpdate?.invoke()
            }
            .addOnFailureListener { error ->
                currentValue = "Unavailable"
                notes = error.message ?: "Failed to query Wear Data Layer nodes."
                lastUpdatedAt = System.currentTimeMillis()
                Log.e(WearLogTags.API, "Connectivity diagnostic failed", error)
                onUpdate?.invoke()
            }
    }

    override fun statuses(): List<WatchMeasurementStatus> {
        val definition = WatchCapabilityCatalog.definitionFor(WatchMeasurementType.CONNECTIVITY)
        return listOf(
            WatchMeasurementStatus(
                definition = definition,
                permissionState = PermissionState.NOT_REQUIRED,
                currentValue = currentValue,
                lastUpdatedAt = lastUpdatedAt,
                statusNotes = notes
            )
        )
    }
}
