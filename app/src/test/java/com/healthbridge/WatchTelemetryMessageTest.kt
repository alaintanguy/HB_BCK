package com.healthbridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer

class WatchTelemetryMessageTest {

    @Test
    fun fromPayload_parsesCurrentWatchTelemetryFormat() {
        val payload = ByteBuffer.allocate(WatchTelemetryMessage.PAYLOAD_SIZE_BYTES)
            .putInt(53)
            .putInt(81)
            .putLong(1_234_567_890L)
            .array()

        val message = WatchTelemetryMessage.fromPayload(payload)

        assertNotNull(message)
        assertEquals(53, message?.heartRate)
        assertEquals(81, message?.watchBattery)
        assertEquals(1_234_567_890L, message?.timestamp)
    }

    @Test
    fun fromPayload_returnsNullForShortPayload() {
        val payload = ByteArray(15)

        val message = WatchTelemetryMessage.fromPayload(payload)

        assertNull(message)
    }
}
