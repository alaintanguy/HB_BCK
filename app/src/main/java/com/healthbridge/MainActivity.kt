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
    private lateinit var continuousSpeechManager: ContinuousSpeechManager
    private lateinit var uiManager: UIManager

    private lateinit var messageManager: MessageManager

    private enum class CaregiverComposeMode { NONE, PATIENT, NOTE }
    private var lastComposeMode = CaregiverComposeMode.NONE
    private var noteRecording = false

    // Patient-message voice dictation stays active across recognition phrases.
    // Saying "SEND TO MARY" sends the accumulated message.
    private var patientDictationActive = false


    private var patientDictationBaseText = ""
    private var patientDictationCommittedText = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)        // YES
        setContentView(R.layout.activity_main)    // YES

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

        continuousSpeechManager = ContinuousSpeechManager(this)
        continuousSpeechManager.initialize(
            onReady = {
                Toast.makeText(
                    this,
                    "VOSK READY",
                    Toast.LENGTH_LONG
                ).show()
            },
            onError = { msg ->
                Toast.makeText(
                    this,
                    "VOSK ERROR: $msg",
                    Toast.LENGTH_LONG
                ).show()
            }
        )

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

            if (MEMBER_ID == "M1") {

                when (lastComposeMode) {
                    CaregiverComposeMode.PATIENT ->
                        uiManager.showPatientComposeMode()

                    CaregiverComposeMode.NOTE ->
                        uiManager.showSoapComposeMode()

                    CaregiverComposeMode.NONE -> {
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
                                        lastComposeMode = CaregiverComposeMode.PATIENT
                                        uiManager.showPatientComposeMode()
                                    }
                                    1 -> {
                                        lastComposeMode = CaregiverComposeMode.NOTE
                                        uiManager.showSoapComposeMode()
                                    }
                                }
                            }
                            .show()
                    }
                }

            } else {
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

                if (uiManager.isSoapMode()) {
                    if (!noteRecording) {
                        noteRecording = true
                        uiManager.showRecordingState(true)
                        startSpeechRecognition(5000L, true)
                    } else {
                        speechManager.stopListening()
                    }
                } else {
                    startPatientDictation()
                }

            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_CODE_RECORD_AUDIO
                )
            }
        }

        // Back → stop recording, preserve the COMPLETE visible draft,
        // then return to conversation without sending.
        uiManager.setOnBackClick {

            if (patientDictationActive) {
                patientDictationBaseText = uiManager.getComposeText().trim()
                patientDictationCommittedText = ""
            }

            patientDictationActive = false
            continuousSpeechManager.stopListening()
            uiManager.showRecordingState(false)
            uiManager.showConversationMode()
        }
        // Send → process message, clear editor, return to Conversation Mode
        uiManager.setOnSendClick {

            if (uiManager.isSoapMode()) {

                saveSoapNoteAndReturn()

            } else {

                patientDictationActive = false
                continuousSpeechManager.stopListening()

                val text = uiManager.getComposeText()

                if (text.isNotBlank()) {

                    messageManager.send(text)

                    uiManager.appendMessage("$MEMBER_ID: $text")

                    uiManager.clearCompose()
                    uiManager.showRecordingState(false)
                    speechManager.speak("Message sent")

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
    private fun saveSoapNoteAndReturn() {

        val note = uiManager.getComposeText()

        if (note.isBlank()) {
            noteRecording = false
            uiManager.showRecordingState(false)
            return
        }

        try {
            val documents =
                android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOCUMENTS
                )

            val soapFolder = File(documents, "HealthBridge/SOAP")

            if (!soapFolder.exists()) {
                soapFolder.mkdirs()
            }

            val timestamp =
                SimpleDateFormat(
                    "yyyy-MM-dd_HH-mm-ss",
                    Locale.US
                ).format(Date())

            val file = File(
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

            uiManager.clearCompose()
            noteRecording = false
            uiManager.showRecordingState(false)
            uiManager.showConversationMode()

        } catch (e: Exception) {

            noteRecording = false
            uiManager.showRecordingState(false)

            android.util.Log.e(
                "HB",
                "SOAP save failed",
                e
            )

            Toast.makeText(
                this,
                "Save failed.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechManager.shutdown()
        continuousSpeechManager.shutdown()
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

                if (uiManager.isSoapMode()) {
                    noteRecording = true
                    uiManager.showRecordingState(true)
                    startSpeechRecognition(5000L, true)
                } else {
                    startPatientDictation()
                }

            } else {

                Toast.makeText(
                    this,
                    "Microphone permission required.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun startSpeechRecognition(
        silenceMillis: Long,
        autoSaveNote: Boolean
    ) {

        speechManager.startListening(
            silenceMillis = silenceMillis,

            onResult = { text ->

                runOnUiThread {

                    if (text.isNotBlank()) {
                        uiManager.appendToCompose(text)
                    }

                    noteRecording = false
                    uiManager.showRecordingState(false)

                    if (autoSaveNote && text.isNotBlank()) {
                        saveSoapNoteAndReturn()
                    }
                }
            },

            onError = { msg ->

                runOnUiThread {

                    noteRecording = false
                    uiManager.showRecordingState(false)

                    Toast.makeText(
                        this,
                        msg,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // =====================================================
    // PATIENT MESSAGE DICTATION — VOSK CONTINUOUS
    // =====================================================

    private fun startPatientDictation() {
        if (patientDictationActive) return

        patientDictationActive = true
        patientDictationBaseText = uiManager.getComposeText().trim()
        patientDictationCommittedText = ""
        uiManager.showRecordingState(true)

        speechManager.speakThen("Ready to record") {
            if (!patientDictationActive) return@speakThen

            continuousSpeechManager.startListening(
                onText = { text ->
                    runOnUiThread {
                        if (patientDictationActive) {
                            handlePatientVoskText(text, true)
                        }
                    }
                },
                onPartial = { partial ->
                    runOnUiThread {
                        if (patientDictationActive) {
                            handlePatientVoskText(partial, false)
                        }
                    }
                },
                onError = { msg ->
                    runOnUiThread {
                        android.util.Log.e("HB", msg)
                        if (patientDictationActive) {
                            patientDictationActive = false
                            uiManager.showRecordingState(false)
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }

    private fun handlePatientVoskText(
        recognized: String,
        isFinalChunk: Boolean
    ) {
        if (!patientDictationActive) return

        val incoming = recognized.trim()
        if (incoming.isBlank()) return

        val liveNewSpeech =
            listOf(patientDictationCommittedText.trim(), incoming)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .trim()

        val commandRegex =
            Regex("""(?i)\bsend\s+(?:this\s+)?to\s+mary\b[.!?,;:]*""")
        val commandMatch = commandRegex.find(liveNewSpeech)

        if (commandMatch != null) {
            val spokenBeforeCommand =
                liveNewSpeech.substring(0, commandMatch.range.first).trim()

            val fullMessage =
                listOf(patientDictationBaseText, spokenBeforeCommand)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .trim()

            patientDictationActive = false
            continuousSpeechManager.stopListening()
            uiManager.showRecordingState(false)

            if (fullMessage.isNotBlank()) {
                messageManager.send(fullMessage)
                uiManager.appendMessage("$MEMBER_ID: $fullMessage")
                uiManager.clearCompose()
                android.util.Log.d("HB", "VOSK SEND TO MARY: $fullMessage")
            }

            patientDictationCommittedText = ""
            patientDictationBaseText = ""
            uiManager.showConversationMode()
            speechManager.speak("Message sent")
            return
        }

        val liveEditorText =
            listOf(patientDictationBaseText, liveNewSpeech)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .trim()

        uiManager.clearCompose()
        uiManager.appendToCompose(liveEditorText)

        if (isFinalChunk) {
            patientDictationCommittedText = liveNewSpeech
        }
    }
}