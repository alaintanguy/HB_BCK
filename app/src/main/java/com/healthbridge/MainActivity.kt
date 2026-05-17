package com.healthbridge

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.healthbridge.telemetry.TelemetryRepository
import com.healthbridge.models.HealthData


class MainActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance(
            "https://healthbridge-e2aac-default-rtdb.firebaseio.com/"
        )

        // Authenticate anonymously
        FirebaseAuth.getInstance()
            .signInAnonymously()
            .addOnSuccessListener {

                // Data to send
                val healthData = mapOf(
                    "name" to "Alain",
                    "heartRate" to 78,
                    "latitude" to 45.5017,
                    "longitude" to -73.5673,
                    "status" to "safe"
                )

                // Firebase write
                database.getReference(
                    "groups/family_001/members/alain"
                ).setValue(healthData)
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "HealthBridge synced",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->

                        Toast.makeText(
                            this,
                            "Database error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                // Realtime listener
                TelemetryRepository.listenToHealthUpdates { data: HealthData? ->

                    if (data != null) {

                        Toast.makeText(
                            this,
                            "Heart Rate: ${data.heartRate}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    "Auth error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        if (!hasLocationPermission()) {

            requestPermissions(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ),
                1001
            )

            return
        }
    }
    private fun hasLocationPermission(): Boolean {

        return checkSelfPermission(
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}