package com.healthbridge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.healthbridge.telemetry.GpsCollector

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {

        googleMap = map

        if (!hasLocationPermission()) {

            requestPermissions(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ),
                1001
            )

            return
        }

        val gpsCollector = GpsCollector(this@MainActivity)

        gpsCollector.getCurrentLocation { latitude: Double, longitude: Double ->

            val currentLocation = LatLng(
                latitude,
                longitude
            )

            googleMap.addMarker(
                MarkerOptions()
                    .position(currentLocation)
                    .title("Alain")
            )

            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    currentLocation,
                    17f
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {

        return checkSelfPermission(
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}