package com.healthbridge

import android.util.Log
import com.healthbridge.firebase.FirebaseManager

class WatchDataManager(
    private val memberId: String
) {

    companion object {
        private const val TAG = "WatchDataManager"
    }

    private var currentWatchHeartRate: Int = 0
    private var currentWatchBattery: Int = 0
    private var lastWatchDataTimestamp: Long = 0

    fun startListeningForWatchData() {
        Log.d(TAG, "Starting to listen for watch data")

        FirebaseManager.listenForWatchData { heartRate, watchBattery, timestamp ->
            currentWatchHeartRate = heartRate
            currentWatchBattery = watchBattery
            lastWatchDataTimestamp = timestamp

            // Update the member's telemetry with watch data
            if (currentWatchHeartRate > 0) {
                FirebaseManager.updateWatchHeartRate(memberId, currentWatchHeartRate, timestamp)
            }

            if (currentWatchBattery > 0) {
                FirebaseManager.updateWatchBattery(memberId, currentWatchBattery, timestamp)
            }

            Log.d(
                TAG,
                "Watch data received and forwarded: HR=$currentWatchHeartRate, Battery=$currentWatchBattery"
            )
        }
    }

    fun getWatchHeartRate(): Int = currentWatchHeartRate
    fun getWatchBattery(): Int = currentWatchBattery
    fun getLastWatchDataTimestamp(): Long = lastWatchDataTimestamp
}
