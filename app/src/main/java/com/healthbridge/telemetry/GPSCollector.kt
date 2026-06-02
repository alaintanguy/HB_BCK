package com.healthbridge.telemetry

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper

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
            Double,
            Double
        ) -> Unit
    ) {


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




                        onLocation(
                            location.latitude,
                            location.longitude,
                            location.altitude
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
