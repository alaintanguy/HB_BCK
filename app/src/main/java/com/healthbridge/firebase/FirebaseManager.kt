package com.healthbridge.firebase

import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.healthbridge.models.HealthData

object FirebaseManager {

    private val database = FirebaseDatabase.getInstance(
        "https://healthbridge-e2aac-default-rtdb.firebaseio.com/"
    )

    fun memberReference(
        memberId: String
    ): DatabaseReference {

        return database.getReference(
            "groups/family_001/members/$memberId"
        )
    }

    fun updateLocation(
        memberId: String,
        latitude: Double,
        longitude: Double
    ) {

        val healthData = HealthData(
            name = memberId,
            heartRate = 78,
            latitude = latitude,
            longitude = longitude,
            status = "safe"
        )

        Log.d(
            "HB_FIREBASE",
            "WRITING LAT=$latitude LON=$longitude"
        )

        memberReference(memberId)
            .child("latest")
            .setValue(healthData)
            .addOnSuccessListener {

                Log.d(
                    "HB_FIREBASE",
                    "FIREBASE WRITE SUCCESS"
                )
            }
            .addOnFailureListener {

                Log.d(
                    "HB_FIREBASE",
                    "FIREBASE WRITE FAILED: ${it.message}"
                )
            }
    }
}