package com.healthbridge.telemetry

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource

class GpsCollector(
    private val context: Context
) {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        onLocation: (Double, Double) -> Unit
    ) {

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->

            if (location != null) {

                onLocation(
                    location.latitude,
                    location.longitude
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(
        intervalMillis: Long,
        onLocation: (Double, Double) -> Unit
    ) {

        val locationRequest =
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                intervalMillis
            ).build()

        val locationCallback =
            object : LocationCallback() {

                override fun onLocationResult(
                    result: LocationResult
                ) {

                    val location =
                        result.lastLocation

                    if (location != null) {

                        onLocation(
                            location.latitude,
                            location.longitude
                        )
                    }
                }
            }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }
}