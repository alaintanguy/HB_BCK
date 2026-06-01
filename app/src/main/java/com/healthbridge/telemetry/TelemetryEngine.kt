package com.healthbridge.telemetry

import android.content.Context
import android.util.Log
import com.healthbridge.firebase.FirebaseManager

class TelemetryEngine(
    private val context: Context,
    private val memberId: String
) {

    private val gpsCollector =
        GpsCollector(context)

    private var intervalMillis: Long = 5000

    fun start() {

        Log.d(
            "HB",
            "TelemetryEngine STARTED for $memberId"
        )

        gpsCollector.startLocationUpdates(
            intervalMillis
        ) { latitude, longitude, altitude ->

            Log.d(
                "HB",
                "REAL GPS UPDATE: $latitude , $longitude"
            )
            Log.d(
                "HB",
                "ALTITUDE: $altitude"
            )

            FirebaseManager.updateLocation(
                memberId,
                latitude,
                longitude,
                altitude
            )
        }
    }

  //  fun setInterval(milliseconds: Long) {

       // intervalMillis = milliseconds}
}