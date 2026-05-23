package com.healthbridge.telemetry

import android.content.Context
import com.healthbridge.firebase.FirebaseManager

class TelemetryEngine(
    private val context: Context
) {

    private val gpsCollector =
        GpsCollector(context)

    private var intervalMillis: Long = 50000

    fun start() {
        android.util.Log.d(
            "HB_GPS",
            "TelemetryEngine STARTED"
        )

        gpsCollector.startLocationUpdates(
            intervalMillis
        ) { latitude, longitude ->
            android.util.Log.d(
                "HB_GPS",
                "GPS UPDATE: $latitude , $longitude"
            )

            FirebaseManager.updateLocation(
                memberId = "mary",
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