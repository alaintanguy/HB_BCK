package com.healthbridge.telemetry

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log

import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

import com.healthbridge.firebase.FirebaseManager

class TelemetryEngine(
    private val context: Context
) {

    private val fusedLocationClient =
        LocationServices
            .getFusedLocationProviderClient(context)

    private var locationCallback:
            LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun start() {

        Log.d(
            "HB",
            "TelemetryEngine START"
        )

        val locationRequest =
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10000L
            )
                .setMinUpdateIntervalMillis(
                    5000L
                )
                .build()

        locationCallback =
            object : LocationCallback() {

                override fun onLocationResult(
                    result: LocationResult
                ) {

                    val location:
                            Location =
                        result.lastLocation
                            ?: return

                    val latitude =
                        location.latitude

                    val longitude =
                        location.longitude

                    if (location.accuracy > 30) {

                        Log.d(
                            "HB",
                            "BAD GPS ACCURACY: ${location.accuracy}"
                        )

                        return
                    }

                    Log.d(
                        "HB",
                        "GPS UPDATE: " +
                                "$latitude , $longitude"
                    )

                    val data =
                        hashMapOf<String, Any>(
                            "latitude" to latitude,
                            "longitude" to longitude,
                            "timestamp" to
                                    System.currentTimeMillis()
                        )

                    val ref =
                        FirebaseManager
                            .memberReference("alain")

                    ref.child("latest")
                        .updateChildren(data)
                        .addOnSuccessListener {

                            Log.d(
                                "HB",
                                "FIREBASE WRITE SUCCESS"
                            )
                        }
                        .addOnFailureListener { error ->

                            Log.d(
                                "HB",
                                "FIREBASE WRITE FAILED: ${error.message}"
                            )
                        }
                }
            }

        fusedLocationClient
            .requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
    }

    fun stop() {

        Log.d(
            "HB",
            "TelemetryEngine STOP"
        )

        if (locationCallback != null) {

            fusedLocationClient
                .removeLocationUpdates(
                    locationCallback!!
                )
        }
    }
}