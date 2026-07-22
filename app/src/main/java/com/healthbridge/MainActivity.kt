// ====================================================================
// HealthBridge
// MainActivity.kt
// Version: 1.0 (Cleanup in progress)
// ====================================================================
//
// NOTE:
// Phase 5A: UI responsibilities moved to UIManager.
// Phase 5B: Permission management moved to PermissionManager.
// Phase 5C: Role management moved to RoleManager.
// Functionality intentionally unchanged.
// ====================================================================

package com.healthbridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
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

    private lateinit var uiManager: UIManager

    private var fullMessageVisible = false

    private var currentMessage = ""

    private lateinit var speechManager: SpeechManager


    private lateinit var messageManager: MessageManager


    // =====================================================
    // TELEMETRY
    // =====================================================

    private lateinit var telemetryEngine:
            TelemetryEngine

    private var isPublisher = true

    private lateinit var mapManager: MapManager

    // =====================================================
    // ROLE MANAGER
    // =====================================================

    private lateinit var roleManager: RoleManager

    // =====================================================
    // PERMISSION MANAGER
    // =====================================================

    private lateinit var permissionManager: PermissionManager

    private var lastReceivedMessage = ""


    // =====================================================

    // PERMISSIONS
    // =====================================================
    // Delegated to PermissionManager (Phase 5B)

    // =====================================================


    // ACTIVITY LIFECYCLE
    // =====================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

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

        uiManager = UIManager(this)
        uiManager.initialize(
            onSpeak = { startSpeechRecognition() },

            onWrite = {
                uiManager.enterComposeMode()
            },

            onSend = { sendDemoMessage() },

            onAck = { acknowledgeMessage() }
        )

        speechManager = SpeechManager(this)
        speechManager.initialize()


        messageManager = MessageManager(MEMBER_ID)

        mapManager =
            MapManager(supportFragmentManager)

        mapManager.initialize()

        permissionManager = PermissionManager(this)

        roleManager = RoleManager(MEMBER_ID)
        roleManager.loadRole { isPublisher ->
            this.isPublisher = isPublisher
            startAccordingToRole()
        }
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

                roleManager.loadRole { isPublisher ->
                    this.isPublisher = isPublisher
                    startAccordingToRole()
                }
            }

            .addOnFailureListener { e ->

                Log.e(
                    "HB",
                    "FIREBASE AUTH FAILED",
                    e
                )

                // Continue anyway while Firebase rules are open
                roleManager.loadRole { isPublisher ->
                    this.isPublisher = isPublisher
                    startAccordingToRole()
                }
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

                                    uiManager.showStatus(
                                        if (status == "OFFLINE")
                                            "$memberName | OFFLINE | LS: ${ageMinutes}m"
                                        else if (lowBattery)
                                            "$memberName | ONLINE | LOW BATTERY $battery% | LS: ${ageMinutes}m"
                                        else
                                            "$memberName | ONLINE | Bat: $battery% | LS: ${ageMinutes}m"
                                    )
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

            if (::roleManager.isInitialized) {
                roleManager.cleanup()
            }

            speechManager.shutdown()

            super.onDestroy()
        }

// =====================================================
//  DEMO FUNCTIONS
// =====================================================

        private fun sendDemoMessage() {

            val messageText = uiManager.getMessageText()

            if (messageText.isEmpty())
                return

            val myName =
                if (MEMBER_ID == "M1") "Alain" else "Mary"

            // FIX: send the message to Firebase so the remote device receives it.
            // This call was missing, which caused outgoing messages to appear
            // only locally and never reach the other member.
            messageManager.send(messageText)

            // Append the sent message to the local conversation immediately,
            // without waiting for the Firebase round-trip.
            uiManager.appendConversation(
                "From: $myName\n$messageText"
            )

            Log.d("HB", "TEXT TO SEND = '$messageText'")
            Log.d("HB", "LENGTH = ${messageText.length}")

            uiManager.clearMessageInput()
            uiManager.exitComposeMode()
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

                        // Enter compose mode first so the editor is
                        // visible before showMessageInput sets its text.
                        uiManager.enterComposeMode()

                        uiManager.showMessageInput(spokenText)

                        Log.d(
                            "HB",
                            "SPEECH INPUT NOW = $spokenText"
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

                if (!permissionManager.hasLocationPermission()) {

                    permissionManager.requestLocationPermission()

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

            uiManager.clearAck()

            Log.d(
                "HB",
                "MESSAGE CLEARED"
            )
        }

    // =====================================================
// MESSAGING
// =====================================================

    private fun listenToMessages() {

        Log.d(
            "HB",
            "MainActivity MEMBER_ID=$MEMBER_ID"
        )

        uiManager.showStatus("LISTENER STARTED : $MEMBER_ID")

        messageManager.startListening { from, text ->

            // Ignore duplicate Firebase callbacks
            if (text == lastReceivedMessage)
                return@startListening

            lastReceivedMessage = text

            currentMessage = text

            runOnUiThread {

                val senderName =
                    when (from) {
                        "M1" -> "Alain"
                        "M2" -> "Mary"
                        else -> from
                    }

                val newEntry =
                    "From: $senderName\n$text"

                uiManager.appendConversation(newEntry)

                

                speechManager.speak(text)
            }
        }
    }
        }



