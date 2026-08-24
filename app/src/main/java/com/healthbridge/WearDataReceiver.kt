package com.healthbridge

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.healthbridge.firebase.FirebaseManager
import java.nio.ByteBuffer

class WearDataReceiver : WearableListenerService() {

    companion object {
        private const val TAG = "WearDataReceiver"
        private const val WATCH_DATA_MESSAGE_PATH = "/healthbridge/watch/telemetry"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        Log.d(TAG, "Received message: ${messageEvent.path}")

        if (messageEvent.path == WATCH_DATA_MESSAGE_PATH) {
            try {
                // Parse message payload: heartRate (4 bytes) + battery (4 bytes) + timestamp (8 bytes)
                val payload = messageEvent.data
                if (payload.size >= 16) {
                    val buffer = ByteBuffer.wrap(payload)
                    val heartRate = buffer.int
                    val battery = buffer.int
                    val timestamp = buffer.long

                    Log.d(TAG, "Watch telemetry received: HR=$heartRate, Battery=$battery, Timestamp=$timestamp")

                    // Update Firebase with watch data
                    // M2 is the patient, so write to M2's telemetry
                    if (MainActivity.MEMBER_ID == "M2") {
                        if (heartRate > 0) {
                            FirebaseManager.updateWatchHeartRate("M2", heartRate, timestamp)
                        }
                        if (battery > 0) {
                            FirebaseManager.updateWatchBattery("M2", battery, timestamp)
                        }
                    }
                } else {
                    Log.w(TAG, "Invalid payload size: ${payload.size}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing watch data", e)
            }
        }
    }
}
