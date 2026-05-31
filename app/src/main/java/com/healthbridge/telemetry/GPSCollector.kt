package com.healthbridge.telemetry

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log

import com.google.android.gms.location.*

class GpsCollector(
    private val context: Context
) {

    private val fusedLocationClient =
        LocationServices
            .getFusedLocationProviderClient(context)

    private var locationCallback:
            LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(
        intervalMillis: Long,
        onLocation: (
            Double,
            Double
        ) -> Unit
    ) {

        Log.d(
            "HB",
            "START LOCATION UPDATES"
        )

        val locationRequest =
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                intervalMillis
            )
                .setMinUpdateIntervalMillis(2000)
                .setWaitForAccurateLocation(true)
                .build()

        locationCallback =
            object : LocationCallback() {

                override fun onLocationResult(
                    result: LocationResult
                ) {

                    val location =
                        result.lastLocation

                    if (location != null) {

                        Log.d(
                            "HB",
                            "GPS CALLBACK RECEIVED"
                        )

                        Log.d(
                            "HB",
                            "REAL GPS: " +
                                    "${location.latitude} , " +
                                    "${location.longitude}"
                        )

                        onLocation(
                            location.latitude,
                            location.longitude
                        )
                    }
                }
            }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {

        if (locationCallback != null) {

            fusedLocationClient
                .removeLocationUpdates(
                    locationCallback!!
                )
        }
    }
}
