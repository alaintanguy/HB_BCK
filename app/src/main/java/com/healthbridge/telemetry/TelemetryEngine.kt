package com.healthbridge.telemetry

import android.content.Context
import com.healthbridge.firebase.FirebaseManager
import android.util.Log

class TelemetryEngine(
    private val context: Context,
    private val memberId: String
) {

    private val gpsCollector =
        GpsCollector(context)

    private var intervalMillis: Long = 60000

    fun start() {


        gpsCollector.startLocationUpdates(
            intervalMillis
        ) { latitude, longitude, altitude ->
            Log.d(
                "HB",
                "GPS UPDATE: $latitude , $longitude , $altitude"
            )

            FirebaseManager.updateTelemetry(
                memberId = memberId,
                latitude = latitude,
                longitude = longitude,
                altitude = altitude
            )
        }
    }

    fun setInterval(
        milliseconds: Long
    ) {
        intervalMillis = milliseconds
    }
}