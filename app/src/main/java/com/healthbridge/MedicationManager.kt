package com.healthbridge

import android.util.Log
import com.google.firebase.database.*

data class Medication(
    val id: String = "",
    val name: String = "",
    val strength: String = "",
    val dose: String = "",
    val schedule: String = "",
    val reminderMinutes: Int = 15,
    val enabled: Boolean = true
)

data class MedicationTimeBlock(
    val schedule: String,
    val medications: List<Medication>
)

class MedicationManager(
    private val memberId: String
) {

    private val TAG = "HB-MED"
    private var currentBlocks: List<MedicationTimeBlock> = emptyList()

    private val medicationHandler =
        android.os.Handler(android.os.Looper.getMainLooper())

    private var dueBlockCallback:
            ((MedicationTimeBlock) -> Unit)? = null

    private var lastReportedSchedule: String? = null

    private val medicationLogReference: DatabaseReference =
        FirebaseDatabase
            .getInstance()
            .reference
            .child("groups")
            .child("family_001")
            .child("members")
            .child(memberId)
            .child("medicationLog")

    private val medicationsReference: DatabaseReference =
        FirebaseDatabase
            .getInstance()
            .reference
            .child("groups")
            .child("family_001")
            .child("members")
            .child(memberId)
            .child("medications")

    /**
     * Listen continuously for medication changes in Firebase.
     *
     * The callback returns enabled medications grouped by schedule.
     * Example:
     *
     * 09:00
     *   Tylenol
     *   Glucophage
     *
     * 19:00
     *   Glucophage
     *   Ivermectin
     */
    fun startListening(
        onBlocksChanged: (List<MedicationTimeBlock>) -> Unit
    ) {

        medicationsReference.addValueEventListener(
            object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val medications = mutableListOf<Medication>()

                    for (child in snapshot.children) {

                        val enabled =
                            child.child("enabled")
                                .getValue(Boolean::class.java)
                                ?: true

                        if (!enabled) {
                            continue
                        }

                        val medication = Medication(
                            id = child.key ?: "",
                            name =
                                child.child("name")
                                    .getValue(String::class.java)
                                    ?: "",
                            strength =
                                child.child("strength")
                                    .getValue(String::class.java)
                                    ?: "",
                            dose =
                                child.child("dose")
                                    .getValue(String::class.java)
                                    ?: "",
                            schedule =
                                child.child("schedule")
                                    .getValue(String::class.java)
                                    ?: "",
                            reminderMinutes =
                                child.child("reminderMinutes")
                                    .getValue(Int::class.java)
                                    ?: 15,
                            enabled = enabled
                        )

                        if (medication.schedule.isNotBlank()) {
                            medications.add(medication)
                        }
                    }

                    val blocks =
                        medications
                            .groupBy { it.schedule }
                            .toSortedMap()
                            .map { (schedule, meds) ->

                                MedicationTimeBlock(
                                    schedule = schedule,
                                    medications = meds
                                )
                            }

                    Log.d(
                        TAG,
                        "Medication blocks loaded: ${blocks.size}"
                    )

                    for (block in blocks) {

                        Log.d(
                            TAG,
                            "TIME BLOCK ${block.schedule}"
                        )

                        for (medication in block.medications) {

                            Log.d(
                                TAG,
                                "  ${medication.name} " +
                                        "${medication.strength} " +
                                        "${medication.dose}"
                            )
                        }
                    }
                    currentBlocks = blocks

                    onBlocksChanged(blocks)

// Firebase medication data has just arrived.
// Check immediately instead of waiting for the next
// one-minute clock cycle.
                    checkForDueMedication()
                }

                override fun onCancelled(error: DatabaseError) {

                    Log.e(
                        TAG,
                        "Medication listener failed",
                        error.toException()
                    )
                }
            }
        )
    }
    fun findDueBlock(
        blocks: List<MedicationTimeBlock>,
        nowMillis: Long = System.currentTimeMillis()
    ): MedicationTimeBlock? {

        val currentTime =
            java.text.SimpleDateFormat(
                "HH:mm",
                java.util.Locale.US
            ).format(java.util.Date(nowMillis))

        Log.d(
            TAG,
            "Checking medications at $currentTime"
        )

        return blocks.firstOrNull { block ->
            block.schedule == currentTime
        }
    }
    fun startClockWatcher(
        onMedicationDue: (MedicationTimeBlock) -> Unit
    ) {
        dueBlockCallback = onMedicationDue

        medicationHandler.removeCallbacks(medicationClockRunnable)
        medicationHandler.post(medicationClockRunnable)

        Log.d(TAG, "Medication clock watcher started")
    }

    private val medicationClockRunnable =
        object : Runnable {

            override fun run() {

                checkForDueMedication()

                medicationHandler.postDelayed(
                    this,
                    60_000L
                )
            }
        }

    private fun checkForDueMedication() {

        if (currentBlocks.isEmpty()) return

        val currentTime =
            java.text.SimpleDateFormat(
                "HH:mm",
                java.util.Locale.US
            ).format(java.util.Date())

        /*
         * A medication block becomes active once its scheduled
         * time has arrived.
         *
         * For this first version, only today's schedule matters.
         * Confirmation logic will later clear the active block.
         */

        val dueBlock =
            currentBlocks
                .filter { it.schedule <= currentTime }
                .maxByOrNull { it.schedule }

        if (dueBlock == null) return

        if (lastReportedSchedule == dueBlock.schedule) {
            return
        }

        val callback = dueBlockCallback

        if (callback == null) {
            Log.d(
                TAG,
                "MEDICATION DUE ${dueBlock.schedule} — callback not ready"
            )
            return
        }

        val today =
            java.text.SimpleDateFormat(
                "yyyy-MM-dd",
                java.util.Locale.US
            ).format(java.util.Date())

        val safeSchedule =
            dueBlock.schedule.replace(":", "-")

        medicationLogReference
            .child(today)
            .child(safeSchedule)
            .addListenerForSingleValueEvent(

                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        // If this block already exists in today's log,
                        // it has already been handled.
                        if (snapshot.exists()) {

                            lastReportedSchedule =
                                dueBlock.schedule

                            Log.d(
                                TAG,
                                "MEDICATION ALREADY HANDLED: " +
                                        "$today ${dueBlock.schedule}"
                            )

                            return
                        }

                        // Not handled yet — show medication table.
                        lastReportedSchedule =
                            dueBlock.schedule

                        Log.d(
                            TAG,
                            "MEDICATION DUE: ${dueBlock.schedule}"
                        )

                        callback.invoke(dueBlock)
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        Log.e(
                            TAG,
                            "MEDICATION LOG CHECK FAILED",
                            error.toException()
                        )
                    }
                }
            )
    }
    fun stopClockWatcher() {

        medicationHandler.removeCallbacks(
            medicationClockRunnable
        )

        dueBlockCallback = null

        Log.d(TAG, "Medication clock watcher stopped")
    }
}