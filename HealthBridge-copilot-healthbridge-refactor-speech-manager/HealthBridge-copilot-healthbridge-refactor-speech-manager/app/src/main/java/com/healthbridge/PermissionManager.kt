package com.healthbridge

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: Activity) {

    private val TAG = "PermissionManager"

    companion object {
        const val REQUEST_CODE = 200
    }

    private val permissions: Array<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        }
    }.toTypedArray()

    fun allGranted(): Boolean = permissions.all {
        ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
    }

    fun request() {
        Log.d(TAG, "Requesting ${permissions.size} permissions")
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE)
    }

    fun onResult(
        requestCode: Int,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (requestCode != REQUEST_CODE) return
        if (grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            Log.d(TAG, "All permissions granted")
            onGranted()
        } else {
            Log.w(TAG, "Some permissions denied")
            onDenied()
        }
    }
}
