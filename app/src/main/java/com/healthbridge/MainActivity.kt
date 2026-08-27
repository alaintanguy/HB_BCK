package com.healthbridge

import android.Manifest
import com.healthbridge.firebase.FirebaseManager
import com.healthbridge.telemetry.TelemetryEngine
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
        const val MEMBER_ID = "M2" +"" // M1 = Caregiver, M2 = Patient
        private const val REQUEST_CODE_RECORD_AUDIO = 200
    }

    private lateinit var medicationManager: MedicationManager
    private lateinit var mapManager: MapManager

    private lateinit var speechManager: SpeechManager
    private lateinit var continuousSpeechManager: ContinuousSpeechManager
    private lateinit var uiManager: UIManager

    private lateinit var messageManager: MessageManager
    private lateinit var telemetryEngine: TelemetryEngine
    private lateinit var fallDetectionManager: FallDetectionManager

    private var fallDialog: android.app.Dialog? = null
    private var fallAlarmActive = false
    private val fallAlarmHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var fallToneGenerator: android.media.ToneGenerator? = null
    private val fallNoResponseHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val fallNoResponseRunnable = Runnable { handleFallNoResponse() }
    private val fallAlarmRunnable = object : Runnable {
        override fun run() {
            if (!fallAlarmActive) return
            if (fallToneGenerator == null) {
                fallToneGenerator = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
            }
            fallToneGenerator?.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700)
            fallAlarmHandler.postDelayed(this, 1200L)
        }
    }

    private enum class CaregiverComposeMode { NONE, PATIENT, NOTE }

    private var lastComposeMode = CaregiverComposeMode.NONE
    private var noteRecording = false

    // NOTE dictation uses the same live partial/final strategy as patient dictation,
    // but it never contains a spoken SEND command and never sends to Firebase.
    private var noteDictationBaseText = ""
    private var noteDictationCommittedText = ""

    // True only after Back leaves an unfinished caregiver draft.
    // The next Write resumes that draft directly; otherwise Write shows the mode chooser.
    private var resumeComposeAfterBack = false

    // M2: prevents Firebase active=false at app startup from being
    // mistaken for a newly acknowledged alert.
    private var m2EmergencyWasActive = false

    // M2 geofence audible alarm
    private var geofenceAlarmActive = false
    // Suppress repeat alarms after M2 acknowledges this excursion.
    // Reset only after M2 returns inside the geofence.
    private var geofenceExitAcknowledged = false
    private var geofenceMistakeDialog: android.app.Dialog? = null
    private val geofenceAlarmHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var geofenceToneGenerator: android.media.ToneGenerator? = null

    private val geofenceAlarmRunnable = object : Runnable {
        override fun run() {
            if (!geofenceAlarmActive) return
            if (geofenceToneGenerator == null) {
                geofenceToneGenerator = android.media.ToneGenerator(
                    android.media.AudioManager.STREAM_ALARM, 100
                )
            }
            geofenceToneGenerator?.startTone(
                android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700
            )
            geofenceAlarmHandler.postDelayed(this, 1200L)
        }
    }

    // Patient-message voice dictation stays active across recognition phrases.
    // Saying "SEND TO MARY" sends the accumulated message.
    private var patientDictationActive = false

    // Prototype battery monitoring for M2.
    private var lowBatteryThreshold = 20
    private var currentBatteryPercent = -1
    private var batteryReceiverRegistered = false

    private val batteryReceiver =
        object : android.content.BroadcastReceiver() {

            override fun onReceive(
                context: android.content.Context?,
                intent: android.content.Intent?
            ) {

                if (MEMBER_ID != "M2" || intent == null) return

                val level =
                    intent.getIntExtra(
                        android.os.BatteryManager.EXTRA_LEVEL,
                        -1
                    )

                val scale =
                    intent.getIntExtra(
                        android.os.BatteryManager.EXTRA_SCALE,
                        100
                    )

                if (level < 0 || scale <= 0) return

                val percent =
                    ((level * 100f) / scale).toInt()

                currentBatteryPercent = percent

                android.util.Log.d(
                    "HB",
                    "M2 BATTERY REPORTED = $percent%"
                )

                FirebaseManager.updateBattery(
                    "M2",
                    percent
                )

                evaluateLowBattery()
            }
        }

    private var patientDictationBaseText = ""
    private var patientDictationCommittedText = ""


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        android.util.Log.d(
            "HB",
            "ONCREATE START"
        )

        // =====================================================
        // INITIALIZATION
        // =====================================================

        mapManager = MapManager(this)
        mapManager.initialize()

        if (MEMBER_ID == "M1") {

            FirebaseManager.listenToMemberLocation(
                "M2"
            ) { lat, lng ->

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

        // =====================================================
        // M2 TELEMETRY
        // =====================================================

        if (MEMBER_ID == "M2") {

            telemetryEngine =
                TelemetryEngine(
                    this,
                    MEMBER_ID
                )

            telemetryEngine.start()

            android.util.Log.d(
                "HB",
                "M2 TELEMETRY ENGINE STARTED"
            )

            fallDetectionManager = FallDetectionManager(this) { accelerationG ->
                runOnUiThread { showPossibleFall(accelerationG) }
            }
            if (!fallDetectionManager.start()) {
                Toast.makeText(this, "Fall detection unavailable: no accelerometer", Toast.LENGTH_LONG).show()
            }

            FirebaseManager.listenForGeofenceStatus("M2") { isOutside, distanceMeters ->
                runOnUiThread {
                    if (isOutside) {
                        if (!geofenceExitAcknowledged) {
                            startGeofenceAlarm(distanceMeters)
                        }
                    } else {
                        geofenceExitAcknowledged = false
                        stopGeofenceAlarm()
                    }
                }
            }
        }

        // =====================================================
        // BATTERY MONITORING — PROTOTYPE
        // =====================================================

        if (MEMBER_ID == "M2") {

            FirebaseManager.listenToLowBatteryThreshold(
                "M2"
            ) { threshold ->

                lowBatteryThreshold = threshold

                android.util.Log.d(
                    "HB",
                    "LOW BATTERY THRESHOLD = $lowBatteryThreshold"
                )

                evaluateLowBattery()
            }

            registerReceiver(
                batteryReceiver,
                android.content.IntentFilter(
                    android.content.Intent.ACTION_BATTERY_CHANGED
                )
            )

            batteryReceiverRegistered = true

        } else if (MEMBER_ID == "M1") {

            FirebaseManager.listenForLowBatteryStatus(
                "M2"
            ) { isLow, battery ->

                runOnUiThread {

                    if (isLow && battery >= 0) {

                        uiManager.showLowBatteryWarning(
                            battery
                        )

                    } else {

                        uiManager.hideLowBatteryWarning()
                    }
                }
            }

            FirebaseManager.listenForGeofenceStatus(
                "M2"
            ) { isOutside, distanceMeters ->

                runOnUiThread {

                    if (isOutside) {

                        uiManager.showGeofenceWarning(
                            distanceMeters
                        )

                    } else {

                        uiManager.hideGeofenceWarning()
                    }
                }
            }
        }

        // =====================================================
        // WATCH TELEMETRY — M1 CAREGIVER (PHASE 2)
        // =====================================================

        if (MEMBER_ID == "M1") {
            FirebaseManager.listenToMemberWatchData("M2") { heartRate, watchBattery, timestamp ->
                runOnUiThread {
                    android.util.Log.d(
                        "HB-WATCH",
                        "M1 received watch data: HR=$heartRate Battery=$watchBattery Timestamp=$timestamp"
                    )
                }
            }
        }

        // =====================================================
        // MESSAGE MANAGER
        // =====================================================

        messageManager =
            MessageManager(MEMBER_ID)

        // =====================================================
        // MEDICATION SYSTEM — M2 PATIENT
        // =====================================================

        if (MEMBER_ID == "M2") {

            medicationManager =
                MedicationManager("M2")

            medicationManager.startListening { blocks ->

                android.util.Log.d(
                    "HB-MED",
                    "MAIN received ${blocks.size} medication blocks"
                )
            }

            medicationManager.startClockWatcher { dueBlock ->

                android.util.Log.d(
                    "HB-MED",
                    "M2 MEDICATION DUE ${dueBlock.schedule}"
                )

                runOnUiThread {

                    uiManager.showMedicationBlock(
                        dueBlock
                    ) { missingMedications ->

                        if (missingMedications.isEmpty()) {

                            // ALL TAKEN
                            android.util.Log.d(
                                "HB-MED",
                                "ALL MEDICATIONS TAKEN: ${dueBlock.schedule}"
                            )

                            FirebaseManager.recordMedicationBlock(
                                memberId = "M2",
                                schedule = dueBlock.schedule,
                                status = "taken"
                            )

                            Toast.makeText(
                                this,
                                "Medication recorded",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {

                            // REPORT MISSING
                            val missingNames =
                                missingMedications
                                    .joinToString(", ") { it.name }

                            val alertMessage =
                                "MEDICATION ALERT — ${dueBlock.schedule}\n" +
                                        "Missing: $missingNames"
                            FirebaseManager.recordMedicationBlock(
                                memberId = "M2",
                                schedule = dueBlock.schedule,
                                status = "missing",
                                missingMedications =
                                    missingMedications.map { it.name }
                            )

                            android.util.Log.d(
                                "HB-MED",
                                alertMessage
                            )

                            messageManager.send(
                                alertMessage
                            )

                            uiManager.appendMessage(
                                "$MEMBER_ID: $alertMessage"
                            )

                            Toast.makeText(
                                this,
                                "Missing medication reported",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

        // =====================================================
        // SPEECH
        // =====================================================

        speechManager =
            SpeechManager(this)

        speechManager.initialize()

        continuousSpeechManager =
            ContinuousSpeechManager(this)

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

        // =====================================================
        // MESSAGE LISTENER
        // =====================================================

        // =====================================================
// LOAD SAVED CONVERSATION HISTORY
// =====================================================

        FirebaseManager.loadMessageHistory(
            MEMBER_ID
        ) { history ->

            runOnUiThread {

                for (message in history) {

                    uiManager.appendMessage(
                        "${message.from}: ${message.text}"
                    )
                }

                android.util.Log.d(
                    "HB",
                    "CONVERSATION HISTORY DISPLAYED: ${history.size}"
                )
            }
        }
        messageManager.startListening { from, text ->

            runOnUiThread {

                uiManager.appendMessage(
                    "$from: $text"
                )

                speechManager.speak(text)

                android.util.Log.d(
                    "HB",
                    "MESSAGE RECEIVED: $text"
                )
            }
        }

        android.util.Log.d(
            "HB",
            "MANAGERS INITIALIZED"
        )

        // =====================================================
        // BUTTON WIRING
        // =====================================================

        // Write → Compose Mode
        uiManager.setOnWriteClick {

            if (MEMBER_ID == "M1") {

                if (
                    resumeComposeAfterBack &&
                    uiManager.getComposeText().isNotBlank() &&
                    lastComposeMode != CaregiverComposeMode.NONE
                ) {

                    resumeComposeAfterBack = false

                    when (lastComposeMode) {

                        CaregiverComposeMode.PATIENT ->
                            uiManager.showPatientComposeMode()

                        CaregiverComposeMode.NOTE ->
                            uiManager.showSoapComposeMode()

                        CaregiverComposeMode.NONE ->
                            Unit
                    }

                } else {

                    resumeComposeAfterBack = false

                    AlertDialog.Builder(this)
                        .setTitle("MESSAGE")
                        .setMessage(
                            "Choose what you want to write"
                        )
                        .setPositiveButton(
                            "TO MARY"
                        ) { _, _ ->

                            lastComposeMode =
                                CaregiverComposeMode.PATIENT

                            uiManager.showPatientComposeMode()
                        }
                        .setNegativeButton(
                            "NOTE TO ME"
                        ) { _, _ ->

                            lastComposeMode =
                                CaregiverComposeMode.NOTE

                            uiManager.showSoapComposeMode()
                        }
                        .show()
                }

            } else {

                // M2 must always remain in patient compose mode.
                resumeComposeAfterBack = false
                lastComposeMode =
                    CaregiverComposeMode.NONE

                uiManager.showPatientComposeMode()
            }
        }

        // =====================================================
        // M2 ALERT
        // =====================================================

        uiManager.setOnAlertClick {

            if (MEMBER_ID == "M2") {

                patientDictationActive = false
                noteRecording = false

                continuousSpeechManager.stopListening()
                speechManager.stopListening()

                uiManager.showRecordingState(false)

                showFullScreenSosConfirmation()
            }
        }

        uiManager.setOnAlertMistakeClick {

            if (MEMBER_ID == "M2") {

                Toast.makeText(
                    this,
                    "Alert cancelled",
                    Toast.LENGTH_SHORT
                ).show()

                uiManager.showConversationMode()
            }
        }

        uiManager.setOnRealAlertClick {

            if (MEMBER_ID == "M2") {

                FirebaseManager.sendEmergencyAlert(
                    memberId = "M2",
                    source =
                        FirebaseManager.AlertSource.PATIENT_BUTTON,
                    details =
                        "Patient pressed REAL ALERT"
                )

                uiManager.showConversationMode()
                uiManager.showAlertSent()
            }
        }

        // =====================================================
        // EMERGENCY ALERT LISTENER
        // =====================================================

        FirebaseManager.listenForEmergencyAlert(
            "M2"
        ) { alert ->

            runOnUiThread {

                if (MEMBER_ID == "M1") {

                    if (alert.active) {

                        uiManager.showCaregiverAlert(
                            alert.source
                        )

                    } else {

                        uiManager.hideEmergencyStatus()
                    }

                } else if (MEMBER_ID == "M2") {

                    if (alert.active) {

                        m2EmergencyWasActive = true
                        uiManager.showAlertSent()

                    } else if (m2EmergencyWasActive) {

                        m2EmergencyWasActive = false
                        uiManager.showAlertReceived()

                    } else {

                        uiManager.hideEmergencyStatus()
                    }
                }
            }
        }

        // M1 acknowledges by pressing the large red alert.
        uiManager.setOnEmergencyStatusClick {

            if (MEMBER_ID == "M1") {

                FirebaseManager.clearEmergencyAlert(
                    "M2"
                )

                Toast.makeText(
                    this,
                    "Mary alert acknowledged",
                    Toast.LENGTH_SHORT
                ).show()

            } else if (MEMBER_ID == "M2") {

                uiManager.hideEmergencyStatus()
            }
        }

        // =====================================================
        // COPY CONVERSATION
        // =====================================================

        uiManager.setOnCopyClick {

            val conversation =
                uiManager.getConversationText()

            val clipboard =
                getSystemService(
                    CLIPBOARD_SERVICE
                ) as android.content.ClipboardManager

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

            android.util.Log.d(
                "HB",
                "Conversation copied"
            )
        }

        // =====================================================
        // SAVE CONVERSATION
        // =====================================================

        uiManager.setOnSaveClick {

            val conversation =
                uiManager.getConversationText()

            try {

                val documents =
                    android.os.Environment
                        .getExternalStoragePublicDirectory(
                            android.os.Environment
                                .DIRECTORY_DOCUMENTS
                        )

                val hbFolder =
                    File(
                        documents,
                        "HealthBridge"
                    )

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

        // =====================================================
        // SPEAK / MICROPHONE BUTTON
        // =====================================================

        uiManager.setOnSpeakClick {
            android.util.Log.d("HB-COMPOSE", "SPEAK BUTTON PRESSED member=$MEMBER_ID")

            android.util.Log.d(
                "HB",
                "Speak button pressed"
            )

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {

                if (uiManager.isSoapMode()) {

                    if (!noteRecording) {
                        startNoteDictation()
                    } else {
                        stopNoteDictation()
                    }

                } else {

                    startPatientDictation()
                }

            } else {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO
                    ),
                    REQUEST_CODE_RECORD_AUDIO
                )
            }
        }

        // =====================================================
        // BACK
        // =====================================================

        uiManager.setOnBackClick {

            if (patientDictationActive) {

                patientDictationBaseText =
                    uiManager
                        .getComposeText()
                        .trim()

                patientDictationCommittedText = ""
            }

            patientDictationActive = false
            noteRecording = false

            continuousSpeechManager.stopListening()
            speechManager.stopListening()

            uiManager.showRecordingState(false)

            if (MEMBER_ID == "M1") {

                resumeComposeAfterBack =
                    uiManager
                        .getComposeText()
                        .isNotBlank() &&
                            lastComposeMode !=
                            CaregiverComposeMode.NONE
            }

            uiManager.showConversationMode()
        }

        // =====================================================
        // CLEAR COMPOSE
        // =====================================================

        uiManager.setOnClearComposeClick {

            AlertDialog.Builder(this)
                .setTitle(
                    "ERASE CURRENT TEXT?"
                )
                .setMessage(
                    "This will erase the current message or note."
                )
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .setPositiveButton(
                    "ERASE"
                ) { _, _ ->

                    patientDictationActive = false
                    noteRecording = false

                    continuousSpeechManager.stopListening()
                    speechManager.stopListening()

                    patientDictationBaseText = ""
                    patientDictationCommittedText = ""

                    noteDictationBaseText = ""
                    noteDictationCommittedText = ""

                    uiManager.clearCompose()
                    uiManager.showRecordingState(false)

                    android.util.Log.d(
                        "HB",
                        "Compose text cleared"
                    )
                }
                .show()
        }

        // =====================================================
        // SEND / SAVE NOTE
        // =====================================================

        uiManager.setOnSendClick {

            val savingNote =
                MEMBER_ID == "M1" &&
                        lastComposeMode ==
                        CaregiverComposeMode.NOTE

            if (savingNote) {

                saveSoapNoteAndReturn()

            } else {

                patientDictationActive = false

                continuousSpeechManager.stopListening()

                val text =
                    uiManager.getComposeText()

                if (text.isNotBlank()) {

                    messageManager.send(text)

                    uiManager.appendMessage(
                        "$MEMBER_ID: $text"
                    )

                    uiManager.clearCompose()

                    lastComposeMode =
                        CaregiverComposeMode.NONE

                    resumeComposeAfterBack = false

                    uiManager.showRecordingState(false)

                    speechManager.speak(
                        "Message sent"
                    )

                    android.util.Log.d(
                        "HB",
                        "Message sent: $text"
                    )
                }

                uiManager.showConversationMode()
            }
        }

        // =====================================================
        // START IN CONVERSATION MODE
        // =====================================================

        uiManager.showConversationMode()

        android.util.Log.d(
            "HB",
            "ONCREATE COMPLETE"
        )
    }


    // =====================================================
    // SAVE NOTE
    // =====================================================

    private fun saveSoapNoteAndReturn() {

        noteRecording = false

        continuousSpeechManager.stopListening()

        uiManager.showRecordingState(false)

        val note =
            uiManager.getComposeText()

        if (note.isBlank()) {

            noteRecording = false

            uiManager.showRecordingState(false)

            return
        }

        try {

            val documents =
                android.os.Environment
                    .getExternalStoragePublicDirectory(
                        android.os.Environment
                            .DIRECTORY_DOCUMENTS
                    )

            val soapFolder =
                File(
                    documents,
                    "HealthBridge/SOAP"
                )

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

            file.writeText(
                note,
                Charsets.UTF_8
            )

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

            lastComposeMode =
                CaregiverComposeMode.NONE

            resumeComposeAfterBack = false

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


    // =====================================================
    // M2 SOS — STANDARD FULL-SCREEN EMERGENCY CONFIRMATION
    // =====================================================

    private fun showFullScreenSosConfirmation() {
        if (MEMBER_ID != "M2") return

        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(dp(14), dp(12), dp(14), dp(14))
        }

        fun label(t: String, size: Float, color: Int) =
            android.widget.TextView(this).apply {
                text = t
                textSize = size
                gravity = android.view.Gravity.CENTER
                setTextColor(color)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

        val title = label("SOS / EMERGENCY", 30f, 0xFFD50000.toInt())
        val detail = label("Do you need help?", 20f, 0xFF000000.toInt())
        val choose = label("PLEASE CHOOSE:", 20f, 0xFF000000.toInt())

        val cancel = android.widget.Button(this).apply {
            text = "MISTAKE / CANCEL\nI AM SAFE"
            textSize = 27f
            gravity = android.view.Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(0xFF169B22.toInt())
            minHeight = 0
            minimumHeight = 0
        }

        val help = android.widget.Button(this).apply {
            text = "I NEED HELP\nSEND SOS NOW"
            textSize = 27f
            gravity = android.view.Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(0xFFE00000.toInt())
            minHeight = 0
            minimumHeight = 0
        }

        root.addView(title)
        root.addView(detail)
        root.addView(choose)

        root.addView(
            cancel,
            android.widget.LinearLayout.LayoutParams(-1, 0, 1f).apply {
                bottomMargin = dp(8)
            }
        )
        root.addView(
            help,
            android.widget.LinearLayout.LayoutParams(-1, 0, 1f).apply {
                topMargin = dp(8)
            }
        )

        val dialog = android.app.Dialog(
            this,
            android.R.style.Theme_Material_Light_NoActionBar_Fullscreen
        ).apply {
            setContentView(root)
            setCancelable(false)
        }

        cancel.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(
                this,
                "Alert cancelled",
                Toast.LENGTH_SHORT
            ).show()
            uiManager.showConversationMode()
        }

        help.setOnClickListener {
            dialog.dismiss()

            FirebaseManager.sendEmergencyAlert(
                memberId = "M2",
                source = FirebaseManager.AlertSource.PATIENT_BUTTON,
                details = "Patient pressed SOS — I NEED HELP"
            )

            messageManager.send("SOS — PATIENT NEEDS HELP")
            uiManager.appendMessage("$MEMBER_ID: SOS — PATIENT NEEDS HELP")

            uiManager.showConversationMode()
            uiManager.showAlertSent()
        }

        dialog.show()
    }


    // =====================================================
    // M2 FALL DETECTION — V1 PHONE ACCELEROMETER PROTOTYPE
    // =====================================================

    private fun showPossibleFall(accelerationG: Float) {
        if (MEMBER_ID != "M2" || fallDialog?.isShowing == true) return

        startFallAlarm()
        fallNoResponseHandler.removeCallbacks(fallNoResponseRunnable)
        fallNoResponseHandler.postDelayed(fallNoResponseRunnable, 60_000L)

        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(dp(14), dp(12), dp(14), dp(14))
        }

        fun label(t: String, size: Float, color: Int) =
            android.widget.TextView(this).apply {
                text = t
                textSize = size
                gravity = android.view.Gravity.CENTER
                setTextColor(color)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

        val title = label(
            "POSSIBLE FALL DETECTED",
            30f,
            0xFFD50000.toInt()
        )

        val detail = label(
            "Strong movement detected (%.1f g)".format(accelerationG),
            18f,
            0xFF000000.toInt()
        )

        val choose = label(
            "PLEASE CHOOSE:",
            20f,
            0xFF000000.toInt()
        )

        val ok = android.widget.Button(this).apply {
            text = "I'M OK / MISTAKE\nI AM SAFE"
            textSize = 27f
            gravity = android.view.Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(0xFF169B22.toInt())
            minHeight = 0
            minimumHeight = 0
        }

        val help = android.widget.Button(this).apply {
            text = "I NEED HELP\nSEND HELP NOW"
            textSize = 27f
            gravity = android.view.Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(0xFFE00000.toInt())
            minHeight = 0
            minimumHeight = 0
        }

        root.addView(title)
        root.addView(detail)
        root.addView(choose)

        // Exactly equal green/red button areas.
        root.addView(
            ok,
            android.widget.LinearLayout.LayoutParams(-1, 0, 1f).apply {
                bottomMargin = dp(8)
            }
        )
        root.addView(
            help,
            android.widget.LinearLayout.LayoutParams(-1, 0, 1f).apply {
                topMargin = dp(8)
            }
        )

        fallDialog = android.app.Dialog(
            this,
            android.R.style.Theme_Material_Light_NoActionBar_Fullscreen
        ).apply {
            setContentView(root)
            setCancelable(false)
        }

        ok.setOnClickListener {
            fallDialog?.dismiss()
            stopFallAlarmAndTimer()

            val fallOkMessage = "FALL ALERT — I'M OK / MISTAKE"
            FirebaseManager.sendMessage(
                "M2",
                "M1",
                fallOkMessage
            )
            uiManager.appendMessage("$MEMBER_ID: $fallOkMessage")

            Toast.makeText(
                this,
                "Fall cancelled",
                Toast.LENGTH_SHORT
            ).show()
        }

        help.setOnClickListener {
            fallDialog?.dismiss()
            stopFallAlarmAndTimer()

            FirebaseManager.sendEmergencyAlert(
                "M2",
                FirebaseManager.AlertSource.POSSIBLE_FALL,
                "Patient confirmed help needed after possible fall"
            )

            val fallHelpMessage = "FALL ALERT — PATIENT NEEDS HELP"
            FirebaseManager.sendMessage(
                "M2",
                "M1",
                fallHelpMessage
            )
            uiManager.appendMessage("$MEMBER_ID: $fallHelpMessage")

            uiManager.showAlertSent()
        }

        fallDialog?.show()
        speechManager.speak(
            "A possible fall was detected. Are you OK?"
        )
    }

    private fun handleFallNoResponse() {
        if (MEMBER_ID != "M2" || fallDialog?.isShowing != true) return
        stopFallAlarmOnly()
        fallDialog?.dismiss()
        fallDialog = null
        FirebaseManager.sendEmergencyAlert(
            "M2",
            FirebaseManager.AlertSource.POSSIBLE_FALL,
            "Possible fall detected — no patient response after 60 seconds"
        )
        val noResponseMessage = "FALL ALERT — NO RESPONSE"
        FirebaseManager.sendMessage("M2", "M1", noResponseMessage)
        uiManager.appendMessage("$MEMBER_ID: $noResponseMessage")
        uiManager.showAlertSent()
    }

    private fun startFallAlarm() {
        fallAlarmActive = true
        fallAlarmHandler.removeCallbacks(fallAlarmRunnable)
        fallAlarmHandler.post(fallAlarmRunnable)
    }

    private fun stopFallAlarmOnly() {
        fallAlarmActive = false
        fallAlarmHandler.removeCallbacks(fallAlarmRunnable)
        fallToneGenerator?.stopTone()
        fallToneGenerator?.release()
        fallToneGenerator = null
    }

    private fun stopFallAlarmAndTimer() {
        stopFallAlarmOnly()
        fallNoResponseHandler.removeCallbacks(fallNoResponseRunnable)
        fallDialog = null
    }

    // =====================================================
    // M2 GEOFENCE AUDIBLE ALARM
    // =====================================================

    private fun startGeofenceAlarm(distanceMeters: Double) {
        if (MEMBER_ID != "M2" || geofenceExitAcknowledged) return
        if (!geofenceAlarmActive) {
            geofenceAlarmActive = true
            geofenceAlarmHandler.removeCallbacks(geofenceAlarmRunnable)
            geofenceAlarmHandler.post(geofenceAlarmRunnable)
        }
        if (geofenceMistakeDialog?.isShowing == true) return

        val yards = (distanceMeters * 1.09361).toInt()
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(dp(14), dp(12), dp(14), dp(14))
        }
        fun label(t:String, size:Float, color:Int) = android.widget.TextView(this).apply {
            text=t; textSize=size; gravity=android.view.Gravity.CENTER; setTextColor(color)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val title=label("OUTSIDE HOME AREA",30f,0xFFD50000.toInt())
        val dist=label("$yards yards from home",18f,0xFF000000.toInt())
        val choose=label("PLEASE CHOOSE:",20f,0xFF000000.toInt())
        val ok=android.widget.Button(this).apply {
            text="I'M OK / MISTAKE\nI AM SAFE"; textSize=27f; gravity=android.view.Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt()); setTypeface(typeface,android.graphics.Typeface.BOLD)
            backgroundTintList=android.content.res.ColorStateList.valueOf(0xFF169B22.toInt())
            minHeight=0; minimumHeight=0
        }
        val help=android.widget.Button(this).apply {
            text="I NEED HELP\nSEND HELP NOW"; textSize=27f; gravity=android.view.Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt()); setTypeface(typeface,android.graphics.Typeface.BOLD)
            backgroundTintList=android.content.res.ColorStateList.valueOf(0xFFE00000.toInt())
            minHeight=0; minimumHeight=0
        }
        root.addView(title)
        root.addView(dist)
        root.addView(choose)
        root.addView(ok,android.widget.LinearLayout.LayoutParams(-1,0,1f).apply { bottomMargin=dp(8) })
        root.addView(help,android.widget.LinearLayout.LayoutParams(-1,0,1f).apply { topMargin=dp(8) })

        geofenceMistakeDialog = android.app.Dialog(this, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen).apply {
            setContentView(root); setCancelable(false)
        }
        ok.setOnClickListener {
            geofenceExitAcknowledged=true; stopGeofenceAlarm()
            messageManager.send("Geofence Mistake Pressed")
            uiManager.appendMessage("$MEMBER_ID: Geofence Mistake Pressed")
        }
        help.setOnClickListener {
            geofenceExitAcknowledged=true; stopGeofenceAlarm()
            FirebaseManager.sendEmergencyAlert("M2", FirebaseManager.AlertSource.GEOFENCE, "Patient needs help while outside home area")
            messageManager.send("GEOFENCE ALERT — PATIENT NEEDS HELP")
            uiManager.appendMessage("$MEMBER_ID: GEOFENCE ALERT — PATIENT NEEDS HELP")
        }
        geofenceMistakeDialog?.show()
    }

    private fun stopGeofenceAlarm() {
        geofenceAlarmActive = false
        geofenceAlarmHandler.removeCallbacks(geofenceAlarmRunnable)
        geofenceToneGenerator?.stopTone()
        geofenceToneGenerator?.release()
        geofenceToneGenerator = null
        geofenceMistakeDialog?.dismiss()
        geofenceMistakeDialog = null
        android.util.Log.d("HB", "M2 GEOFENCE ALARM STOPPED")
    }


    // =====================================================
    // LOW BATTERY
    // =====================================================

    private fun evaluateLowBattery() {

        if (
            MEMBER_ID != "M2" ||
            currentBatteryPercent < 0
        ) return

        val isLow =
            currentBatteryPercent <=
                    lowBatteryThreshold

        android.util.Log.d(
            "HB",
            "BATTERY CHECK: " +
                    "$currentBatteryPercent <= " +
                    "$lowBatteryThreshold = $isLow"
        )

        FirebaseManager.updateLowBatteryAlert(
            "M2",
            isLow
        )
    }

    // =====================================================
    // DESTROY
    // =====================================================

    override fun onDestroy() {
        stopFallAlarmAndTimer()
        if (::fallDetectionManager.isInitialized) fallDetectionManager.stop()
        stopGeofenceAlarm()

        if (batteryReceiverRegistered) {

            unregisterReceiver(
                batteryReceiver
            )

            batteryReceiverRegistered = false
        }

        if (
            MEMBER_ID == "M2" &&
            ::medicationManager.isInitialized
        ) {
            medicationManager.stopClockWatcher()
        }

        super.onDestroy()

        speechManager.shutdown()
        continuousSpeechManager.shutdown()
    }


    // =====================================================
    // PERMISSION RESULT
    // =====================================================

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

        if (
            requestCode ==
            REQUEST_CODE_RECORD_AUDIO
        ) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] ==
                PackageManager.PERMISSION_GRANTED
            ) {

                if (uiManager.isSoapMode()) {

                    startNoteDictation()

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


    // =====================================================
    // OLD SPEECH RECOGNITION SUPPORT
    // =====================================================

    private fun startSpeechRecognition(
        silenceMillis: Long,
        autoSaveNote: Boolean
    ) {

        speechManager.startListening(

            silenceMillis = silenceMillis,

            onResult = { text ->

                runOnUiThread {

                    if (text.isNotBlank()) {

                        uiManager.appendToCompose(
                            text
                        )
                    }

                    noteRecording = false

                    uiManager.showRecordingState(false)

                    if (
                        autoSaveNote &&
                        text.isNotBlank()
                    ) {

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
    // NOTE TO ME / SOAP — VOSK CONTINUOUS
    // =====================================================

    private fun startNoteDictation() {

        if (noteRecording) return

        noteRecording = true

        noteDictationBaseText =
            uiManager
                .getComposeText()
                .trim()

        noteDictationCommittedText = ""

        uiManager.showRecordingState(true)

        speechManager.speakThen(
            "Ready to record"
        ) {

            if (!noteRecording) {
                return@speakThen
            }

            continuousSpeechManager.startListening(

                onText = { text ->

                    runOnUiThread {

                        if (noteRecording) {

                            handleNoteVoskText(
                                text,
                                true
                            )
                        }
                    }
                },

                onPartial = { partial ->

                    runOnUiThread {

                        if (noteRecording) {

                            handleNoteVoskText(
                                partial,
                                false
                            )
                        }
                    }
                },

                onError = { msg ->

                    runOnUiThread {

                        android.util.Log.e(
                            "HB",
                            "NOTE VOSK: $msg"
                        )

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
    }


    private fun handleNoteVoskText(
        recognized: String,
        isFinalChunk: Boolean
    ) {

        if (!noteRecording) return

        val incoming =
            recognized.trim()

        if (incoming.isBlank()) return

        val liveNewSpeech =
            listOf(
                noteDictationCommittedText.trim(),
                incoming
            )
                .filter {
                    it.isNotBlank()
                }
                .joinToString(" ")
                .trim()

        val liveEditorText =
            listOf(
                noteDictationBaseText,
                liveNewSpeech
            )
                .filter {
                    it.isNotBlank()
                }
                .joinToString(" ")
                .trim()

        uiManager.clearCompose()

        uiManager.appendToCompose(
            liveEditorText
        )

        if (isFinalChunk) {

            noteDictationCommittedText =
                liveNewSpeech
        }
    }


    private fun stopNoteDictation() {

        if (!noteRecording) return

        noteRecording = false

        continuousSpeechManager.stopListening()

        noteDictationBaseText =
            uiManager
                .getComposeText()
                .trim()

        noteDictationCommittedText = ""

        uiManager.showRecordingState(false)

        android.util.Log.d(
            "HB",
            "NOTE VOSK recording stopped"
        )
    }


    // =====================================================
    // PATIENT MESSAGE DICTATION — VOSK CONTINUOUS
    // =====================================================

    private fun startPatientDictation() {

        if (patientDictationActive) return

        patientDictationActive = true

        patientDictationBaseText =
            uiManager
                .getComposeText()
                .trim()

        patientDictationCommittedText = ""

        uiManager.showRecordingState(true)

        speechManager.speakThen(
            "Ready to record"
        ) {

            if (!patientDictationActive) {
                return@speakThen
            }

            continuousSpeechManager.startListening(

                onText = { text ->

                    runOnUiThread {

                        if (patientDictationActive) {

                            handlePatientVoskText(
                                text,
                                true
                            )
                        }
                    }
                },

                onPartial = { partial ->

                    runOnUiThread {

                        if (patientDictationActive) {

                            handlePatientVoskText(
                                partial,
                                false
                            )
                        }
                    }
                },

                onError = { msg ->

                    runOnUiThread {

                        android.util.Log.e(
                            "HB",
                            msg
                        )

                        if (patientDictationActive) {

                            patientDictationActive = false

                            uiManager.showRecordingState(false)

                            Toast.makeText(
                                this,
                                msg,
                                Toast.LENGTH_SHORT
                            ).show()
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

        val incoming =
            recognized.trim()

        if (incoming.isBlank()) return

        val liveNewSpeech =
            listOf(
                patientDictationCommittedText.trim(),
                incoming
            )
                .filter {
                    it.isNotBlank()
                }
                .joinToString(" ")
                .trim()

        val commandRegex =
            Regex(
                """(?i)\b(?:send|sent)\s+(?:this\s+)?(?:to\s+)?(?:mary|marry|marie)\b[.!?,;:]*"""
            )

        val commandMatch =
            commandRegex.find(
                liveNewSpeech
            )

        if (commandMatch != null) {

            val spokenBeforeCommand =
                liveNewSpeech
                    .substring(
                        0,
                        commandMatch.range.first
                    )
                    .trim()

            val fullMessage =
                listOf(
                    patientDictationBaseText,
                    spokenBeforeCommand
                )
                    .filter {
                        it.isNotBlank()
                    }
                    .joinToString(" ")
                    .trim()

            patientDictationActive = false

            continuousSpeechManager.stopListening()

            uiManager.showRecordingState(false)

            if (fullMessage.isNotBlank()) {

                messageManager.send(
                    fullMessage
                )

                uiManager.appendMessage(
                    "$MEMBER_ID: $fullMessage"
                )

                uiManager.clearCompose()

                android.util.Log.d(
                    "HB",
                    "VOSK SEND TO MARY: $fullMessage"
                )
            }

            patientDictationCommittedText = ""
            patientDictationBaseText = ""

            uiManager.showConversationMode()

            speechManager.speak(
                "Message sent"
            )

            return
        }

        val liveEditorText =
            listOf(
                patientDictationBaseText,
                liveNewSpeech
            )
                .filter {
                    it.isNotBlank()
                }
                .joinToString(" ")
                .trim()

        uiManager.clearCompose()

        uiManager.appendToCompose(
            liveEditorText
        )

        if (isFinalChunk) {

            patientDictationCommittedText =
                liveNewSpeech
        }
    }
}