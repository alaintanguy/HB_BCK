package com.healthbridge.firebase

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
    ){

        val healthData = HealthData(
            name = "Alain",
            heartRate = 78,
            latitude = latitude,
            longitude = longitude,
            status = "safe"
        )
        //memberReference().setValue(healthData)
        fun updateLocation(
            memberId: String,
            latitude: Double,
            longitude: Double
        ) {

            val healthData = HealthData(
                name = "Alain",
                heartRate = 78,
                latitude = latitude,
                longitude = longitude,
                status = "safe"
            )

            memberReference(memberId)
                .setValue(healthData)
        }
    }


}