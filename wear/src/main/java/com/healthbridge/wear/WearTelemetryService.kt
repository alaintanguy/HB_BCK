package com.healthbridge.wear

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WearTelemetryService
 * 
 * Responsible for collecting telemetry from the Wear OS device:
 * - Battery level (0% to 100% are all valid readings)
 * - Heart rate (only valid readings > 0)
 * 
 * Sends telemetry to Firebase via Data Layer every 60 seconds.
 * Thread is properly lifecycle-managed and stops on onDestroy().
 */
class WearTelemetryService : Service() {
    
    companion object {
        private const val TAG = "WearTelemetryService"
        private const val TELEMETRY_INTERVAL_MS = 60000L // 60 seconds
    }
    
    private val isServiceRunning = AtomicBoolean(false)
    private var telemetryThread: Thread? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // Heart rate state management
    private var latestHeartRate: Int = 0
    private var hasValidHeartRate = false
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "WearTelemetryService starting")
        
        if (!isServiceRunning.getAndSet(true)) {
            startTelemetryLoop()
        }
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "WearTelemetryService destroying")
        stopTelemetryLoop()
        isServiceRunning.set(false)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    /**
     * Start the telemetry collection loop in a background thread
     * Thread will be stopped when onDestroy() is called
     */
    private fun startTelemetryLoop() {
        telemetryThread = Thread {
            Log.d(TAG, "Telemetry loop thread started")
            
            try {
                while (isServiceRunning.get()) {
                    try {
                        collectAndSendTelemetry()
                        Thread.sleep(TELEMETRY_INTERVAL_MS)
                    } catch (e: InterruptedException) {
                        Log.d(TAG, "Telemetry loop interrupted")
                        break
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in telemetry loop", e)
                    }
                }
            } finally {
                Log.d(TAG, "Telemetry loop thread stopping")
            }
        }.apply {
            isDaemon = false
            start()
        }
    }
    
    /**
     * Stop the telemetry collection loop safely
     */
    private fun stopTelemetryLoop() {
        isServiceRunning.set(false)
        telemetryThread?.interrupt()
        
        // Wait for thread to finish (with timeout)
        try {
            telemetryThread?.join(5000)
        } catch (e: InterruptedException) {
            Log.w(TAG, "Interrupted while waiting for telemetry thread to stop")
        }
    }
    
    /**
     * Collect current telemetry and send to Firebase
     */
    private fun collectAndSendTelemetry() {
        val battery = getBatteryLevel()
        
        // Only send heart rate if we have a valid reading (> 0)
        val shouldSendHeartRate = hasValidHeartRate && latestHeartRate > 0
        
        val timestamp = System.currentTimeMillis()
        
        Log.d(TAG, "Telemetry: battery=$battery, heartRate=${if (shouldSendHeartRate) latestHeartRate else "unavailable"}, timestamp=$timestamp")
        
        // Send telemetry to Firebase
        sendTelemetryToFirebase(
            battery = battery,
            heartRate = if (shouldSendHeartRate) latestHeartRate else null,
            timestamp = timestamp
        )
    }
    
    /**
     * Get battery level from the device
     * 
     * PHASE 2 CORRECTION #1: Accept battery 0% as a valid reading
     * Logic: level >= 0 && scale > 0
     * 
     * Returns battery percentage (0-100), or -1 if unavailable/error
     */
    fun getBatteryLevel(): Int {
        return try {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            if (batteryManager != null) {
                val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                val scale = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
                
                // Accept level 0 as valid if scale > 0
                if (level >= 0 && scale > 0) {
                    (level * 100) / scale
                } else {
                    -1 // unavailable
                }
            } else {
                // Fallback: use IntentFilter for battery info
                val intent = registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                
                // PHASE 2 CORRECTION: Accept level 0 as valid (level >= 0 && scale > 0)
                if (level >= 0 && scale > 0) {
                    (level * 100) / scale
                } else {
                    -1 // unavailable
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery level", e)
            -1 // unavailable/error
        }
    }
    
    /**
     * Update the latest heart rate reading
     * Only mark as valid if the reading is > 0
     * 
     * PHASE 2 CORRECTION #2: Don't publish HR=0
     */
    fun updateHeartRate(heartRate: Int) {
        if (heartRate > 0) {
            latestHeartRate = heartRate
            hasValidHeartRate = true
            Log.d(TAG, "Heart rate updated: $heartRate bpm")
        } else {
            Log.d(TAG, "Heart rate reading <= 0, not marking as valid: $heartRate")
            // Don't update hasValidHeartRate, keep previous valid state
        }
    }
    
    /**
     * Send telemetry data to Firebase
     * Preserves the watch telemetry packet timestamp
     */
    private fun sendTelemetryToFirebase(
        battery: Int,
        heartRate: Int?,
        timestamp: Long
    ) {
        try {
            val database = FirebaseDatabase.getInstance().reference
            
            // Use Firebase path: groups/family_001/members/M2/telemetry/watch
            // Preserve the packet timestamp for later historical analysis
            val watchTelemetry = mutableMapOf<String, Any?>(
                "battery" to battery,
                "timestamp" to timestamp
            )
            
            // Only include heartRate if we have a valid reading
            if (heartRate != null && heartRate > 0) {
                watchTelemetry["heartRate"] = heartRate
            }
            
            // Write to Firebase (M2 is the patient device)
            // In a real implementation, this would sync via Data Layer to the phone
            database.child("groups")
                .child("family_001")
                .child("members")
                .child("M2")
                .child("telemetry")
                .child("watch")
                .updateChildren(watchTelemetry.toMap())
                .addOnSuccessListener {
                    Log.d(TAG, "Telemetry sent to Firebase")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to send telemetry to Firebase", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending telemetry to Firebase", e)
        }
    }
}
