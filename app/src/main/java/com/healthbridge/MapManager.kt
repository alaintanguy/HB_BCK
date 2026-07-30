package com.healthbridge

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

// =====================================================
// MAP MANAGER — map-only responsibilities
// =====================================================
class MapManager(private val activity: AppCompatActivity) : OnMapReadyCallback {

    private val TAG = "MapManager"
    private var googleMap: GoogleMap? = null

    // Initialize the SupportMapFragment and request map asynchronously
    fun initialize() {
        val mapFragment = activity.supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment
        if (mapFragment != null) {
            mapFragment.getMapAsync(this)
        } else {
            Log.e(TAG, "SupportMapFragment not found (R.id.map)")
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        Log.d(TAG, "Map ready")
    }

    // Move camera to a given location
    fun moveCameraTo(lat: Double, lng: Double, zoom: Float = 15f) {
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), zoom))
    }

    // Place a marker on the map
    fun placeMarker(lat: Double, lng: Double, title: String) {
        googleMap?.addMarker(MarkerOptions().position(LatLng(lat, lng)).title(title))
    }

    // Expose the underlying GoogleMap for extended use
    fun getMap(): GoogleMap? = googleMap
}