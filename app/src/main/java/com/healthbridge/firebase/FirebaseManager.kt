package com.healthbridge.firebase

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseManager {

    private val database: FirebaseDatabase =
        FirebaseDatabase.getInstance()

    fun memberReference(
        memberId: String
    ): DatabaseReference {

        return database.reference
            .child("families")
            .child("family_001")
            .child("members")
            .child(memberId)
    }

    fun updateLocation(
        memberId: String,
        latitude: Double,
        longitude: Double,
        altitude : Double
    ) {

        val updates = mapOf(
            "latest/lat" to latitude,
            "latest/lng" to longitude,
            "latest/altitude" to altitude,
            "latest/time" to System.currentTimeMillis()
        )

        memberReference(memberId)
            .updateChildren(updates)
            .addOnSuccessListener {

                android.util.Log.d(
                    "HB",
                    "FIREBASE WRITE SUCCESS"
                )
            }
            .addOnFailureListener { error ->

                android.util.Log.e(
                    "HB",
                    "FIREBASE WRITE FAILED: ${error.message}"
                )
            }
    }
}