package com.healthbridge

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.healthbridge.firebase.FirebaseManager
import com.healthbridge.telemetry.TelemetryEngine
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity :
    AppCompatActivity(),
    OnMapReadyCallback {

    companion object {
        //  const val MEMBER_ID = "alain"
        //  const val IS_PUBLISHER = false

        const val MEMBER_ID = "mary"
        const val IS_PUBLISHER = true
    }

    private lateinit var telemetryEngine:
            TelemetryEngine

    private lateinit var googleMap:
            GoogleMap

    private var memberMarker:
            Marker? = null

    private lateinit var infoText:
            TextView

    private fun hasLocationPermission(): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            100
        )
    }

    private fun openAppSettings() {

        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        )

        intent.data = Uri.fromParts(
            "package",
            packageName,
            null
        )

        startActivity(intent)
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        Log.e("HB", "MARY BUILD JUNE16")

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )

        infoText =
            findViewById(R.id.infoText)

        val mapFragment =
            supportFragmentManager
                .findFragmentById(
                    R.id.map
                ) as SupportMapFragment

        mapFragment.getMapAsync(this)

        if (IS_PUBLISHER) {

            Log.d(
                "HB",
                "STARTING TELEMETRY ENGINE"
            )

            if (!hasLocationPermission()) {

                infoText.text =
                    "LOCATION PERMISSION REQUIRED\n" +
                            "Settings → HealthBridge → Permissions → Location → Allow all the time"

                requestLocationPermission()

                return
            }


            telemetryEngine =
                TelemetryEngine(
                    this,
                    MEMBER_ID
                )

            telemetryEngine.start()
        }
        listenToMember("mary") //MEMBER_ID)


    }

        override fun onMapReady(
            map: GoogleMap
        ) {

            googleMap = map

            val start =
                LatLng(
                    38.5816,
                    -122.5825
                )

            googleMap.moveCamera(
                CameraUpdateFactory
                    .newLatLngZoom(
                        start,
                        15f
                    )
            )
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

                                snapshot
                                    .child("telemetry")
                                    .child("location")
                                    .child("lat")
                                    .getValue(Double::class.java)
                                    ?: return

                            val longitude =
                                snapshot
                                    .child("telemetry")
                                    .child("location")
                                    .child("lng")
                                    .getValue(Double::class.java)
                                    ?: return

                            Log.d(
                                "HB",
                                "MARY MARKER: $latitude , $longitude"
                            )

                            val altitude =
                                snapshot
                                    .child("telemetry")
                                    .child("location")
                                    .child("altitude")
                                    .getValue(Double::class.java)
                                    ?: 0.0

                            val timestamp =
                                snapshot
                                    .child("telemetry")
                                    .child("timestamp")
                                    .getValue(Long::class.java)
                                    ?: 0L

                            val battery =
                                snapshot
                                    .child("device")
                                    .child("phoneBattery")
                                    .getValue(Int::class.java)
                                    ?: 0

                            val lowBattery =
                                snapshot
                                    .child("alerts")
                                    .child("lowBattery")
                                    .getValue(Boolean::class.java)
                                    ?: false

                            val batteryStatus =
                                if (lowBattery)
                                    "LOW BATTERY"
                                else
                                    "OK"

                            Log.d(
                                "HB",
                                "FB LOCATION: $latitude , $longitude"
                            )
                            val lastSeen =
                                snapshot
                                    .child("device")
                                    .child("lastSeen")
                                    .getValue(Long::class.java)
                                    ?: 0L

                            val ageMinutes =
                                (System.currentTimeMillis() - lastSeen) /
                                        60000

                            val status =
                                if (ageMinutes <= 2)
                                    "ONLINE"
                                else
                                    "OFFLINE"

                            val position =
                                LatLng(
                                    latitude,
                                    longitude
                                )

                            runOnUiThread {

                                if (
                                    memberMarker == null
                                ) {

                                    memberMarker =
                                        googleMap.addMarker(

                                            MarkerOptions()
                                                .position(position)
                                                .title(memberId)
                                                .icon(
                                                    BitmapDescriptorFactory
                                                        .defaultMarker(
                                                            if (memberId == "M1")
                                                                BitmapDescriptorFactory.HUE_RED
                                                            else
                                                                BitmapDescriptorFactory.HUE_BLUE
                                                        )
                                                )
                                        )

                                } else {

                                    memberMarker?.position =
                                        position
                                }

                                googleMap.animateCamera(
                                    CameraUpdateFactory
                                        .newLatLng(position)
                                )

                                infoText.text =
                                    "Lat: $latitude  Lng: $longitude\n" +
                                            "$memberId - $status - LS: $ageMinutes min - Bat: $battery%"

                            }
                        }

                        override fun onCancelled(
                            error: DatabaseError
                        ) {

                            Log.e(
                                "HB",
                                "FIREBASE READ FAILED",
                                error.toException()
                            )
                        }
                    }
                )
        }
    }

