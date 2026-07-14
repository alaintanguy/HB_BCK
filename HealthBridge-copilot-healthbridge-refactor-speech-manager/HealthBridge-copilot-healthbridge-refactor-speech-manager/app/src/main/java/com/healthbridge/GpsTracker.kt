package com.healthbridge

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log

class GpsTracker(private val context: Context) : LocationListener {

    private val TAG = "GpsTracker"
    private var locationManager: LocationManager? = null
    private var location: Location? = null

    init {
        getLocation()
    }

    private fun getLocation() {
        try {
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val gpsEnabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val networkEnabled = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (gpsEnabled) {
                locationManager!!.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE,
                    this
                )
                location = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                Log.d(TAG, "Using GPS provider")
            } else if (networkEnabled) {
                locationManager!!.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE,
                    this
                )
                location = locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                Log.d(TAG, "Using Network provider")
            } else {
                Log.e(TAG, "No location provider available")
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location: ${e.message}")
        }
    }

    fun getLatitude(): Double? = location?.latitude

    fun getLongitude(): Double? = location?.longitude

    fun stopUsingGPS() {
        locationManager?.removeUpdates(this)
        Log.d(TAG, "GPS stopped")
    }

    override fun onLocationChanged(loc: Location) {
        location = loc
        Log.d(TAG, "Location updated: ${loc.latitude}, ${loc.longitude}")
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String) {
        Log.d(TAG, "Provider enabled: $provider")
    }

    override fun onProviderDisabled(provider: String) {
        Log.d(TAG, "Provider disabled: $provider")
    }

    companion object {
        private const val MIN_TIME_BW_UPDATES = 60_000L    // 1 minute
        private const val MIN_DISTANCE_CHANGE = 10f         // 10 meters
    }
}
