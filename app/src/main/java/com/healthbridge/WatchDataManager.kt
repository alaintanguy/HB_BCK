package com.healthbridge

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * WatchDataManager
 * 
 * Manages watch telemetry data from Firebase.
 * 
 * PHASE 2 CORRECTIONS:
 * - Removed obsolete Firebase path: watch_data/latest
 * - Uses only M2 telemetry/watch path needed by M1
 * - No unnecessary Firebase listeners reading back watch telemetry
 * 
 * Data flows: Watch → Wear OS Data Layer → M2 → Firebase → M1
 */
object WatchDataManager {
    
    private const val TAG = "WatchDataManager"
    private val database = FirebaseDatabase.getInstance().reference
    
    // Listener references for cleanup
    private var watchTelemetryListener: ValueEventListener? = null
    private var watchTelemetryReference: DatabaseReference? = null
    
    /**
     * Start listening for watch telemetry updates from Firebase
     * Only listens to the M2 telemetry/watch path - no unnecessary listeners
     */
    fun startWatchTelemetryListener(
        memberId: String,
        onDataReceived: (battery: Int?, heartRate: Int?, timestamp: Long?) -> Unit,
        onError: (error: DatabaseError) -> Unit = {}
    ) {
        Log.d(TAG, "Starting watch telemetry listener for member: $memberId")
        
        // PHASE 2 CORRECTION #7: Use only M2 telemetry/watch path
        // Do NOT use obsolete watch_data/latest path
        watchTelemetryReference = database
            .child("groups")
            .child("family_001")
            .child("members")
            .child(memberId)
            .child("telemetry")
            .child("watch")
        
        watchTelemetryListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    Log.d(TAG, "Watch telemetry data received")
                    
                    val battery = snapshot.child("battery").getValue(Int::class.java)
                    val heartRate = snapshot.child("heartRate").getValue(Int::class.java)
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java)
                    
                    // Validate battery reading (0% is valid)
                    if (battery != null && battery >= 0) {
                        Log.d(TAG, "Valid battery reading: $battery%")
                    } else if (battery == null) {
                        Log.d(TAG, "No battery data available")
                    } else {
                        Log.w(TAG, "Invalid battery reading: $battery")
                    }
                    
                    // Only use heart rate if valid (> 0)
                    if (heartRate != null && heartRate > 0) {
                        Log.d(TAG, "Valid heart rate reading: $heartRate bpm")
                    } else if (heartRate == null) {
                        Log.d(TAG, "No heart rate data available (sensor not ready)")
                    } else {
                        Log.d(TAG, "Ignoring invalid heart rate reading: $heartRate")
                    }
                    
                    // PHASE 2 CORRECTION #6: Preserve watch telemetry packet timestamp
                    if (timestamp != null) {
                        Log.d(TAG, "Watch telemetry timestamp: $timestamp (for historical analysis)")
                    }
                    
                    onDataReceived(battery, heartRate, timestamp)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing watch telemetry data", e)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Watch telemetry listener cancelled: ${error.message}")
                onError(error)
            }
        }
        
        watchTelemetryReference?.addValueEventListener(watchTelemetryListener!!)
    }
    
    /**
     * Stop listening for watch telemetry updates
     */
    fun stopWatchTelemetryListener() {
        Log.d(TAG, "Stopping watch telemetry listener")
        
        watchTelemetryListener?.let { listener ->
            watchTelemetryReference?.removeEventListener(listener)
        }
        
        watchTelemetryListener = null
        watchTelemetryReference = null
    }
    
    /**
     * Read current watch telemetry once (non-listening)
     */
    fun readWatchTelemetry(
        memberId: String,
        onSuccess: (battery: Int?, heartRate: Int?, timestamp: Long?) -> Unit,
        onError: (error: DatabaseError) -> Unit = {}
    ) {
        Log.d(TAG, "Reading current watch telemetry for member: $memberId")
        
        database
            .child("groups")
            .child("family_001")
            .child("members")
            .child(memberId)
            .child("telemetry")
            .child("watch")
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    val battery = snapshot.child("battery").getValue(Int::class.java)
                    val heartRate = snapshot.child("heartRate").getValue(Int::class.java)
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java)
                    
                    onSuccess(battery, heartRate, timestamp)
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading watch telemetry", e)
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to read watch telemetry: ${error.message}")
                if (error is DatabaseError) {
                    onError(error)
                }
            }
    }
    
    /**
     * Update watch battery in Firebase (from wear device)
     */
    fun updateWatchBattery(memberId: String, battery: Int) {
        Log.d(TAG, "Updating watch battery for member $memberId: $battery%")
        
        database
            .child("groups")
            .child("family_001")
            .child("members")
            .child(memberId)
            .child("telemetry")
            .child("watch")
            .child("battery")
            .setValue(battery)
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to update watch battery", error)
            }
    }
    
    /**
     * Update watch heart rate in Firebase (from wear device)
     * Only updates if heart rate is valid (> 0)
     */
    fun updateWatchHeartRate(memberId: String, heartRate: Int) {
        // PHASE 2 CORRECTION #2: Don't publish HR=0
        if (heartRate <= 0) {
            Log.w(TAG, "Ignoring invalid heart rate update: $heartRate")
            return
        }
        
        Log.d(TAG, "Updating watch heart rate for member $memberId: $heartRate bpm")
        
        database
            .child("groups")
            .child("family_001")
            .child("members")
            .child(memberId)
            .child("telemetry")
            .child("watch")
            .child("heartRate")
            .setValue(heartRate)
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to update watch heart rate", error)
            }
    }
    
    /**
     * Update watch telemetry with timestamp (atomic operation)
     * Used to preserve the exact time the watch collected the telemetry
     */
    fun updateWatchTelemetry(
        memberId: String,
        battery: Int,
        heartRate: Int?,
        timestamp: Long
    ) {
        Log.d(TAG, "Updating watch telemetry for member $memberId")
        
        val updates = mutableMapOf<String, Any?>(
            "battery" to battery,
            "timestamp" to timestamp
        )
        
        // Only include heart rate if valid (> 0)
        if (heartRate != null && heartRate > 0) {
            updates["heartRate"] = heartRate
        }
        
        database
            .child("groups")
            .child("family_001")
            .child("members")
            .child(memberId)
            .child("telemetry")
            .child("watch")
            .updateChildren(updates.toMap())
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to update watch telemetry", error)
            }
    }
}
