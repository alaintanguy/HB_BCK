package com.healthbridge.firebase

import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FirebaseManager {

    private val database =
        FirebaseDatabase.getInstance()

    fun memberReference(
        memberId: String
    ) =
        database.getReference(
            "groups/family_001/members/$memberId"
        )

    fun updateTelemetry(
        memberId: String,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        accuracy: Float,
        speed: Float
    ) {

        val timestamp =
            System.currentTimeMillis()

        val dateFormat =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )

        val timeFormat =
            SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
            )

        val currentDate =
            dateFormat.format(
                Date(timestamp)
            )

        val currentTime =
            timeFormat.format(
                Date(timestamp)
            )

        val reference =
            database.getReference(
                "groups/family_001/members/$memberId/telemetry"
            )

        val updates = mapOf(

            "timestamp" to timestamp,

            "readable/date" to currentDate,

            "readable/time" to currentTime,

            "location/lat" to latitude,

            "location/lng" to longitude,

            "location/altitude" to altitude,

            "location/accuracy" to accuracy,

            "location/speed" to speed
        )

        reference.updateChildren(updates)
    }
}