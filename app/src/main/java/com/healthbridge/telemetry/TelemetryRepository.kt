package com.healthbridge.telemetry

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.healthbridge.firebase.FirebaseManager
import com.healthbridge.models.HealthData

object TelemetryRepository {

    fun listenToHealthUpdates(
        memberId: String,
        onUpdate: (HealthData?) -> Unit
    ) {

        Log.d("HB", "LISTENER ATTACH")

        FirebaseManager
            .memberReference(memberId)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    Log.d("HB", "FIREBASE UPDATE")

                    val data = snapshot.getValue(
                        HealthData::class.java
                    )

                    Log.d("HB", "DATA = $data")

                    onUpdate(data)
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {

                    Log.d(
                        "HB",
                        error.message
                    )
                }
            })
    }
}