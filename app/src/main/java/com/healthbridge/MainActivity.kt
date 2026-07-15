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

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.healthbridge.firebase.FirebaseManager
import com.healthbridge.telemetry.TelemetryEngine
import java.util.Locale

// =====================================================
// MAIN ACTIVITY
// =====================================================

class MainActivity :
    AppCompatActivity() {

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

    private lateinit var speechManager: SpeechManager



    private lateinit var messageManager: MessageManager

    private lateinit var sendButton: Button
    private lateinit var speakButton: Button

    // =====================================================
    // TELEMETRY
    // =====================================================

    private lateinit var telemetryEngine:
            TelemetryEngine

    private var isPublisher = true

    private lateinit var mapManager: MapManager

    private lateinit var statusText: TextView
    private lateinit var messageEdit: EditText
    private lateinit var ackStatus: TextView



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

        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )

        Log.i(
            "HB",
            "========================================"
        )

        Log.i(
            "HB",
            "HealthBridge STARTED  -  $MEMBER_ID"
        )

        Log.i(
            "HB",
            "========================================"
        )
        Log.i(
            "HB",
            "HealthBridge STARTED  -  $MEMBER_ID"
        )

        Log.i(
            "HB",
            "========================================"
        )
        speechManager = SpeechManager(this)
        speechManager.initialize()

        messageManager = MessageManager(MEMBER_ID)

        statusText =
            findViewById(R.id.statusText)

        statusText.text =
            "No pending messages"

        messageEdit =
            findViewById(R.id.messageEdit)
        ackStatus =
            findViewById(R.id.ackStatus)

        messageEdit.clearFocus()

        ackButton = findViewById(R.id.ackButton)

        sendButton = findViewById(R.id.sendButton)

        speakButton = findViewById(R.id.speakButton)

        speakButton.setOnClickListener {
            statusText.text = "STARTING SPEECH"

            startSpeechRecognition()
        }

        sendButton.setOnClickListener {
            sendDemoMessage()

        }

        ackButton.setOnClickListener {

            acknowledgeMessage()
        }

        statusText.bringToFront()

        // statusText.text = "No message"
        statusText.setOnClickListener {

            acknowledgeMessage()
        }

        mapManager =
            MapManager(supportFragmentManager)

        mapManager.initialize()

        loadRole()
        // authenticateFirebase()

    }

    private fun authenticateFirebase() {

        FirebaseAuth.getInstance()
            .signInAnonymously()

            .addOnSuccessListener {

                Log.d(
                    "HB",
                    "FIREBASE AUTH SUCCESS"
                )

                loadRole()
            }

            .addOnFailureListener { e ->

                Log.e(
                    "HB",
                    "FIREBASE AUTH FAILED",
                    e
                )

                // Continue anyway while Firebase rules are open
                loadRole()
            }
    }

    private fun loadRole() {


        FirebaseManager
            .memberReference(MEMBER_ID)
            .child("profile")
            .child("role")

            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    Log.d(
                        "HB",
                        "ROLE VALUE = ${snapshot.value}"
                    )

                    val role =
                        snapshot.getValue(String::class.java)
                            ?: "caregiver"

                    Log.d(
                        "HB",
                        "ROLE = $role"
                    )

                    isPublisher =
                        (role == "patient")

                    startAccordingToRole()
                }

                override fun onCancelled(error: DatabaseError) {

                    Log.e(
                        "HB",
                        "ROLE LISTENER FAILED",
                        error.toException()
                    )
                }
            })
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

                        runOnUiThread {
                            mapManager.updateMemberLocation(
                                memberId,
                                memberName,
                                latitude,
                                longitude
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

        speechManager.shutdown()

        super.onDestroy()
    }

// =====================================================
//  DEMO FUNCTIONS
// =====================================================

    private fun sendDemoMessage() {

        val messageText =
            messageEdit.text.toString().trim()

        if (messageText.isEmpty())
            return

        val target =
            if (MEMBER_ID == "M1")
                "M2"
            else
                "M1"

        messageManager.send(messageText)

        messageEdit.setText("")
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
        speechLauncher.launch(intent)
    }

// =====================================================
// ROLE MANAGEMENT
// =====================================================

    private fun startAccordingToRole() {
        Log.d(
            "HB",
            "START ACCORDING TO ROLE MEMBER=$MEMBER_ID isPublisher=$isPublisher"
        )

        if (isPublisher) {

            Log.d(
                "HB",
                "START ACCORDING TO ROLE MEMBER=M2 isPublisher=true"
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

            // Show BOTH members on the map
            listenToMember("M1")
            listenToMember("M2")

            listenToMessages()

        } else {

            // Viewer also shows BOTH members
            listenToMember("M1")
            listenToMember("M2")

            listenToMessages()
        }
    }

// =====================================================
// MESSAGING
// =====================================================

    private fun acknowledgeMessage() {

        currentMessage = ""

        messageEdit.setText("")

        statusText.text = "No pending messages"

        Log.d(
            "HB",
            "MESSAGE CLEARED"
        )
    }

    private fun listenToMessages() {

        Log.d(
            "HB",
            "MainActivity MEMBER_ID=$MEMBER_ID"
        )
        statusText.text = "LISTENER STARTED : $MEMBER_ID"

        messageManager.startListening { from, text ->

            currentMessage = text

            runOnUiThread {

                val senderName =
                    when (from) {

                        "M1" -> "Alain"

                        "M2" -> "Mary"

                        else -> from
                    }

                messageEdit.setText(
                    "From: $senderName\n\n${text}"
                )

                statusText.text = "Message from $senderName"


                Log.d(
                    "HB",
                    "TTS SPEAKING MESSAGE FOR MEMBER=$MEMBER_ID"
                )

                speechManager.speak(text)

            }
        }
    }
}