package com.healthbridge

import android.content.pm.PackageManager
import android.os.Bundle


import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment

import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

import com.healthbridge.firebase.FirebaseManager
import com.healthbridge.telemetry.TelemetryEngine
import android.content.Intent
import com.healthbridge.telemetry.TelemetryForegroundService
import kotlin.collections.getValue

class MainActivity : AppCompatActivity(),
    OnMapReadyCallback {

    companion object {

        // CHANGE THIS FOR EACH PHONE ======================

        const val MEMBER_ID = "alain"
         //const val MEMBER_ID = "mary"

        const val IS_PUBLISHER = true
        const val IS_VIEWER = true
    }

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
            TelemetryEngine(
                this,
                MEMBER_ID
            )
        if (IS_PUBLISHER) {
            telemetryEngine.start()
        }

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

        if (
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission
                    .ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            if (IS_PUBLISHER) {

                val intent = Intent(
                    this,
                    TelemetryForegroundService::class.java
                )

                intent.putExtra(
                    "MEMBER_ID",
                    MEMBER_ID
                )

                startForegroundService(intent)
            }
        }

        FirebaseAuth.getInstance()
            .signInAnonymously()
            .addOnSuccessListener {

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

        if (IS_VIEWER) {

            listenToMember("alain")
            listenToMember("mary")
        }
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
                            snapshot.child("telemetry")
                                .child("location")
                                .child("lat")
                                .getValue(Double::class.java)
                                ?: return

                        val longitude =
                            snapshot.child("telemetry")
                                .child("location")
                                .child("lng")
                                .getValue(Double::class.java)
                                ?: return

                        if (
                            latitude == 0.0 &&
                            longitude == 0.0
                        ) {
                            return
                        }

                        val firebaseLocation =
                            LatLng(
                                latitude,
                                longitude
                            )

                        val existingMarker =
                            memberMarkers[memberId]

                        if (existingMarker == null) {

                            val markerColor =
                                when (memberId) {

                                    "alain" ->
                                        BitmapDescriptorFactory
                                            .HUE_RED

                                    "mary" ->
                                        BitmapDescriptorFactory
                                            .HUE_BLUE

                                    else ->
                                        BitmapDescriptorFactory
                                            .HUE_GREEN
                                }

                            val marker =
                                googleMap.addMarker(
                                    MarkerOptions()
                                        .position(
                                            firebaseLocation
                                        )
                                        .title(memberId)
                                        .icon(
                                            BitmapDescriptorFactory
                                                .defaultMarker(
                                                    markerColor
                                                )
                                        )
                                )

                            if (marker != null) {

                                memberMarkers[memberId] =
                                    marker
                            }

                            googleMap.animateCamera(
                                CameraUpdateFactory
                                    .newLatLngZoom(
                                        firebaseLocation,
                                        12f
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

            if (IS_PUBLISHER) {

                telemetryEngine.start()
            }
        }
    }
}