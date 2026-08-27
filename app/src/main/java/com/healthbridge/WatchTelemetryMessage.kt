package com.healthbridge

import java.nio.ByteBuffer

data class WatchTelemetryMessage(
    val heartRate: Int,
    val watchBattery: Int,
    val timestamp: Long
) {
    companion object {
        const val PAYLOAD_SIZE_BYTES = 16

        fun fromPayload(payload: ByteArray): WatchTelemetryMessage? {
            if (payload.size < PAYLOAD_SIZE_BYTES) {
                return null
            }

            val buffer = ByteBuffer.wrap(payload)
            return WatchTelemetryMessage(
                heartRate = buffer.int,
                watchBattery = buffer.int,
                timestamp = buffer.long
            )
        }
    }
}
