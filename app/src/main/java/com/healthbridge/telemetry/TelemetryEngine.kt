package com.healthbridge.telemetry

import android.location.Location
import android.util.Log
import android.content.Context

import com.healthbridge.firebase.FirebaseManager

class TelemetryEngine(
    private val context: Context,
    private val memberId: String
) {

    companion object {

        private const val NORMAL_INTERVAL =
            60000L

        private const val EMERGENCY_INTERVAL =
            10000L
    }

    private val gpsCollector =
        GpsCollector(context)

    private var currentInterval =
        NORMAL_INTERVAL

    private var emergencyMode = false

    private val accuracyLimit = 15f

    private val movementThreshold = 10f

    private var lastLatitude: Double? = null

    private var lastLongitude: Double? = null

    fun setEmergencyMode(
        enabled: Boolean
    ) {

        emergencyMode = enabled

        currentInterval =
            if (enabled) {

                EMERGENCY_INTERVAL

            } else {

                NORMAL_INTERVAL
            }
    }

    fun start() {

        gpsCollector.startLocationUpdates(
            currentInterval
        ) {
                latitude,
                longitude,
                altitude,
                accuracy,
                speed ->

            Log.d(
                "HB",
                "GPS UPDATE: $latitude , $longitude"
            )

          //  if (accuracy > accuracyLimit) {

               // Log.d("HB","IGNORED: BAD ACCURACY = $accuracy")

                //return@startLocationUpdates }

            val previousLat = lastLatitude
            val previousLng = lastLongitude

            if (
                previousLat != null &&
                previousLng != null
            ) {

                val results = FloatArray(1)

                Location.distanceBetween(
                    previousLat,
                    previousLng,
                    latitude,
                    longitude,
                    results
                )

                val distanceMeters = results[0]

                if (
                    distanceMeters <
                    movementThreshold
                ) {

                    Log.d(
                        "HB",
                        "SMALL MOVEMENT -> HEARTBEAT ONLY = $distanceMeters"
                    )
                }
            }

            FirebaseManager.updateTelemetry(
                memberId,
                latitude,
                longitude,
                altitude,
                accuracy,
                speed
            )

            lastLatitude = latitude
            lastLongitude = longitude
        }
    }
}