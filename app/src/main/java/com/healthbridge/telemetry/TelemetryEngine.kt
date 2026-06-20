package com.healthbridge.telemetry

import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
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

                FirebaseManager.updateLastSeen(
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

            lastLatitude = latitude
            lastLongitude = longitude
        }
    }


}
