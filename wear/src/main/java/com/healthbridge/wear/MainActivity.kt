package com.healthbridge.wear

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.healthbridge.wear.health.WearLogTags

class MainActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        private const val PERMISSION_REQUEST_BODY_SENSORS = 101
    }

    private lateinit var heartRateValue: TextView
    private lateinit var batteryValue: TextView
    private lateinit var statusMessage: TextView
    private lateinit var diagnosticsButton: Button

    private var sensorManager: SensorManager? = null
    private var heartRateSensor: Sensor? = null
    private var currentHeartRate: Int = 0
    private var currentBattery: Int = 0

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                currentBattery = if (level >= 0 && scale > 0) {
                    (level / scale.toFloat() * 100).toInt()
                } else {
                    -1
                }
                updateBatteryDisplay()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_wear)

        // Initialize UI elements
        heartRateValue = findViewById(R.id.heart_rate_value)
        batteryValue = findViewById(R.id.battery_value)
        statusMessage = findViewById(R.id.status_message)
        diagnosticsButton = findViewById(R.id.open_diagnostics_button)
        diagnosticsButton.setOnClickListener {
            Log.d(WearLogTags.DIAG, "Opening diagnostic screen from main UI")
            startActivity(Intent(this, DiagnosticActivity::class.java))
        }

        // Initialize sensor manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Initialize heart rate sensor
        initializeHeartRateSensor()

        // Get initial battery level
        updateBatteryLevel()

        // Register for battery updates
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, intentFilter)

// Start Phase 2 telemetry when permission was already granted
        if (hasHeartRatePermission()) {
            startForegroundService(Intent(this, WearTelemetryService::class.java))
        }


}

    override fun onResume() {
        super.onResume()
        if (heartRateSensor != null && hasHeartRatePermission()) {
            sensorManager?.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
    }

    /**
     * Initialize heart rate sensor and request permissions if needed
     */
    private fun initializeHeartRateSensor() {
        // Try to get the heart rate sensor
        heartRateSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        if (heartRateSensor == null) {
            // Heart rate sensor not available
            Log.w(WearLogTags.API, "TYPE_HEART_RATE sensor not available on this watch")
            statusMessage.text = getString(R.string.hr_unavailable)
            heartRateValue.text = getString(R.string.hr_no_reading)
            return
        }

        // Check if we have permission to access heart rate
        if (!hasHeartRatePermission()) {
            Log.d(WearLogTags.API, "Requesting BODY_SENSORS permission for heart rate access")
            requestHeartRatePermission()
        }
    }

    /**
     * Check if BODY_SENSORS permission is granted
     */
    private fun hasHeartRatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BODY_SENSORS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request BODY_SENSORS permission
     */
    private fun requestHeartRatePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.BODY_SENSORS),
            PERMISSION_REQUEST_BODY_SENSORS
        )
    }

    /**
     * Handle permission request result
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_BODY_SENSORS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, register sensor
                Log.d(WearLogTags.API, "BODY_SENSORS permission granted")
                if (heartRateSensor != null) {
                    sensorManager?.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_UI)
                    statusMessage.text = ""
                    // Start Phase 2 Watch telemetry after permission is granted
                    startForegroundService(Intent(this, WearTelemetryService::class.java))
                }
            } else {
                // Permission denied
                Log.w(WearLogTags.API, "BODY_SENSORS permission denied")
                statusMessage.text = getString(R.string.no_permission)
                heartRateValue.text = getString(R.string.hr_no_reading)
            }
        }
    }

    /**
     * Get and display current battery level
     */
    private fun updateBatteryLevel() {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, ifilter)
        if (batteryStatus != null) {
            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            currentBattery = if (level >= 0 && scale > 0) {
                (level / scale.toFloat() * 100).toInt()
            } else {
                -1
            }
        }
        updateBatteryDisplay()
    }

    /**
     * Update battery display on UI
     */
    private fun updateBatteryDisplay() {
        batteryValue.text = if (currentBattery >= 0) {
            "$currentBattery %"
        } else {
            getString(R.string.battery_no_reading)
        }
    }

    /**
     * Sensor event listener: Called when heart rate value changes
     */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_HEART_RATE) {
            // Check if we have a valid heart rate reading
            if (event.values.isNotEmpty()) {
                currentHeartRate = event.values[0].toInt()
                updateHeartRateDisplay()

                // Clear any status messages when we get valid readings
                if (currentHeartRate > 0) {
                    Log.d(WearLogTags.HEALTH, "Heart rate = $currentHeartRate BPM")
                    statusMessage.text = ""
                }
            }
        }
    }

    /**
     * Update heart rate display on UI
     */
    private fun updateHeartRateDisplay() {
        heartRateValue.text = if (currentHeartRate > 0) {
            "$currentHeartRate BPM"
        } else {
            getString(R.string.hr_no_reading)
        }
    }

    /**
     * Sensor accuracy changed listener
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
}
