package com.healthbridge.telemetry

import android.content.Context
import com.healthbridge.firebase.FirebaseManager

class TelemetryEngine(
    private val context: Context
) {

    private val gpsCollector =
        GpsCollector(context)

    private var intervalMillis: Long = 60000

    fun start() {

        gpsCollector.startLocationUpdates(
            intervalMillis
        ) { latitude, longitude ->

            FirebaseManager.updateLocation(
                memberId = "alain",
                latitude,
                longitude
            )
        }
    }

    fun setInterval(
        milliseconds: Long
    ) {

        intervalMillis = milliseconds
    }
}