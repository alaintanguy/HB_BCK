package com.healthbridge.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.healthbridge.wear.health.WatchHealthDataCollector
import com.healthbridge.wear.health.WearLogTags
import com.healthbridge.wear.health.model.PermissionState
import com.healthbridge.wear.health.model.WatchHealthData
import com.healthbridge.wear.health.model.WatchMeasurementStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiagnosticActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_DIAGNOSTIC_PERMISSIONS = 202
        private val TIME_FORMAT = SimpleDateFormat("HH:mm:ss", Locale.US)
    }

    private lateinit var collector: WatchHealthDataCollector
    private lateinit var lastRefreshView: TextView
    private lateinit var capabilityContainer: LinearLayout
    private lateinit var refreshButton: Button
    private lateinit var bodySensorsButton: Button
    private lateinit var activityRecognitionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostics)

        Log.d(WearLogTags.DIAG, "Diagnostic screen opened")

        collector = WatchHealthDataCollector(applicationContext)
        lastRefreshView = findViewById(R.id.last_refresh_value)
        capabilityContainer = findViewById(R.id.capability_container)
        refreshButton = findViewById(R.id.refresh_capabilities_button)
        bodySensorsButton = findViewById(R.id.request_body_sensors_button)
        activityRecognitionButton = findViewById(R.id.request_activity_recognition_button)

        refreshButton.setOnClickListener {
            Log.d(WearLogTags.DIAG, "Manual diagnostic refresh requested")
            collector.refresh()
            render(collector.snapshot())
        }
        bodySensorsButton.setOnClickListener { requestPermissions(arrayOf(Manifest.permission.BODY_SENSORS)) }
        activityRecognitionButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermissions(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        collector.start {
            runOnUiThread {
                render(collector.snapshot())
            }
        }
        collector.refresh()
        render(collector.snapshot())
    }

    override fun onPause() {
        collector.stop()
        super.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_DIAGNOSTIC_PERMISSIONS) {
            permissions.forEachIndexed { index, permission ->
                val result = grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED
                Log.d(
                    WearLogTags.DIAG,
                    "Permission $permission: ${if (result) "GRANTED" else "DENIED"}"
                )
            }
            collector.refresh()
            render(collector.snapshot())
        }
    }

    private fun requestPermissions(permissions: Array<String>) {
        ActivityCompat.requestPermissions(
            this,
            permissions,
            REQUEST_CODE_DIAGNOSTIC_PERMISSIONS
        )
    }

    private fun render(data: WatchHealthData) {
        lastRefreshView.text = TIME_FORMAT.format(Date(data.collectedAt))
        updatePermissionButtons()
        capabilityContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        data.orderedMeasurements().forEach { measurement ->
            capabilityContainer.addView(createMeasurementView(inflater, measurement))
        }
    }

    private fun updatePermissionButtons() {
        val bodyGranted = hasPermission(Manifest.permission.BODY_SENSORS)
        bodySensorsButton.isVisible = !bodyGranted

        val needsActivityPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val activityGranted = !needsActivityPermission ||
            hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        activityRecognitionButton.isVisible = needsActivityPermission && !activityGranted
    }

    private fun createMeasurementView(
        inflater: LayoutInflater,
        measurement: WatchMeasurementStatus
    ): View {
        val view = inflater.inflate(R.layout.item_diagnostic_capability, capabilityContainer, false)
        view.findViewById<TextView>(R.id.measurement_name).text = measurement.definition.displayName
        view.findViewById<TextView>(R.id.measurement_availability).text =
            "Available: ${yesNo(measurement.definition.watchSupports)} | HB access: ${measurement.definition.hbAccess.label}"
        view.findViewById<TextView>(R.id.measurement_api).text =
            "API: ${measurement.definition.api} | Access: ${measurement.definition.accessType.label}"
        view.findViewById<TextView>(R.id.measurement_permission).text =
            "Permission: ${formatPermission(measurement.permissionState, measurement.definition.permissions)}"
        view.findViewById<TextView>(R.id.measurement_current_value).text =
            "Current Value: ${measurement.currentValue}"
        view.findViewById<TextView>(R.id.measurement_last_updated).text =
            "Last Updated: ${measurement.lastUpdatedAt?.let { TIME_FORMAT.format(Date(it)) } ?: getString(R.string.unknown_value)}"
        view.findViewById<TextView>(R.id.measurement_sampling).text =
            "Sampling: ${measurement.definition.samplingFrequency} | Battery: ${measurement.definition.batteryImpact.label}"
        view.findViewById<TextView>(R.id.measurement_restrictions).text =
            "Restrictions: ${measurement.definition.restrictions}"
        view.findViewById<TextView>(R.id.measurement_notes).text =
            "Notes: ${measurement.statusNotes}"
        return view
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun formatPermission(state: PermissionState, permissions: List<String>): String {
        return when (state) {
            PermissionState.GRANTED -> permissions.joinToString()
            PermissionState.DENIED -> permissions.joinToString().ifEmpty { "Denied" }
            PermissionState.NOT_REQUIRED -> "Not required"
            PermissionState.RESTRICTED -> permissions.joinToString().ifEmpty { "Restricted / not requestable here" }
        }
    }

    private fun yesNo(value: Boolean): String = if (value) "Yes" else "No"
}
