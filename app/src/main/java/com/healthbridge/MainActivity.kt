package com.healthbridge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

import com.healthbridge.firebase.FirebaseManager
import com.healthbridge.telemetry.TelemetryEngine

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap

    private val memberMarkers =
        mutableMapOf<String, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            val telemetryEngine =
                TelemetryEngine(this)

            telemetryEngine.start()

        } else {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                1001
            )
        }

        FirebaseAuth.getInstance()
            .signInAnonymously()
            .addOnSuccessListener {

                Log.d("HB", "AUTH OK")

                val mapFragment =
                    supportFragmentManager
                        .findFragmentById(R.id.map)
                            as SupportMapFragment

                mapFragment.getMapAsync(this)
            }
            .addOnFailureListener {

                Log.d("HB", "AUTH FAILED")
            }
    }

    override fun onMapReady(map: GoogleMap) {

        Log.d(
            "HB_MAP",
            "MAP READY"
        )

        googleMap = map

        FirebaseManager
            .memberReference("mary")
            .addValueEventListener(
                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        Log.d(
                            "HB",
                            "FIREBASE CALLBACK"
                        )

                        val latitude =
                            snapshot.child("latest")
                                .child("latitude")
                                .getValue(Double::class.java)
                                ?: 0.0

                        val longitude =
                            snapshot.child("latest")
                                .child("longitude")
                                .getValue(Double::class.java)
                                ?: 0.0

                        Log.d(
                            "HB",
                            "LAT=$latitude"
                        )

                        Log.d(
                            "HB",
                            "LON=$longitude"
                        )

                        val firebaseLocation =
                            LatLng(
                                latitude,
                                longitude
                            )

                        val memberId = "mary"

                        val existingMarker =
                            memberMarkers[memberId]

                        if (existingMarker == null) {

                            val marker =
                                googleMap.addMarker(
                                    MarkerOptions()
                                        .position(firebaseLocation)
                                        .title(memberId)
                                )

                            if (marker != null) {

                                memberMarkers[memberId] =
                                    marker
                            }

                            googleMap.moveCamera(
                                CameraUpdateFactory
                                    .newLatLngZoom(
                                        firebaseLocation,
                                        15f
                                    )
                            )

                        } else {

                            existingMarker.position =
                                firebaseLocation
                        }
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        Log.d(
                            "HB",
                            error.message
                        )
                    }
                }
            )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode == 1001 &&
            grantResults.isNotEmpty() &&
            grantResults[0] ==
            PackageManager.PERMISSION_GRANTED
        ) {

            val telemetryEngine =
                TelemetryEngine(this)

            telemetryEngine.start()
        }
    }
}