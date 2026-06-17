package com.healthbridge.telemetry

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class BatteryCollector(
    private val context: Context
) {

    fun getBatteryLevel(): Int {

        val batteryIntent =
            context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

        val level =
            batteryIntent?.getIntExtra(
                BatteryManager.EXTRA_LEVEL,
                -1
            ) ?: -1

        val scale =
            batteryIntent?.getIntExtra(
                BatteryManager.EXTRA_SCALE,
                -1
            ) ?: -1

        return if (
            level != -1 &&
            scale != -1
        ) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            0
        }
    }
}