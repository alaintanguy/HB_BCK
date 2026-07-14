package com.healthbridge.telemetry

import android.util.Log
import com.healthbridge.GpsTracker
import android.content.Context

class GpsCollector(context: Context) {

    private val TAG = "GpsCollector"
    private val tracker = GpsTracker(context)

    data class Location(val lat: Double, val lng: Double)

    fun getLocation(): Location? {
        val lat = tracker.getLatitude()
        val lng = tracker.getLongitude()
        return if (lat != null && lng != null) {
            Log.d(TAG, "Location: $lat, $lng")
            Location(lat, lng)
        } else {
            Log.d(TAG, "No location available yet")
            null
        }
    }

    fun stop() {
        tracker.stopUsingGPS()
        Log.d(TAG, "GpsCollector stopped")
    }
}
