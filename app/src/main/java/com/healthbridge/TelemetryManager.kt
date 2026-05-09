package com.healthbridge

import com.google.firebase.database.FirebaseDatabase

class TelemetryManager {

    private val database =
        FirebaseDatabase.getInstance()

    fun uploadTelemetry() {

        val ref = database.getReference(
            "groups/family_001/members/alain/latest"
        )

        val data = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "heartRate" to 89,
            "steps" to 1200
        )

        ref.setValue(data)
    }
}