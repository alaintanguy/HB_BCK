package com.healthbridge

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * HealthBridge Fall Detection V2.1 for M2.
 *
 * V1 triggered on one large acceleration spike. V2 requires a sequence:
 *
 *   possible drop / unusual low-g movement
 *       -> strong impact
 *       -> short period of relative stillness
 *
 * This is still a prototype using only the phone accelerometer.
 * Thresholds are intentionally easy to inspect/tune from HB-FALL Logcat.
 */
class FallDetectionManager(
    context: Context,
    private val onPossibleFall: (accelerationG: Float) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var running = false
    private var lastTriggerElapsed = 0L

    // ---------- V2 thresholds ----------
    // Normal stationary magnitude is approximately 1.0 g.
    private val lowGThreshold = 0.65f
    private val impactThresholdG = 2.4f

    // Low-g must be followed fairly quickly by an impact.
    private val lowGToImpactWindowMs = 3_000L

    // After impact, observe the phone before deciding.
    private val postImpactObservationMs = 2_500L

    // During the post-impact window, count samples close to normal gravity.
    // This is a simple proxy for "phone became relatively still".
    private val stillLowerG = 0.82f
    private val stillUpperG = 1.18f
    private val requiredStillFraction = 0.65f

    private val triggerCooldownMs = 15_000L

    private enum class State {
        MONITORING,
        LOW_G_SEEN,
        OBSERVING_AFTER_IMPACT
    }

    private var state = State.MONITORING
    private var lowGElapsed = 0L
    private var impactElapsed = 0L
    private var impactG = 0f

    private var observationSamples = 0
    private var stillSamples = 0

    fun isAvailable(): Boolean = accelerometer != null

    fun start(): Boolean {
        if (running) return true

        val sensor = accelerometer ?: run {
            Log.w("HB-FALL", "No accelerometer available")
            return false
        }

        resetSequence()

        running = sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_GAME
        )

        Log.d(
            "HB-FALL",
            "Fall detector V2.1 started=$running lowG=${lowGThreshold}g impact=${impactThresholdG}g window=${lowGToImpactWindowMs}ms"
        )

        return running
    }

    fun stop() {
        if (!running) return

        sensorManager.unregisterListener(this)
        running = false
        resetSequence()

        Log.d("HB-FALL", "Fall detector V2 stopped")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitude = sqrt(x * x + y * y + z * z)
        val g = magnitude / SensorManager.GRAVITY_EARTH
        val now = SystemClock.elapsedRealtime()

        if (g >= 2.0f) {
            Log.d("HB-FALL", "ACCELERATION %.2fg state=$state".format(g))
        }

        when (state) {

            State.MONITORING -> {
                if (g <= lowGThreshold) {
                    lowGElapsed = now
                    state = State.LOW_G_SEEN
                    Log.d(
                        "HB-FALL",
                        "V2 LOW-G %.2fg — waiting for impact".format(g)
                    )
                }
            }

            State.LOW_G_SEEN -> {
                val sinceLowG = now - lowGElapsed

                if (sinceLowG > lowGToImpactWindowMs) {
                    Log.d("HB-FALL", "V2 low-g expired — reset")
                    resetSequence()
                    return
                }

                if (g >= impactThresholdG) {
                    impactElapsed = now
                    impactG = g
                    observationSamples = 0
                    stillSamples = 0
                    state = State.OBSERVING_AFTER_IMPACT

                    Log.w(
                        "HB-FALL",
                        "V2 IMPACT %.2fg after low-g — observing".format(g)
                    )
                }
            }

            State.OBSERVING_AFTER_IMPACT -> {
                observationSamples++

                if (g in stillLowerG..stillUpperG) {
                    stillSamples++
                }

                val elapsed = now - impactElapsed

                if (elapsed >= postImpactObservationMs) {
                    val stillFraction =
                        if (observationSamples > 0) {
                            stillSamples.toFloat() / observationSamples.toFloat()
                        } else {
                            0f
                        }

                    Log.d(
                        "HB-FALL",
                        "V2 POST-IMPACT still=%.0f%% samples=%d".format(
                            stillFraction * 100f,
                            observationSamples
                        )
                    )

                    val cooldownOk =
                        now - lastTriggerElapsed >= triggerCooldownMs

                    if (
                        stillFraction >= requiredStillFraction &&
                        cooldownOk
                    ) {
                        lastTriggerElapsed = now

                        Log.w(
                            "HB-FALL",
                            "V2 POSSIBLE FALL impact=%.2fg still=%.0f%%".format(
                                impactG,
                                stillFraction * 100f
                            )
                        )

                        val detectedImpactG = impactG
                        resetSequence()
                        onPossibleFall(detectedImpactG)

                    } else {
                        Log.d(
                            "HB-FALL",
                            "V2 sequence rejected — movement continued or cooldown active"
                        )
                        resetSequence()
                    }
                }
            }
        }
    }

    private fun resetSequence() {
        state = State.MONITORING
        lowGElapsed = 0L
        impactElapsed = 0L
        impactG = 0f
        observationSamples = 0
        stillSamples = 0
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}