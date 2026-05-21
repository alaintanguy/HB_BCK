package com.healthbridge

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
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

    // MULTIUSER MARKERS
    private val memberMarkers =
        mutableMapOf<String, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // START REALTIME GPS TELEMETRY
        val telemetryEngine = TelemetryEngine(this)
        telemetryEngine.start()

        FirebaseAuth.getInstance()
            .signInAnonymously()
            .addOnSuccessListener {

                Log.d("HB", "AUTH OK")

                val mapFragment = supportFragmentManager
                    .findFragmentById(R.id.map)
                        as SupportMapFragment

                mapFragment.getMapAsync(this)
            }
            .addOnFailureListener {

                Log.d("HB", "AUTH FAILED")
            }
    }

    override fun onMapReady(map: GoogleMap) {

        googleMap = map

        // FIREBASE LISTENER
        FirebaseManager
            .memberReference("alain")
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
                            snapshot.child("latitude")
                                .getValue(Double::class.java)
                                ?: 0.0

                        val longitude =
                            snapshot.child("longitude")
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

                        val firebaseLocation = LatLng(
                            latitude,
                            longitude
                        )

                        val memberId = "alain"

                        val existingMarker =
                            memberMarkers[memberId]

                        if (existingMarker == null) {

                            val marker = googleMap.addMarker(
                                MarkerOptions()
                                    .position(firebaseLocation)
                                    .title(memberId)
                            )

                            if (marker != null) {

                                memberMarkers[memberId] =
                                    marker
                            }

                            googleMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
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
}