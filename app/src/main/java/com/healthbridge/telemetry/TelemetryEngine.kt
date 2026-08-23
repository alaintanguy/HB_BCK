package com.healthbridge.telemetry

import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.healthbridge.firebase.FirebaseManager

class TelemetryEngine(
    private val context: Context,
    private val memberId: String
) {


    companion object {

        private const val NORMAL_INTERVAL =
            60000L

        private const val EMERGENCY_INTERVAL =
            10000L
    }

    private val gpsCollector =
        GpsCollector(context)

    private val batteryCollector =
        BatteryCollector(context)

    private var currentInterval =
        NORMAL_INTERVAL

    private var lastLatitude: Double? = null

    private var lastLongitude: Double? = null

    private val movementThreshold = 10f

    private var lowBatteryThreshold = 20

    // Simple geofence prototype.
    private var homeLatitude: Double? = null
    private var homeLongitude: Double? = null
    private var geofenceRadiusMeters = 91f
    private var geofenceOutside = false

    private val heartbeatHandler =
        Handler(Looper.getMainLooper())

    private val heartbeatRunnable =
        object : Runnable {

            override fun run() {

                Log.d(
                    "HB",
                    "HEARTBEAT RUNNING"
                )

                val battery =
                    batteryCollector.getBatteryLevel()
                Log.d(
                    "HB",
                    "BATTERY LEVEL = $battery"
                )

                FirebaseManager.updateBattery(
                    memberId,
                    battery
                )

                // Heartbeat must refresh telemetry timestamp/date/time
                // even when the patient has not moved.
                FirebaseManager.updateHeartbeat(
                    memberId
                )

                FirebaseManager.updateStatus(
                    memberId,
                    "online"
                )

                FirebaseManager.updateLowBatteryAlert(
                    memberId,
                    battery <= lowBatteryThreshold
                )

                Log.d(
                    "HB",
                    "HEARTBEAT SENT"
                )

                heartbeatHandler.postDelayed(
                    this,
                    currentInterval
                )
            }
        }

    fun setEmergencyMode(
        enabled: Boolean
    ) {

        currentInterval =
            if (enabled) {
                EMERGENCY_INTERVAL
            } else {
                NORMAL_INTERVAL
            }
    }

    fun start() {

        Log.d(
            "HB",
            "ENTERED TelemetryEngine.start()"
        )

        FirebaseManager.listenToLowBatteryThreshold(
            memberId
        ) { threshold ->

            lowBatteryThreshold = threshold

            Log.d(
                "HB",
                "LOW BATTERY THRESHOLD = $threshold"
            )
        }

        if (memberId == "M2") {
            FirebaseManager.listenToHomeGeofence(memberId) {
                    latitude,
                    longitude,
                    radiusMeters,
                    enabled ->

                if (enabled) {
                    homeLatitude = latitude
                    homeLongitude = longitude
                    geofenceRadiusMeters = radiusMeters.toFloat()

                    Log.d(
                        "HB",
                        "GEOFENCE HOME FROM FIREBASE: " +
                                "$latitude , $longitude radius=$radiusMeters"
                    )
                } else {
                    homeLatitude = null
                    homeLongitude = null
                    geofenceOutside = false

                    Log.d("HB", "GEOFENCE DISABLED IN FIREBASE")
                }
            }
        }

        heartbeatHandler.post(
            heartbeatRunnable
        )

        gpsCollector.startLocationUpdates(
            currentInterval
        ) { latitude,
            longitude,
            altitude,
            accuracy,
            speed ->

            Log.d(
                "HB",
                "GPS UPDATE: $latitude , $longitude"
            )

            // TEMPORARY GPS DIAGNOSTIC — display M2 coordinates.
            if (memberId == "M2") {
                Toast.makeText(
                    context,
                    "M2 GPS  Lat: %.6f  Lng: %.6f".format(latitude, longitude),
                    Toast.LENGTH_LONG
                ).show()
            }

            val previousLat = lastLatitude
            val previousLng = lastLongitude

            if (
                previousLat != null &&
                previousLng != null
            ) {

                val results = FloatArray(1)

                Location.distanceBetween(
                    previousLat,
                    previousLng,
                    latitude,
                    longitude,
                    results
                )

                val distanceMeters = results[0]

                Log.d(
                    "HB",
                    "DISTANCE = $distanceMeters"
                )
            }

            FirebaseManager.updateLocation(
                memberId,
                latitude,
                longitude,
                altitude
            )

            if (memberId == "M2") {
                val homeLat = homeLatitude
                val homeLng = homeLongitude

                if (homeLat == null || homeLng == null) {
                    Log.d(
                        "HB",
                        "GEOFENCE waiting for enabled Firebase Home coordinates"
                    )
                } else {
                    val homeDistance = FloatArray(1)
                    Location.distanceBetween(
                        homeLat,
                        homeLng,
                        latitude,
                        longitude,
                        homeDistance
                    )

                    val distanceFromHome = homeDistance[0]
                    val outsideNow = distanceFromHome > geofenceRadiusMeters

                    Log.d(
                        "HB",
                        "GEOFENCE distance=$distanceFromHome m " +
                                "radius=$geofenceRadiusMeters m outside=$outsideNow"
                    )

                    if (outsideNow != geofenceOutside) {
                        geofenceOutside = outsideNow

                        FirebaseManager.updateGeofenceAlert(
                            memberId,
                            outsideNow,
                            distanceFromHome.toDouble()
                        )

                        // Send one conversation event only when Mary crosses the border.
                        val eventText =
                            if (outsideNow) {
                                val yards = (distanceFromHome * 1.09361f).toInt()
                                "SYSTEM_EVENT:GEOFENCE_EXIT:$yards"
                            } else {
                                "SYSTEM_EVENT:GEOFENCE_RETURN"
                            }

                        FirebaseManager.sendMessage(
                            memberId,
                            "M1",
                            eventText
                        )
                    }
                }
            }

            lastLatitude = latitude
            lastLongitude = longitude
        }
    }


}