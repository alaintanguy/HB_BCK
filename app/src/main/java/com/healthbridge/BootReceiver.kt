package com.healthbridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.healthbridge.telemetry.TelemetryForegroundService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_USER_UNLOCKED
        ) return

        if (intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            Log.d(
                "HB-BOOT",
                "Locked boot completed — waiting for user unlock before starting telemetry"
            )
            return
        }
        Log.d("HB-BOOT", "Phone boot/unlock completed — restarting HealthBridge telemetry")
        val serviceIntent =
            Intent(context, TelemetryForegroundService::class.java).apply {
                putExtra("MEMBER_ID", "M2")
            }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            Log.d("HB-BOOT", "HealthBridge telemetry service start requested")
        } catch (e: Exception) {
            Log.e("HB-BOOT", "Unable to restart HealthBridge telemetry after boot", e)
        }
    }
}