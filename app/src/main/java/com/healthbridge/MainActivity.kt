// ====================================================================
// HealthBridge
// MainActivity.kt
// Version: 1.0 (Cleanup in progress)
// ====================================================================
//
// NOTE:
// First cleanup pass.
// Functionality intentionally unchanged.
// ====================================================================

package com.healthbridge

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.healthbridge.firebase.FirebaseManager
import com.healthbridge.telemetry.TelemetryEngine
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.FirebaseDatabase
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.widget.Button
import android.widget.EditText
import android.speech.RecognizerIntent
import android.app.Activity

// =====================================================
// MAIN ACTIVITY
// =====================================================

class MainActivity :
    AppCompatActivity(),
    OnMapReadyCallback {

    // =====================================================
    // CONFIGURATION
    // =====================================================

    companion object {
        const val MEMBER_ID = "M1"   // M1=Motorola M2=samsung


    }

    // =====================================================
    // UI
    // =====================================================

    private lateinit var ackButton: Button

    private var fullMessageVisible = false

    private var currentMessage = ""

    private var currentMessageId = ""


    private lateinit var sendButton: Button
    private lateinit var speakButton: Button


    // =====================================================
    // TELEMETRY
    // =====================================================

    private lateinit var telemetryEngine:
            TelemetryEngine


    private var isPublisher = true

    private lateinit var googleMap:
            GoogleMap

    private var memberMarker:
            Marker? = null

    private lateinit var statusText: TextView
    private lateinit var messageEdit: EditText
    private lateinit var ackStatus: TextView

    private lateinit var textToSpeech: TextToSpeech

    private var lastMessage = ""

    // =====================================================
    // PERMISSIONS
    // =====================================================

    private fun hasLocationPermission(): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            100
        )
    }

    private fun openAppSettings() {

        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        )

        intent.data = Uri.fromParts(
            "package",
            packageName,
            null
        )

        startActivity(intent)
    }

    // =====================================================
    // ACTIVITY LIFECYCLE
    // =====================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )
        textToSpeech = TextToSpeech(this) { status ->

            if (status == TextToSpeech.SUCCESS) {

                textToSpeech.language = Locale.US

                Log.d(
                    "HB",
                    "TTS READY"
                )

            } else {

                Log.e(
                    "HB",
                    "TTS INIT FAILED"
                )
            }
        }
        statusText =
            findViewById(R.id.statusText)

        messageEdit =
            findViewById(R.id.messageEdit)

        ackStatus =
            findViewById(R.id.ackStatus)

        ackButton = findViewById(R.id.ackButton)

        sendButton = findViewById(R.id.sendButton)

        sendButton.setOnClickListener {

            sendDemoMessage()
        }

        messageEdit =
            findViewById(R.id.messageEdit)

        ackButton.setOnClickListener {

            acknowledgeMessage()
        }

        statusText.bringToFront()

        statusText.text = "No message"
        statusText.setOnClickListener {

            acknowledgeMessage()
        }

        val mapFragment =
            supportFragmentManager
                .findFragmentById(
                    R.id.map
                ) as SupportMapFragment

        mapFragment.getMapAsync(this)

        loadRole()
        /* for demo
        sendButton.visibility =
            if (isPublisher)
                View.GONE
            else
                View.VISIBLE
*/
    }

    private fun loadRole() {

        Log.d(
            "HB",
            "LOAD ROLE CALLED"
        )

        FirebaseManager
            .memberReference(MEMBER_ID)
            .child("profile")
            .child("role")
            .get()
            .addOnSuccessListener { snapshot ->

                val role =
                    snapshot.getValue(String::class.java)
                        ?: "caregiver"

                isPublisher =
                    role == "patient"

                Log.d(
                    "HB",
                    "ROLE = $role  PUBLISHER = $isPublisher"
                )

                startAccordingToRole()
            }
    }


    private fun displayMessage() {


        if (currentMessage.length <= 80) {

            statusText.text =
                currentMessage

            return
        }

        if (fullMessageVisible) {

            statusText.text =
                currentMessage +
                        "\n\n[LESS]"

        } else {

            statusText.text =
                currentMessage.take(80) +
                        "..." +
                        "\n\n[MORE]"
        }


    }


    override fun onMapReady(
        map: GoogleMap
    ) {

        googleMap = map

        val start =
            LatLng(
                38.5816,
                -122.5825
            )

        googleMap.moveCamera(
            CameraUpdateFactory
                .newLatLngZoom(
                    start,
                    15f
                )
        )
    }

    private fun listenToMember(
        memberId: String
    ) {
        Log.d(
            "HB",
            "LISTENER ADDED FOR $memberId"
        )

        FirebaseManager
            .memberReference(memberId)

            .addValueEventListener(

                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        val latitude =

                            snapshot
                                .child("telemetry")
                                .child("location")
                                .child("lat")
                                .getValue(Double::class.java)
                                ?: return

                        val longitude =
                            snapshot
                                .child("telemetry")
                                .child("location")
                                .child("lng")
                                .getValue(Double::class.java)
                                ?: return

                        Log.d(
                            "HB",
                            "$memberId MARKER: $latitude , $longitude"

                        )

                        val altitude =
                            snapshot
                                .child("telemetry")
                                .child("location")
                                .child("altitude")
                                .getValue(Double::class.java)
                                ?: 0.0

                        val timestamp =
                            snapshot
                                .child("telemetry")
                                .child("timestamp")
                                .getValue(Long::class.java)
                                ?: 0L

                        val battery =
                            snapshot
                                .child("device")
                                .child("phoneBattery")
                                .getValue(Int::class.java)
                                ?: 0

                        val lowBattery =
                            snapshot
                                .child("alerts")
                                .child("lowBattery")
                                .getValue(Boolean::class.java)
                                ?: false

                        val batteryStatus =
                            if (lowBattery)
                                "LOW BATTERY"
                            else
                                "OK"

                        Log.d(
                            "HB",
                            "FB LOCATION: $latitude , $longitude"
                        )

                        val memberName =
                            snapshot
                                .child("profile")
                                .child("name")
                                .getValue(String::class.java)
                                ?: memberId

                        val lastSeen =
                            snapshot
                                .child("device")
                                .child("lastSeen")
                                .getValue(Long::class.java)
                                ?: 0L

                        val ageMinutes =
                            (System.currentTimeMillis() - lastSeen) /
                                    60000

                        val status =
                            if (ageMinutes <= 2)
                                "ONLINE"
                            else
                                "OFFLINE"

                        val position =
                            LatLng(
                                latitude,
                                longitude
                            )

                        runOnUiThread {

                            if (
                                memberMarker == null
                            ) {

                                memberMarker =
                                    googleMap.addMarker(

                                        MarkerOptions()
                                            .position(position)
                                            .title(memberId)
                                            .icon(
                                                BitmapDescriptorFactory
                                                    .defaultMarker(
                                                        if (memberId == "M1")
                                                            BitmapDescriptorFactory.HUE_RED
                                                        else
                                                            BitmapDescriptorFactory.HUE_BLUE
                                                    )
                                            )
                                    )

                            } else {

                                memberMarker?.position =
                                    position
                            }

                            googleMap.animateCamera(
                                CameraUpdateFactory
                                    .newLatLng(position)
                            )

                            if (!isPublisher) {

                                statusText.text =
                                    if (status == "OFFLINE")
                                        "$memberName | OFFLINE | LS: ${ageMinutes}m"
                                    else if (lowBattery)
                                        "$memberName | ONLINE | LOW BATTERY $battery% | LS: ${ageMinutes}m"
                                    else
                                        "$memberName | ONLINE | Bat: $battery% | LS: ${ageMinutes}m"
                            }
                            Log.d(
                                "HB",
                                "MESSAGE SNAPSHOT = ${snapshot.value}"
                            )
                        }
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        Log.e(
                            "HB",
                            "FIREBASE READ FAILED",
                            error.toException()
                        )
                    }
                }
            )
    }

    override fun onDestroy() {

        textToSpeech.stop()
        textToSpeech.shutdown()

        super.onDestroy()
    }

// =====================================================
//  DEMO FUNCTIONS
// =====================================================

    private fun sendDemoMessage() {

        val messageText =
            messageEdit.text.toString().trim()

        if (messageText.isEmpty()) {
            return
        }

        val message = hashMapOf(
            "text" to messageText,
            "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP,
            "acknowledged" to false
        )

        FirebaseDatabase
            .getInstance()
            .getReference(
                "groups/family_001/messages/M2/msg_001"
            )
            .setValue(message)
            .addOnSuccessListener {

                Log.d(
                    "HB",
                    "DEMO MESSAGE SENT"
                )

                // Ready for the next reminder
                messageEdit.setText("")
                ackStatus.text = "Last ACK: Waiting..."

            }
            .addOnFailureListener { error ->

                Log.e(
                    "HB",
                    "SEND FAILED",
                    error
                )
            }
    }

// =====================================================
// ROLE MANAGEMENT
// =====================================================

    private fun startAccordingToRole() {
        Log.d(
            "HB",
            "LOAD ROLE CALLED"
        )

        if (isPublisher) {

            Log.d(
                "HB",
                "STARTING TELEMETRY ENGINE"
            )

            if (!hasLocationPermission()) {

                requestLocationPermission()

                return
            }

            telemetryEngine =
                TelemetryEngine(
                    this,
                    MEMBER_ID
                )

            telemetryEngine.start()

            listenToMessages()

        } else {

            listenToMember("M2")
        }
    }

// =====================================================
// MESSAGING
// =====================================================

    private fun acknowledgeMessage() {

        if (currentMessageId.isBlank()) {

            Log.d(
                "HB",
                "NO MESSAGE TO ACKNOWLEDGE"
            )

            return
        }

        Log.d(
            "HB",
            "ACKNOWLEDGE PRESSED"
        )


        FirebaseDatabase
            .getInstance()
            .getReference(
                "groups/family_001/messages/M2/$currentMessageId/acknowledged"
            )
            .setValue(true)

            .addOnSuccessListener {

                Log.d(
                    "HB",
                    "ACK SUCCESS"
                )

                currentMessage = ""
                currentMessageId = ""
                lastMessage = ""

                statusText.text = "No pending messages"
            }
            .addOnFailureListener { error ->

                Log.e(
                    "HB",
                    "ACK FAILED",
                    error
                )
            }
    }


    private fun listenToMessages() {


        FirebaseDatabase
            .getInstance()
            .getReference(
                "groups/family_001/messages/M2"
            )
            .addValueEventListener(

                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        var newestTimestamp = 0L
                        var newestMessage = "No messages"

                        for (messageSnapshot in snapshot.children) {

                            val acknowledged =
                                messageSnapshot
                                    .child("acknowledged")
                                    .getValue(Boolean::class.java)
                                    ?: false

                            if (acknowledged) {
                                continue
                            }

                            val timestamp =
                                messageSnapshot
                                    .child("timestamp")
                                    .getValue(Long::class.java)
                                    ?: 0L

                            val text =
                                messageSnapshot
                                    .child("text")
                                    .getValue(String::class.java)
                                    ?: ""

                            val messageId =
                                messageSnapshot.key ?: ""

                            if (timestamp > newestTimestamp) {

                                newestTimestamp = timestamp
                                newestMessage = text
                                currentMessageId = messageId
                            }
                        }
                        val message = newestMessage

                        Log.d(
                            "HB",
                            "SELECTED MESSAGE = $newestMessage  ID = $currentMessageId"
                        )

                        Log.d(
                            "HB",
                            "SPEAKING MESSAGE: $message"
                        )

                        textToSpeech.speak(
                            message,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "HB_MESSAGE"
                        )

                        lastMessage = message

                        runOnUiThread {

                            currentMessage = message

                            displayMessage()
                        }
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {
                    }
                }
            )


    }

}