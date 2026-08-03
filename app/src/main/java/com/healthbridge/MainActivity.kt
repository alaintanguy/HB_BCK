package com.healthbridge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// =====================================================
// MAIN ACTIVITY — coordinator only
// =====================================================
class MainActivity : AppCompatActivity() {

    companion object {
        const val MEMBER_ID = "M1"   // M1=Motorola M2=samsung
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

        uiManager = UIManager(this)
        uiManager.initialize()

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
        uiManager.setOnWriteClick {

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
                            android.widget.Toast.makeText(
                                this,
                                "SOAP Mode - Coming Soon",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .show()
        }

        // Microphone → speech recognition hook (preserved for SpeechManager integration)
        uiManager.setOnCopyClick {
            android.util.Log.d("HB", "Copy pressed")
        }

        uiManager.setOnSaveClick {
            android.util.Log.d("HB", "Save pressed")
        }
        uiManager.setOnSpeakClick {
            android.util.Log.d("HB", "Speak button pressed — starting speech recognition")
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
                startSpeechRecognition()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_CODE_RECORD_AUDIO
                )
            }
        }
        // Send → process message, clear editor, return to Conversation Mode
        uiManager.setOnSendClick {

            val text = uiManager.getComposeText()

            if (text.isNotBlank()) {

                messageManager.send(text)

                uiManager.appendMessage("Me: $text")

                uiManager.clearCompose()

                android.util.Log.d("HB", "Message sent: $text")
            }

            uiManager.showConversationMode()
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechRecognition()
            } else {
                android.widget.Toast.makeText(
                    this,
                    "Microphone permission required for speech recognition",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startSpeechRecognition() {
        speechManager.startListening(
            onResult = { text ->
                if (text.isNotBlank()) {
                    runOnUiThread { uiManager.appendToCompose(text) }
                }
            },
            onError = { msg ->
                runOnUiThread {
                    android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}