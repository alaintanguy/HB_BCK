package com.healthbridge.firebase

import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseManager {

    private val database: DatabaseReference =
        FirebaseDatabase
            .getInstance()
            .reference

    fun memberReference(
        memberId: String
    ): DatabaseReference {

        return database
            .child("groups")
            .child("family_001")
            .child("members")
            .child(memberId)
    }

    fun updateLocation(
        memberId: String,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ) {

        val currentTime =
            System.currentTimeMillis()

        val readableDate =
            java.text.SimpleDateFormat(
                "yyyy-MM-dd",
                java.util.Locale.getDefault()
            ).format(
                java.util.Date(currentTime)
            )

        val readableTime =
            java.text.SimpleDateFormat(
                "HH:mm:ss",
                java.util.Locale.getDefault()
            ).format(
                java.util.Date(currentTime)
            )

        val updates =
            mapOf(
                "telemetry/location/lat" to latitude,
                "telemetry/location/lng" to longitude,
                "telemetry/location/altitude" to altitude,

                "telemetry/timestamp" to currentTime,

                "telemetry/readable/date" to readableDate,
                "telemetry/readable/time" to readableTime
            )

        memberReference(memberId)
            .updateChildren(updates)
            .addOnSuccessListener {

                Log.d(
                    "HB",
                    "FIREBASE TELEMETRY SUCCESS"
                )
            }
            .addOnFailureListener { error ->

                Log.e(
                    "HB",
                    "FIREBASE TELEMETRY FAILED",
                    error
                )
            }
    }

    fun updateLastSeen(
        memberId: String
    ) {

        memberReference(memberId)
            .child("device")
            .child("lastSeen")
            .setValue(
                System.currentTimeMillis()
            )
    }

    fun updateStatus(
        memberId: String,
        status: String
    ) {

        memberReference(memberId)
            .child("device")
            .child("status")
            .setValue(status)
    }

    fun updateBattery(
        memberId: String,
        battery: Int
    ) {

        memberReference(memberId)
            .child("device")
            .child("phoneBattery")
            .setValue(battery)
            .addOnSuccessListener {

                Log.d(
                    "HB",
                    "FIREBASE BATTERY SUCCESS"
                )
            }
            .addOnFailureListener { error ->

                Log.e(
                    "HB",
                    "FIREBASE BATTERY FAILED",
                    error
                )
            }
    }

    fun updateLowBatteryAlert(
        memberId: String,
        isLow: Boolean
    ) {

        memberReference(memberId)
            .child("alerts")
            .child("lowBattery")
            .setValue(isLow)
    }
}