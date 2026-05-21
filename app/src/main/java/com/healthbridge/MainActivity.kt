package com.healthbridge

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.healthbridge.firebase.FirebaseManager
import com.healthbridge.telemetry.TelemetryEngine


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var firebaseMarker:
            com.google.android.gms.maps.model.Marker? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val telemetryEngine= TelemetryEngine(this)
        telemetryEngine.start()


        setContentView(R.layout.activity_main)

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
                                .value.toString()
                                .toDouble()

                        val longitude =
                            snapshot.child("longitude")
                                .value.toString()
                                .toDouble()

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

                        if (firebaseMarker == null) {

                            firebaseMarker = googleMap.addMarker(
                                MarkerOptions()
                                    .position(firebaseLocation)
                                    .title("Alain")
                            )

                            googleMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    firebaseLocation,
                                    15f
                                )
                            )

                        } else {

                            firebaseMarker?.position = firebaseLocation
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