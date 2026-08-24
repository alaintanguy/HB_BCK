package com.healthbridge.wear

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var heartRateTextView: TextView
    private lateinit var batteryTextView: TextView

    private var telemetryService: WearTelemetryService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WearTelemetryService.LocalBinder
            telemetryService = binder.getService()
            isBound = true
            Log.d("MainActivity", "Telemetry service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            Log.d("MainActivity", "Telemetry service disconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.status)
        heartRateTextView = findViewById(R.id.heart_rate)
        batteryTextView = findViewById(R.id.battery)

        // Start the telemetry service
        startTelemetryService()

        // Update UI periodically
        updateUI()
    }

    private fun startTelemetryService() {
        val serviceIntent = Intent(this, WearTelemetryService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun updateUI() {
        Thread {
            while (!isDestroyed) {
                try {
                    runOnUiThread {
                        statusTextView.text = "HealthBridge Watch Active"
                        heartRateTextView.text = "Heart Rate: Monitoring"
                        batteryTextView.text = "Battery: Monitoring"
                    }
                    Thread.sleep(5000)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error updating UI", e)
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
        }
    }
}
