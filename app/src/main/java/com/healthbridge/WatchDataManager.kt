package com.healthbridge

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
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

        // Listen to the watch telemetry path where M2 publishes data received from Watch
        FirebaseManager.memberReference(memberId)
            .child("telemetry")
            .child("watch")
            .addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val heartRate = snapshot.child("heart_rate").getValue(Int::class.java) ?: 0
                            val battery = snapshot.child("battery").getValue(Int::class.java) ?: 0
                            val timestamp = snapshot.child("heart_rate_timestamp").getValue(Long::class.java)
                                ?: snapshot.child("battery_timestamp").getValue(Long::class.java)
                                ?: System.currentTimeMillis()

                            currentWatchHeartRate = heartRate
                            currentWatchBattery = battery
                            lastWatchDataTimestamp = timestamp

                            Log.d(
                                TAG,
                                "Watch data received: HR=$currentWatchHeartRate, Battery=$currentWatchBattery"
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing watch data", e)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Watch data listener failed", error.toException())
                    }
                }
            )
    }

    fun getWatchHeartRate(): Int = currentWatchHeartRate
    fun getWatchBattery(): Int = currentWatchBattery
    fun getLastWatchDataTimestamp(): Long = lastWatchDataTimestamp
}

