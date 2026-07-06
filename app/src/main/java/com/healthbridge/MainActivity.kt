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

        setContentView(R.layout.activity_main)

        Log.e(
            "HB",
            "********** HB STARTED  M1 **********"
        )

        statusText =
            findViewById(R.id.statusText)

        statusText.text =
            "BUILD JULY 7 - $MEMBER_ID"

        messageEdit =
            findViewById(R.id.messageEdit)
        ackStatus =
            findViewById(R.id.ackStatus)

        ackButton = findViewById(R.id.ackButton)

        sendButton = findViewById(R.id.sendButton)

        speakButton = findViewById(R.id.speakButton)

        speakButton.setOnClickListener {

            Log.d("HB", "MIC BUTTON PRESSED")

            statusText.text = "STARTING SPEECH"

            startSpeechRecognition()
        }

        sendButton.setOnClickListener {
            Log.d("HB", "SEND BUTTON PRESSED")

            sendDemoMessage()



        }

        messageEdit =
            findViewById(R.id.messageEdit)

        ackButton.setOnClickListener {

            acknowledgeMessage()
        }

        statusText.bringToFront()

       // statusText.text = "No message"
        statusText.text = "BUILD JULY 7 - $MEMBER_ID"
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

                Log.d("HB", "ROLE = $isPublisher")

                if (isPublisher) {
                    Log.d("HB", "I AM THE PATIENT")
                } else {
                    Log.d("HB", "I AM THE CAREGIVER")
                }

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

            .addListenerForSingleValueEvent(

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

        // -------------------------------------------------
        // Determine who receives the message
        // -------------------------------------------------

            //------------------------------------------------
            // Determine recipient
            //------------------------------------------------

            val target =
                if (MEMBER_ID == "M1")
                    "M2"
                else
                    "M1"

            //------------------------------------------------
            // Build message
            //------------------------------------------------

            val message = hashMapOf(

                "from" to MEMBER_ID,

                "text" to messageText,

                "timestamp" to
                        com.google.firebase.database.ServerValue.TIMESTAMP,

                "delivered" to false,

                "reply_from" to "",

                "reply_text" to "",

                "reply_timestamp" to 0L
            )

            //------------------------------------------------
            // Send
            //------------------------------------------------

            FirebaseDatabase
                .getInstance()
                .getReference(
                    "groups/family_001/messages/$target/msg_001"
                )
                .setValue(message)

                .addOnSuccessListener {

                    Log.d(
                        "HB",
                        "MESSAGE SENT TO $target"
                    )

                    messageEdit.setText("")

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
// SPEECH RECOGNITION
// =====================================================

    private val speechLauncher =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->

            Log.d("HB", "RESULT CODE = ${result.resultCode}")
            Log.d("HB", "RESULT DATA = ${result.data}")

            if (result.resultCode == Activity.RESULT_OK) {

                val matches =
                    result.data?.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                    )

                Log.d("HB", "MATCHES = $matches")

                if (!matches.isNullOrEmpty()) {

                    val spokenText = matches[0]

                    Log.d("HB", "SPEECH = $spokenText")

                    messageEdit.setText(spokenText)

                    statusText.text = spokenText

                    Log.d(
                        "HB",
                        "EDITTEXT NOW = ${messageEdit.text}"
                    )
                }

            } else {

                Log.d(
                    "HB",
                    "SPEECH CANCELLED OR FAILED"
                )
            }
        }
    private fun startSpeechRecognition() {
        Log.d("HB", "LAUNCHING SPEECH")

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale.getDefault()
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            "Speak your reminder"
        )
        Log.d("HB", "LAUNCHING SPEECH")

        speechLauncher.launch(intent)
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

            listenToMessages()
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
                "groups/family_001/messages/$MEMBER_ID/$currentMessageId/acknowledged"
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
                "groups/family_001/messages/$MEMBER_ID/msg_001"
            )
            .addValueEventListener(

                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        Log.d("HB", "MESSAGE LISTENER FIRED")

                        if (!snapshot.exists()) {
                            return
                        }

                        val delivered =
                            snapshot.child("delivered")
                                .getValue(Boolean::class.java)
                                ?: true

                        if (delivered) {

                            Log.d(
                                "HB",
                                "MESSAGE ALREADY DELIVERED"
                            )

                            return
                        }

                        val from =
                            snapshot.child("from")
                                .getValue(String::class.java)
                                ?: ""

                        val senderName =
                            when (from) {
                                "M1" -> "Alain"
                                "M2" -> "Mary"
                                else -> from
                            }

                        val message =
                            snapshot.child("text")
                                .getValue(String::class.java)
                                ?: ""
                        val timestamp =
                            snapshot.child("timestamp")
                                .getValue(Long::class.java)
                                ?: 0L

                        Log.d(
                            "HB",
                            "NEW MESSAGE FROM $from : $message"
                        )

                        currentMessage = message

                        runOnUiThread {

                            messageEdit.setText(
                                "From: $senderName\n\n$message"
                            )

                            statusText.text = "Message from $senderName"

                        }   // <-- THIS BRACE IS MISSING

                        textToSpeech.speak(
                            message,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "HB_MESSAGE"
                        )

                        //--------------------------------------------------
                        // Mark message as delivered
                        //--------------------------------------------------

                        snapshot.ref
                            .child("delivered")
                            .setValue(true)
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

}