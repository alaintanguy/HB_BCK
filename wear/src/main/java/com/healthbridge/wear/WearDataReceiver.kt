package com.healthbridge.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/**
 * WearDataReceiver
 * 
 * Receives data from M2 (patient phone) via Wear OS Data Layer
 * Validates battery readings with corrected logic (0% is valid)
 */
class WearDataReceiver : WearableListenerService() {
    
    companion object {
        private const val TAG = "WearDataReceiver"
        private const val WATCH_DATA_PATH = "/watch/telemetry"
    }
    
    override fun onDataChanged(dataEvents: com.google.android.gms.wearable.DataEventBuffer) {
        Log.d(TAG, "Data changed")
        
        for (dataEvent in dataEvents) {
            if (dataEvent.type == com.google.android.gms.wearable.DataEvent.TYPE_CHANGED) {
                val dataItem = dataEvent.dataItem
                
                if (dataItem.uri.path.startsWith(WATCH_DATA_PATH)) {
                    try {
                        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                        
                        // Extract battery reading
                        val battery = dataMap.getInt("battery", -1)
                        
                        // PHASE 2 CORRECTION #5: Accept 0% battery as valid
                        // Changed from: if (battery > 0)
                        // To: Accept any battery value >= 0 (0% is valid)
                        if (battery >= 0) {
                            Log.d(TAG, "Valid battery reading: $battery%")
                            handleBatteryUpdate(battery)
                        } else {
                            Log.w(TAG, "Invalid battery reading: $battery (unavailable/error)")
                        }
                        
                        // Extract heart rate if available
                        if (dataMap.containsKey("heartRate")) {
                            val heartRate = dataMap.getInt("heartRate", -1)
                            if (heartRate > 0) {
                                Log.d(TAG, "Valid heart rate reading: $heartRate bpm")
                                handleHeartRateUpdate(heartRate)
                            }
                        }
                        
                        // Extract timestamp for telemetry correlation
                        if (dataMap.containsKey("timestamp")) {
                            val timestamp = dataMap.getLong("timestamp", 0)
                            Log.d(TAG, "Telemetry timestamp: $timestamp")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing data change", e)
                    }
                }
            }
        }
    }
    
    private fun handleBatteryUpdate(battery: Int) {
        // Process battery update
        // This could update local UI, send alerts, etc.
        Log.d(TAG, "Processing battery update: $battery%")
    }
    
    private fun handleHeartRateUpdate(heartRate: Int) {
        // Process heart rate update
        // This could update local UI, trigger alerts, etc.
        Log.d(TAG, "Processing heart rate update: $heartRate bpm")
    }
}
