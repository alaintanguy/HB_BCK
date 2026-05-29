package com.healthbridge

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

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

class MainActivity : AppCompatActivity(),
    OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap

    private lateinit var telemetryEngine:
            TelemetryEngine

    private val memberMarkers =
        mutableMapOf<String, Marker>()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        telemetryEngine =
            TelemetryEngine(this)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission
                    .ACCESS_FINE_LOCATION,

                android.Manifest.permission
                    .ACCESS_COARSE_LOCATION
            ),
            1001
        )

        FirebaseAuth.getInstance()
            .signInAnonymously()
            .addOnSuccessListener {

                Log.d(
                    "HB",
                    "AUTH OK"
                )

                val mapFragment =
                    supportFragmentManager
                        .findFragmentById(R.id.map)
                            as SupportMapFragment

                mapFragment.getMapAsync(this)
            }
    }

    override fun onMapReady(
        map: GoogleMap
    ) {

        googleMap = map

        listenToMember("alain")

        // LATER:
        // listenToMember("mary")
    }

    private fun listenToMember(
        memberId: String
    ) {

        FirebaseManager
            .memberReference(memberId)
            .addValueEventListener(

                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        val latitude =
                            snapshot.child("latest")
                                .child("latitude")
                                .getValue(Double::class.java)
                                ?: return

                        val longitude =
                            snapshot.child("latest")
                                .child("longitude")
                                .getValue(Double::class.java)
                                ?: return

                        val firebaseLocation =
                            LatLng(
                                latitude,
                                longitude
                            )

                        val existingMarker =
                            memberMarkers[memberId]

                        if (existingMarker == null) {

                            val marker =
                                googleMap.addMarker(
                                    MarkerOptions()
                                        .position(
                                            firebaseLocation
                                        )
                                        .title(memberId)
                                )

                            if (marker != null) {

                                memberMarkers[memberId] =
                                    marker
                            }

                            googleMap.animateCamera(
                                CameraUpdateFactory
                                    .newLatLngZoom(
                                        firebaseLocation,
                                        16f
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
            grantResults[0]
            == PackageManager.PERMISSION_GRANTED
        ) {

            telemetryEngine.start()
        }
    }
}