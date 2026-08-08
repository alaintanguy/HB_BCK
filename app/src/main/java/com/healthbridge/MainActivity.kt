package com.healthbridge

import android.Manifest
import com.healthbridge.firebase.FirebaseManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// =====================================================
// MAIN ACTIVITY — coordinator only
// =====================================================
class MainActivity : AppCompatActivity() {

    companion object {
        const val MEMBER_ID = "M1" //M1= Samsung for Caregiver M2= Moto for Patient
        private const val REQUEST_CODE_RECORD_AUDIO = 200
    }

    private lateinit var mapManager: MapManager


    private lateinit var speechManager: SpeechManager
    private lateinit var uiManager: UIManager

    private lateinit var messageManager: MessageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        android.util.Log.d("HB", "ONCREATE START")

        // =====================================================
        // INITIALIZATION
        // =====================================================
        mapManager = MapManager(this)
        mapManager.initialize()
        if (MEMBER_ID == "M1") {

            FirebaseManager.listenToMemberLocation("M2") { lat, lng ->

                runOnUiThread {

                    mapManager.updatePatientMarker(
                        lat,
                        lng,
                        "Mary"
                    )
                }
            }
        }

        uiManager = UIManager(this)
        uiManager.initialize()
        uiManager.configureForMember(MEMBER_ID)

        messageManager = MessageManager(MEMBER_ID)

        speechManager = SpeechManager(this)
        speechManager.initialize()

        messageManager.startListening { from, text ->

            runOnUiThread {

                uiManager.appendMessage("$from: $text")

                speechManager.speak(text)

                android.util.Log.d(
                    "HB",
                    "MESSAGE RECEIVED: $text")
            }
        }

        android.util.Log.d("HB", "MANAGERS INITIALIZED")

        // =====================================================
        // BUTTON WIRING
        // =====================================================

        // Write → Compose Mode
        // Write button
        uiManager.setOnWriteClick {

            if (MEMBER_ID == "M1") {

                // M1 CAREGIVER:
                // choose Message To Patient or SOAP Note

                AlertDialog.Builder(this)
                    .setTitle("WRITE MODE")
                    .setItems(
                        arrayOf(
                            "Message To Patient",
                            "SOAP Note"
                        )
                    ) { _, which ->

                        when (which) {

                            0 -> {
                                uiManager.showPatientComposeMode()
                            }

                            1 -> {
                                uiManager.showSoapComposeMode()
                            }
                        }
                    }
                    .show()

            } else {

                // M2 PATIENT:
                // go directly to message compose
                uiManager.showPatientComposeMode()
            }
        }

        // Microphone → speech recognition hook (preserved for SpeechManager integration)
        uiManager.setOnCopyClick {

            val conversation = uiManager.getConversationText()

            val clipboard =
                getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager

            val clip =
                android.content.ClipData.newPlainText(
                    "HealthBridge Conversation",
                    conversation
                )

            clipboard.setPrimaryClip(clip)

            Toast.makeText(
                this,
                "Conversation copied.",
                Toast.LENGTH_SHORT
            ).show()

            android.util.Log.d("HB", "Conversation copied")
        }

        uiManager.setOnSaveClick {

            val conversation = uiManager.getConversationText()

            try {

                val documents =
                    android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOCUMENTS
                    )

                val hbFolder =
                    File(documents, "HealthBridge")

                if (!hbFolder.exists()) {
                    hbFolder.mkdirs()
                }

                val timestamp =
                    SimpleDateFormat(
                        "yyyy-MM-dd_HH-mm-ss",
                        Locale.US
                    ).format(Date())

                val file =
                    File(
                        hbFolder,
                        "Conversation_$timestamp.txt"
                    )

                file.writeText(
                    conversation,
                    Charsets.UTF_8
                )

                Toast.makeText(
                    this,
                    "Conversation saved.",
                    Toast.LENGTH_SHORT
                ).show()

                android.util.Log.d(
                    "HB",
                    "Conversation saved: ${file.absolutePath}"
                )

            } catch (e: Exception) {

                android.util.Log.e(
                    "HB",
                    "Save failed",
                    e
                )

                Toast.makeText(
                    this,
                    "Save failed.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        uiManager.setOnSpeakClick {

            android.util.Log.d("HB", "Speak button pressed")

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED) {

                startSpeechRecognition()

            } else {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_CODE_RECORD_AUDIO
                )
            }
        }

        // Back → return to conversation without sending
        uiManager.setOnBackClick {

            uiManager.showConversationMode()
        }
        // Send → process message, clear editor, return to Conversation Mode
        uiManager.setOnSendClick {

            if (uiManager.isSoapMode()) {

                val note = uiManager.getComposeText()

                if (note.isNotBlank()) {

                    try {

                        val documents =
                            android.os.Environment.getExternalStoragePublicDirectory(
                                android.os.Environment.DIRECTORY_DOCUMENTS
                            )

                        val soapFolder =
                            File(documents, "HealthBridge/SOAP")

                        if (!soapFolder.exists()) {
                            soapFolder.mkdirs()
                        }

                        val timestamp =
                            SimpleDateFormat(
                                "yyyy-MM-dd_HH-mm-ss",
                                Locale.US
                            ).format(Date())

                        val file =
                            File(
                                soapFolder,
                                "SOAP_$timestamp.txt"
                            )

                        file.writeText(note, Charsets.UTF_8)

                        Toast.makeText(
                            this,
                            "SOAP Note Saved",
                            Toast.LENGTH_SHORT
                        ).show()

                        android.util.Log.d(
                            "HB",
                            "SOAP Note saved: ${file.absolutePath}"
                        )

                    } catch (e: Exception) {

                        android.util.Log.e("HB", "SOAP save failed", e)

                        Toast.makeText(
                            this,
                            "Save failed.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                uiManager.clearCompose()
                uiManager.showConversationMode()

            } else {

                val text = uiManager.getComposeText()

                if (text.isNotBlank()) {

                    messageManager.send(text)

                    uiManager.appendMessage("$MEMBER_ID: $text")

                    uiManager.clearCompose()

                    android.util.Log.d("HB", "Message sent: $text")
                }

                uiManager.showConversationMode()
            }
        }
        // =====================================================
        // START IN CONVERSATION MODE
        // =====================================================
        uiManager.showConversationMode()

        android.util.Log.d("HB", "ONCREATE COMPLETE")
    }
    override fun onDestroy() {
        super.onDestroy()
        speechManager.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == REQUEST_CODE_RECORD_AUDIO) {

            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startSpeechRecognition()

            } else {

                Toast.makeText(
                    this,
                    "Microphone permission required.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun startSpeechRecognition() {

        speechManager.startListening(

            onResult = { text ->

                if (text.isNotBlank()) {

                    runOnUiThread {

                        uiManager.appendToCompose(text)

                    }
                }
            },

            onError = { msg ->

                runOnUiThread {

                    Toast.makeText(
                        this,
                        msg,
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }
        )
    }

}