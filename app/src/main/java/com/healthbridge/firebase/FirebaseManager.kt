package com.healthbridge.firebase

import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
data class VoiceMessage(

    val from: String = "",

    val to: String = "",

    val text: String = "",

    val timestamp: Any? = null,

    val status: String = "new"
)
object FirebaseManager {

    private val database: DatabaseReference =
        FirebaseDatabase
            .getInstance()
            .reference
    private fun messageReference(): DatabaseReference {

        return database
            .child("groups")
            .child("family_001")
            .child("messages")
            .child("current")
    }
    private fun messageHistoryReference(): DatabaseReference {

        return database
            .child("groups")
            .child("family_001")
            .child("messages")
            .child("history")
    }

    // =====================================================
    // MESSAGE STATES
    // =====================================================

    const val MSG_NEW = "new"

    const val MSG_PLAYING = "playing"

    const val MSG_PLAYED = "played"

    const val MSG_ACKNOWLEDGED = "acknowledged"

    fun sendMessage(
        from: String,
        to: String,
        text: String
    ) {

        val message = hashMapOf(

            "from" to from,

            "to" to to,

            "text" to text,

            "timestamp" to
                    com.google.firebase.database.ServerValue.TIMESTAMP,

            "status" to MSG_NEW
        )
// Save permanent copy in conversation history
        messageHistoryReference()
            .push()
            .setValue(message)
            .addOnSuccessListener {
                Log.d("HB", "MESSAGE SAVED TO HISTORY")
            }
            .addOnFailureListener { error ->
                Log.e("HB", "MESSAGE HISTORY SAVE FAILED", error)
            }
        // Keep existing live-message system
        messageReference()
            .setValue(message)
            .addOnSuccessListener {

                Log.d(
                    "HB",
                    "MESSAGE SENT: $from -> $to"
                )

            }
            .addOnFailureListener { error ->

                Log.e(
                    "HB",
                    "MESSAGE SEND FAILED",
                    error
                )
            }
    }



    fun markMessagePlayed() {

        messageReference()
            .child("status")
            .setValue(MSG_PLAYED)
            .addOnSuccessListener {

                Log.d(
                    "HB",
                    "MESSAGE MARKED PLAYED"
                )
            }
            .addOnFailureListener { error ->

                Log.e(
                    "HB",
                    "FAILED TO MARK MESSAGE PLAYED",
                    error
                )
            }
    }
    fun memberReference(
        memberId: String
    ): DatabaseReference {

        return database
            .child("groups")
            .child("family_001")
            .child("members")
            .child(memberId)
    }

    fun updateLocation(
        memberId: String,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ) {
        Log.d(
            "HB",
            "WRITING GPS: $latitude , $longitude"
        )
        val currentTime =
            System.currentTimeMillis()

        val readableDate =
            java.text.SimpleDateFormat(
                "yyyy-MM-dd",
                java.util.Locale.getDefault()
            ).format(
                java.util.Date(currentTime)
            )

        val readableTime =
            java.text.SimpleDateFormat(
                "HH:mm:ss",
                java.util.Locale.getDefault()
            ).format(
                java.util.Date(currentTime)
            )

        val updates =
            mapOf(
                "telemetry/location/lat" to latitude,
                "telemetry/location/lng" to longitude,
                "telemetry/location/altitude" to altitude,

                "telemetry/timestamp" to currentTime,

                "telemetry/readable/date" to readableDate,
                "telemetry/readable/time" to readableTime
            )

        memberReference(memberId)
            .updateChildren(updates)
            .addOnSuccessListener {

                Log.d(
                    "HB",
                    "FIREBASE TELEMETRY SUCCESS"
                )
                Log.d(
                    "HB",
                    "FB MEMBER = $memberId"
                )

                Log.d(
                    "HB",
                    "FB PATH = groups/family_001/members/$memberId"
                )
            }
            .addOnFailureListener { error ->

                Log.e(
                    "HB",
                    "FIREBASE TELEMETRY FAILED",
                    error
                )
            }
    }

    fun updateLastSeen(
        memberId: String
    ) {

        memberReference(memberId)
            .child("device")
            .child("lastSeen")
            .setValue(
                System.currentTimeMillis()
            )
    }
    fun listenToLastSeen(
        memberId: String,
        onChanged: (Long) -> Unit
    ) {
        memberReference(memberId).child("device").child("lastSeen")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onChanged(snapshot.getValue(Long::class.java) ?: 0L)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("HB", "LAST SEEN LISTENER FAILED: $memberId", error.toException())
                }
            })
    }

    fun updateHeartbeat(
        memberId: String
    ) {
        val currentTime = System.currentTimeMillis()

        val readableDate =
            java.text.SimpleDateFormat(
                "yyyy-MM-dd",
                java.util.Locale.getDefault()
            ).format(java.util.Date(currentTime))

        val readableTime =
            java.text.SimpleDateFormat(
                "HH:mm:ss",
                java.util.Locale.getDefault()
            ).format(java.util.Date(currentTime))

        val updates = mapOf<String, Any>(
            "telemetry/timestamp" to currentTime,
            "telemetry/readable/date" to readableDate,
            "telemetry/readable/time" to readableTime,
            "device/lastSeen" to currentTime
        )

        memberReference(memberId)
            .updateChildren(updates)
            .addOnSuccessListener {
                Log.d("HB", "HEARTBEAT FIREBASE SUCCESS: $memberId $readableTime")
            }
            .addOnFailureListener { error ->
                Log.e("HB", "HEARTBEAT FIREBASE FAILED: $memberId", error)
            }
    }
    fun loadMessageHistory(
        memberId: String,
        onHistoryLoaded: (List<VoiceMessage>) -> Unit
    ) {

        messageHistoryReference()
            .orderByChild("timestamp")
            .addListenerForSingleValueEvent(

                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        val messages =
                            mutableListOf<VoiceMessage>()

                        for (child in snapshot.children) {

                            val message =
                                child.getValue(
                                    VoiceMessage::class.java
                                )

                            if (message != null) {

                                // Keep messages involving this member.
                                if (
                                    message.from == memberId ||
                                    message.to == memberId
                                ) {
                                    messages.add(message)
                                }
                            }
                        }

                        Log.d(
                            "HB",
                            "HISTORY LOADED: ${messages.size} messages"
                        )

                        onHistoryLoaded(messages)
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        Log.e(
                            "HB",
                            "MESSAGE HISTORY LOAD FAILED",
                            error.toException()
                        )
                    }
                }
            )
    }
    fun listenForMessages(

        memberId: String,

        onMessage: (VoiceMessage) -> Unit

    ) {

        Log.d(
            "HB",
            "MESSAGE LISTENER STARTED FOR memberId=$memberId"
        )

        messageReference()

            .addValueEventListener(

                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        Log.d("HB", "MESSAGE NODE CHANGED")
                        Log.d("HB", "RAW MESSAGE = ${snapshot.value}")

                        val message =
                            snapshot.getValue(
                                VoiceMessage::class.java
                            )

                        if (message == null) {

                            Log.d("HB", "VOICE MESSAGE IS NULL")
                            return
                        }

                        Log.d(
                            "HB",
                            "FROM=${message.from} TO=${message.to} STATUS=${message.status} TEXT=${message.text}"
                        )

                        if (message.to != memberId) {

                            Log.d("HB", "IGNORED - NOT FOR ME")
                            return
                        }

                        if (message.status != MSG_NEW) {

                            Log.d("HB", "IGNORED - STATUS=${message.status}")
                            return
                        }

                        Log.d("HB", "MESSAGE ACCEPTED")

                        onMessage(message)
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        Log.e(
                            "HB",
                            "MESSAGE LISTENER FAILED",
                            error.toException()
                        )
                    }
                }
            )
    }
    fun updateStatus(
        memberId: String,
        status: String
    ) {

        memberReference(memberId)
            .child("device")
            .child("status")
            .setValue(status)
    }

    fun updateBattery(
        memberId: String,
        battery: Int
    ) {
        Log.d(
            "HB",
            "WRITING BATTERY = $battery"
        )
        memberReference(memberId)
            .child("device")
            .child("phoneBattery")


            .setValue(battery)
            .addOnSuccessListener {


            }
            .addOnFailureListener { error ->

                Log.e(
                    "HB",
                    "FIREBASE BATTERY FAILED",
                    error
                )
            }
    }
    fun listenToLowBatteryThreshold(
        memberId: String,
        onThreshold: (Int) -> Unit
    ) {

        memberReference(memberId)
            .child("settings")
            .child("lowBatteryThreshold")
            .addValueEventListener(
                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        val threshold =
                            snapshot.getValue(Int::class.java)
                                ?: 20

                        onThreshold(threshold)
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {
                    }
                }
            )
    }
    fun updateLowBatteryAlert(
        memberId: String,
        isLow: Boolean
    ) {

        memberReference(memberId)
            .child("alerts")
            .child("lowBattery")
            .setValue(isLow)
    }


    fun listenForLowBatteryStatus(
        memberId: String,
        onChanged: (isLow: Boolean, battery: Int) -> Unit
    ) {
        memberReference(memberId)
            .addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val isLow =
                            snapshot.child("alerts").child("lowBattery")
                                .getValue(Boolean::class.java) ?: false

                        val battery =
                            snapshot.child("device").child("phoneBattery")
                                .getValue(Int::class.java) ?: -1

                        onChanged(isLow, battery)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(
                            "HB",
                            "LOW BATTERY LISTENER FAILED: $memberId",
                            error.toException()
                        )
                    }
                }
            )
    }

    // =====================================================
    // GEOFENCE ALERT — PROTOTYPE
    // =====================================================

    fun listenToHomeGeofence(
        memberId: String,
        onChanged: (latitude: Double, longitude: Double, radiusMeters: Double, enabled: Boolean) -> Unit
    ) {
        memberReference(memberId)
            .child("geofences")
            .child("home")
            .addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val latitude =
                            snapshot.child("lat").getValue(Double::class.java)

                        val longitude =
                            snapshot.child("lng").getValue(Double::class.java)

                        val radius =
                            snapshot.child("radius").getValue(Double::class.java)
                                ?: snapshot.child("radius").getValue(Long::class.java)?.toDouble()
                                ?: 150.0

                        val enabled =
                            snapshot.child("enabled").getValue(Boolean::class.java) ?: false

                        if (latitude == null || longitude == null) {
                            Log.e("HB", "GEOFENCE HOME INVALID: missing lat/lng for $memberId")
                            return
                        }

                        Log.d(
                            "HB",
                            "GEOFENCE HOME LOADED: $latitude , $longitude radius=$radius enabled=$enabled"
                        )

                        onChanged(latitude, longitude, radius, enabled)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(
                            "HB",
                            "GEOFENCE HOME LISTENER FAILED: $memberId",
                            error.toException()
                        )
                    }
                }
            )
    }

    fun updateGeofenceAlert(
        memberId: String,
        isOutside: Boolean,
        distanceMeters: Double
    ) {
        val updates = mapOf<String, Any>(
            "alerts/geofenceOutside" to isOutside,
            "geofences/home/distanceMeters" to distanceMeters,
            "geofences/home/lastChecked" to
                    com.google.firebase.database.ServerValue.TIMESTAMP
        )

        memberReference(memberId)
            .updateChildren(updates)
            .addOnSuccessListener {
                Log.d(
                    "HB",
                    "GEOFENCE UPDATED: $memberId outside=$isOutside distance=$distanceMeters"
                )
            }
            .addOnFailureListener { error ->
                Log.e(
                    "HB",
                    "GEOFENCE UPDATE FAILED: $memberId",
                    error
                )
            }
    }

    fun listenForGeofenceStatus(
        memberId: String,
        onChanged: (isOutside: Boolean, distanceMeters: Double) -> Unit
    ) {
        memberReference(memberId)
            .addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val isOutside =
                            snapshot.child("alerts")
                                .child("geofenceOutside")
                                .getValue(Boolean::class.java) ?: false

                        val distance =
                            snapshot.child("geofences")
                                .child("home")
                                .child("distanceMeters")
                                .getValue(Double::class.java) ?: 0.0

                        onChanged(isOutside, distance)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(
                            "HB",
                            "GEOFENCE LISTENER FAILED: $memberId",
                            error.toException()
                        )
                    }
                }
            )
    }

    // =====================================================
    // EMERGENCY ALERT SYSTEM
    // =====================================================

    enum class AlertSource {
        PATIENT_BUTTON, VOICE_HELP, PENDANT, POSSIBLE_FALL, NO_MOVEMENT,
        HEART_RATE, HEALTH_MEASUREMENT, GEOFENCE, AI_DETECTED
    }

    data class EmergencyAlert(
        val active: Boolean = false,
        val source: String = "",
        val details: String = "",
        val timestamp: Long = 0L
    )

    private fun emergencyAlertReference(memberId: String): DatabaseReference =
        memberReference(memberId).child("alerts").child("emergency")

    fun sendEmergencyAlert(memberId: String, source: AlertSource, details: String = "") {
        val alert = hashMapOf<String, Any>(
            "active" to true,
            "source" to source.name,
            "details" to details,
            "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP
        )
        emergencyAlertReference(memberId).setValue(alert)
            .addOnSuccessListener { Log.d("HB", "EMERGENCY ALERT SENT: $memberId ${source.name}") }
            .addOnFailureListener { e -> Log.e("HB", "EMERGENCY ALERT SEND FAILED: $memberId", e) }
    }

    fun clearEmergencyAlert(memberId: String) {
        emergencyAlertReference(memberId).child("active").setValue(false)
            .addOnSuccessListener { Log.d("HB", "EMERGENCY ALERT CLEARED: $memberId") }
            .addOnFailureListener { e -> Log.e("HB", "EMERGENCY ALERT CLEAR FAILED: $memberId", e) }
    }

    fun listenForEmergencyAlert(memberId: String, onAlertChanged: (EmergencyAlert) -> Unit) {
        emergencyAlertReference(memberId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onAlertChanged(EmergencyAlert(
                    active = snapshot.child("active").getValue(Boolean::class.java) ?: false,
                    source = snapshot.child("source").getValue(String::class.java) ?: "",
                    details = snapshot.child("details").getValue(String::class.java) ?: "",
                    timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                ))
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("HB", "EMERGENCY ALERT LISTENER FAILED: $memberId", error.toException())
            }
        })
    }

    // =====================================================
    // LOCATION LISTENER
    // =====================================================
    fun listenToMemberLocation(
        memberId: String,
        onLocation: (Double, Double) -> Unit
    ) {
        memberReference(memberId)
            .child("telemetry")
            .child("location")
            .addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val lat = snapshot.child("lat").getValue(Double::class.java)
                        val lng = snapshot.child("lng").getValue(Double::class.java)

                        if (lat != null && lng != null) {
                            Log.d("HB", "LOCATION RECEIVED: $memberId $lat,$lng")
                            onLocation(lat, lng)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(
                            "HB",
                            "LOCATION LISTENER FAILED: $memberId",
                            error.toException()
                        )
                    }
                }
            )
    }


    // =====================================================
    // MEDICATION HISTORY — M2 PATIENT
    // =====================================================

    private fun medicationHistoryReference(
        memberId: String,
        date: String,
        schedule: String
    ): DatabaseReference {

        val safeSchedule = schedule.replace(":", "-")

        return memberReference(memberId)
            .child("medications")
            .child("history")
            .child(date)
            .child(safeSchedule)
    }

    fun saveMedicationResult(
        memberId: String,
        schedule: String,
        missingMedicationNames: List<String>
    ) {

        val date = java.text.SimpleDateFormat(
            "yyyy-MM-dd",
            java.util.Locale.US
        ).format(java.util.Date())

        val status =
            if (missingMedicationNames.isEmpty()) "all_taken"
            else "missing"

        val result = hashMapOf<String, Any>(
            "status" to status,
            "missing" to missingMedicationNames.joinToString(", "),
            "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP
        )

        medicationHistoryReference(memberId, date, schedule)
            .setValue(result)
            .addOnSuccessListener {
                Log.d(
                    "HB-MED",
                    "MEDICATION RESULT SAVED: $memberId $date $schedule status=$status"
                )
            }
            .addOnFailureListener { error ->
                Log.e(
                    "HB-MED",
                    "MEDICATION RESULT SAVE FAILED: $memberId $date $schedule",
                    error
                )
            }
    }

    fun checkMedicationHandledToday(
        memberId: String,
        schedule: String,
        onResult: (Boolean) -> Unit
    ) {

        val date = java.text.SimpleDateFormat(
            "yyyy-MM-dd",
            java.util.Locale.US
        ).format(java.util.Date())

        medicationHistoryReference(memberId, date, schedule)
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.exists())
            }
            .addOnFailureListener { error ->
                Log.e(
                    "HB-MED",
                    "MEDICATION HISTORY CHECK FAILED: $memberId $date $schedule",
                    error
                )

                // If Firebase cannot be checked, do not silently suppress a due dose.
                onResult(false)
            }
    }

    // =====================================================
    // MEDICATION LOG
    // =====================================================

    fun recordMedicationBlock(
        memberId: String,
        schedule: String,
        status: String,
        missingMedications: List<String> = emptyList()
    ) {

        val today =
            java.text.SimpleDateFormat(
                "yyyy-MM-dd",
                java.util.Locale.US
            ).format(java.util.Date())

        val safeSchedule =
            schedule.replace(":", "-")

        val updates =
            hashMapOf<String, Any>(
                "schedule" to schedule,
                "status" to status,
                "timestamp" to
                        com.google.firebase.database.ServerValue.TIMESTAMP
            )

        if (missingMedications.isNotEmpty()) {
            updates["missing"] = missingMedications
        }

        memberReference(memberId)
            .child("medicationLog")
            .child(today)
            .child(safeSchedule)
            .setValue(updates)
            .addOnSuccessListener {

                Log.d(
                    "HB-MED",
                    "MEDICATION LOG SAVED: $today $schedule $status"
                )
            }
            .addOnFailureListener { error ->

                Log.e(
                    "HB-MED",
                    "MEDICATION LOG SAVE FAILED",
                    error
                )
            }
    }

    // =====================================================
    // WATCH DATA — GALAXY WATCH INTEGRATION (PHASE 2)
    // =====================================================

    fun listenForWatchData(
        onWatchDataChanged: (heartRate: Int, watchBattery: Int, timestamp: Long) -> Unit
    ) {
        database
            .child("watch_data")
            .child("latest")
            .addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val heartRate = snapshot.child("heart_rate").getValue(Int::class.java) ?: 0
                            val watchBattery = snapshot.child("battery").getValue(Int::class.java) ?: 0
                            val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()

                            Log.d("HB-WATCH", "Watch data received: HR=$heartRate, Battery=$watchBattery")
                            onWatchDataChanged(heartRate, watchBattery, timestamp)
                        } catch (e: Exception) {
                            Log.e("HB-WATCH", "Error parsing watch data", e)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("HB-WATCH", "Watch data listener failed", error.toException())
                    }
                }
            )
    }

    fun updateWatchHeartRate(
        memberId: String,
        heartRate: Int,
        timestamp: Long = System.currentTimeMillis()
    ) {
        Log.d("HB-WATCH", "Updating watch heart rate: $heartRate BPM")
        memberReference(memberId)
            .child("telemetry")
            .child("watch")
            .updateChildren(
                mapOf(
                    "heart_rate" to heartRate,
                    "heart_rate_timestamp" to timestamp
                )
            )
            .addOnFailureListener { error ->
                Log.e("HB-WATCH", "Failed to update watch heart rate", error)
            }
    }

    fun updateWatchBattery(
        memberId: String,
        watchBattery: Int,
        timestamp: Long = System.currentTimeMillis()
    ) {
        Log.d("HB-WATCH", "Updating watch battery: $watchBattery%")
        memberReference(memberId)
            .child("telemetry")
            .child("watch")
            .updateChildren(
                mapOf(
                    "battery" to watchBattery,
                    "battery_timestamp" to timestamp
                )
            )
            .addOnFailureListener { error ->
                Log.e("HB-WATCH", "Failed to update watch battery", error)
            }
    }


}