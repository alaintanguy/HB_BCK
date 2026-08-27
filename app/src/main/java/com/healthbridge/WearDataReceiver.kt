package com.healthbridge

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearDataReceiver : WearableListenerService() {

    companion object {
        private const val TAG = "HB-WEAR-PHONE"
        private const val WATCH_DATA_MESSAGE_PATH = "/healthbridge/watch/telemetry"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        Log.d(TAG, "Received watch message path=${messageEvent.path}")

        if (messageEvent.path != WATCH_DATA_MESSAGE_PATH) {
            return
        }

        try {
            val telemetry = WatchTelemetryMessage.fromPayload(messageEvent.data)
            if (telemetry == null) {
                Log.w(TAG, "Invalid watch telemetry payload size=${messageEvent.data.size}")
                return
            }

            Log.d(
                TAG,
                "Watch telemetry received: heartRate=${telemetry.heartRate}, watchBattery=${telemetry.watchBattery}, timestamp=${telemetry.timestamp}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing watch telemetry", e)
        }
    }
}
