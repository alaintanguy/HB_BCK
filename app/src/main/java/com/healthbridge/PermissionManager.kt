// ====================================================================
// HealthBridge
// PermissionManager.kt
// Version: 1.0
// ====================================================================
//
// Phase 5B: Permission management responsibilities extracted from
// MainActivity. Encapsulates location permission check, request, and
// app settings navigation.
// ====================================================================

package com.healthbridge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// =====================================================
// PERMISSION MANAGER
// =====================================================

class PermissionManager(private val activity: AppCompatActivity) {

    companion object {
        const val REQUEST_CODE_LOCATION = 100
    }

    // =====================================================
    // PERMISSION CHECKS
    // =====================================================

    fun hasLocationPermission(): Boolean {

        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestLocationPermission() {

        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            REQUEST_CODE_LOCATION
        )
    }

    fun openAppSettings() {

        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        )

        intent.data = Uri.fromParts(
            "package",
            activity.packageName,
            null
        )

        activity.startActivity(intent)
    }
}
