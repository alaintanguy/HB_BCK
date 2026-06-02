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


        gpsCollector.startLocationUpdates(
            intervalMillis
        ) { latitude, longitude, altitude ->


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