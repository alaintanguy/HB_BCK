package com.healthbridge.telemetry

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.healthbridge.firebase.FirebaseManager
import com.healthbridge.models.HealthData

object TelemetryRepository {

    fun listenToHealthUpdates(
        onUpdate: (HealthData?) -> Unit
    ) {

        FirebaseManager
            .memberReference()
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.exists()) {

                        val data = snapshot.getValue(
                            HealthData::class.java
                        )

                        onUpdate(data)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }
}