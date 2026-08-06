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

}

