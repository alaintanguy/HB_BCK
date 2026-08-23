package com.healthbridge

import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

// =====================================================
// UI MANAGER — UI-only responsibilities
// =====================================================
class UIManager(private val activity: AppCompatActivity) {

    private val TAG = "UIManager"

    // ── Conversation Mode views ───────────────────────
    private lateinit var conversationModeContainer: View
    private lateinit var conversationContainer: LinearLayout
    private lateinit var conversationScroll: ScrollView

    private lateinit var mapView: View

    private lateinit var btnWrite: FloatingActionButton
    private lateinit var btnCopy: FloatingActionButton
    private lateinit var btnSave: FloatingActionButton

    private lateinit var patientLabel: TextView
    private lateinit var copyLabel: TextView
    private lateinit var saveLabel: TextView
    private lateinit var alertButtonGroup: View
    private lateinit var btnAlert: FloatingActionButton
    private lateinit var alertModeContainer: View
    private lateinit var btnRealAlert: Button
    private lateinit var btnAlertMistake: Button
    private lateinit var emergencyStatusOverlay: View
    private lateinit var btnEmergencyStatus: Button
    private var alertFlashAnimator: android.animation.ObjectAnimator? = null
    private val alertHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var caregiverAlertSounding = false
    private var lowBatteryVisible = false
    private var geofenceVisible = false
    private var lastGeofenceDistanceMeters = 0.0
    // M2 emergency/fall acknowledgement banner may coexist with Medication Mode.
    // Medication gets the main screen; this banner is restored after medication closes.
    private var m2AlertSentVisible = false
    private val alertTone by lazy {
        android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 70)
    }
    private val caregiverBeep = object : Runnable {
        override fun run() {
            if (!caregiverAlertSounding) return
            alertTone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 140)
            alertHandler.postDelayed(this, 1200L)
        }
    }

    // ── Compose Mode views ────────────────────────────
    private lateinit var composeModeContainer: View
    private lateinit var composeEditor: EditText

    private lateinit var composeTitle: TextView
    private lateinit var btnSpeak: FloatingActionButton
    private lateinit var btnBack: FloatingActionButton
    private lateinit var backButtonGroup: View
    private lateinit var composeButtonBar: LinearLayout
    // private lateinit var btnSend: FloatingActionButton

    private lateinit var btnSend: FloatingActionButton
    private lateinit var sendLabel: TextView
    private lateinit var btnClearCompose: Button

    private var soapMode = false
    private var configuredMemberId: String = ""

    // ── Medication Mode views — M2 only ─────────────────
    private lateinit var medicationModeContainer: View
    private lateinit var medicationTitle: TextView
    private lateinit var medicationListContainer: LinearLayout
    private lateinit var btnMedicationAllTaken: Button
    private lateinit var btnMedicationDone: Button
    private val medicationChecks = mutableListOf<android.widget.CheckBox>()
    private var medicationDoneAction: ((List<Medication>) -> Unit)? = null
    private var currentMedicationBlock: MedicationTimeBlock? = null

    // =====================================================
    // INITIALIZATION
    // =====================================================

    fun initialize() {

        conversationModeContainer =
            activity.findViewById(R.id.conversationModeContainer)

        conversationContainer =
            activity.findViewById(R.id.conversationContainer)

        conversationScroll =
            activity.findViewById(R.id.conversationScroll)

        mapView =
            activity.findViewById(R.id.map)

        btnWrite =
            activity.findViewById(R.id.btnWrite)

        btnCopy =
            activity.findViewById(R.id.btnCopy)

        btnSave =
            activity.findViewById(R.id.btnSave)

        patientLabel =
            activity.findViewById(R.id.patientLabel)

        copyLabel =
            activity.findViewById(R.id.copyLabel)

        saveLabel =
            activity.findViewById(R.id.saveLabel)

        alertButtonGroup = activity.findViewById(R.id.alertButtonGroup)
        btnAlert = activity.findViewById(R.id.btnAlert)
        alertModeContainer = activity.findViewById(R.id.alertModeContainer)
        btnRealAlert = activity.findViewById(R.id.btnRealAlert)
        btnAlertMistake = activity.findViewById(R.id.btnAlertMistake)
        emergencyStatusOverlay = activity.findViewById(R.id.emergencyStatusOverlay)
        btnEmergencyStatus = activity.findViewById(R.id.btnEmergencyStatus)

        composeModeContainer =
            activity.findViewById(R.id.composeModeContainer)

        composeEditor =
            activity.findViewById(R.id.composeEditor)
        composeTitle =
            activity.findViewById(R.id.composeTitle)
        btnSpeak =
            activity.findViewById(R.id.btnSpeak)

        btnBack =
            activity.findViewById(R.id.btnBack)
        backButtonGroup =
            activity.findViewById(R.id.backButtonGroup)
        composeButtonBar =
            activity.findViewById(R.id.composeButtonBar)

        btnSend =
            activity.findViewById(R.id.btnSend)

        sendLabel =
            activity.findViewById(R.id.sendLabel)

        btnClearCompose =
            activity.findViewById(R.id.btnClearCompose)

        medicationModeContainer =
            activity.findViewById(R.id.medicationModeContainer)
        medicationTitle =
            activity.findViewById(R.id.medicationTitle)
        medicationListContainer =
            activity.findViewById(R.id.medicationListContainer)
        btnMedicationAllTaken =
            activity.findViewById(R.id.btnMedicationAllTaken)
        btnMedicationDone =
            activity.findViewById(R.id.btnMedicationDone)

        btnMedicationAllTaken.setOnClickListener {

            medicationDoneAction?.invoke(emptyList())

            hideMedicationMode()
        }

        btnMedicationDone.setOnClickListener {

            val block = currentMedicationBlock

            if (block != null) {

                val missingMedications =
                    block.medications.filterIndexed { index, _ ->

                        index >= medicationChecks.size ||
                                !medicationChecks[index].isChecked
                    }

                medicationDoneAction?.invoke(missingMedications)
            }

            hideMedicationMode()
        }

        Log.d(TAG, "UIManager initialized")
    }

    // =====================================================
    // DEVICE / ROLE DISPLAY
    // =====================================================

    fun configureForMember(memberId: String) {

        configuredMemberId = memberId

        if (memberId == "M1") {

            // ---------------------------------------------
            // M1 = CAREGIVER
            // ---------------------------------------------

            mapView.visibility = View.VISIBLE

            btnCopy.visibility = View.VISIBLE
            btnSave.visibility = View.VISIBLE

            copyLabel.visibility = View.VISIBLE
            saveLabel.visibility = View.VISIBLE

            patientLabel.text = "Patient"
            btnWrite.contentDescription = "Patient"
            alertButtonGroup.visibility = View.GONE

            Log.d(TAG, "Configured UI for M1 caregiver")

        } else {

            // ---------------------------------------------
            // M2 = PATIENT
            // ---------------------------------------------

            mapView.visibility = View.GONE

            btnCopy.visibility = View.GONE
            btnSave.visibility = View.GONE

            copyLabel.visibility = View.GONE
            saveLabel.visibility = View.GONE

            patientLabel.text = "Write"
            btnWrite.contentDescription = "Write"
            alertButtonGroup.visibility = View.VISIBLE

            Log.d(TAG, "Configured UI for M2 patient")
        }
    }

    // =====================================================
    // MODE TRANSITIONS
    // =====================================================

    fun showConversationMode() {

        conversationModeContainer.visibility = View.VISIBLE
        composeModeContainer.visibility = View.GONE
        alertModeContainer.visibility = View.GONE
        medicationModeContainer.visibility = View.GONE

        hideKeyboard()

        Log.d(TAG, "Conversation mode active")
    }

    fun showComposeMode() {

        conversationModeContainer.visibility = View.GONE
        alertModeContainer.visibility = View.GONE
        medicationModeContainer.visibility = View.GONE
        composeModeContainer.visibility = View.VISIBLE

        // Voice-first compose: do not open the keyboard automatically.
        // The normal EditText behavior will open it only when the user taps the editor.
        composeEditor.clearFocus()
        hideKeyboard()

        Log.d(TAG, "Compose mode active")
    }

    fun showPatientComposeMode() {

        soapMode = false
        composeTitle.text =
            if (configuredMemberId == "M1") "MESSAGE TO MARY" else "MESSAGE"
        composeTitle.setTextColor(0xFFF44336.toInt())
        composeTitle.setTypeface(
            composeTitle.typeface,
            android.graphics.Typeface.BOLD
        )
        btnSend.contentDescription = "Send"
        sendLabel.text = "Send"

        // Compose controls are role-specific.
        // Hide the WHOLE Back group on M2, including its "Back" label.
        if (configuredMemberId == "M1") {
            backButtonGroup.visibility = View.VISIBLE
            // Raise M1 compose controls by about half a 56dp FAB diameter
            // so they remain fully visible above the keyboard.
            composeButtonBar.translationY =
                -28f * activity.resources.displayMetrics.density
        } else {
            // M2 has only Speak + Send.
            // Hide the complete Back group but keep the original vertical position.
            backButtonGroup.visibility = View.GONE
            composeButtonBar.translationY = 0f
        }

        // Preserve an unfinished patient message when returning from Conversation.
        composeEditor.setSelection(composeEditor.text.length)

        showComposeMode()
    }

    fun showSoapComposeMode() {

        soapMode = true
        composeTitle.text = "NOTE TO ME"
        composeTitle.setTextColor(0xFF2196F3.toInt())
        composeTitle.setTypeface(
            composeTitle.typeface,
            android.graphics.Typeface.BOLD
        )

        btnSend.contentDescription = "Save Note"
        sendLabel.text = "Save Note"
        backButtonGroup.visibility = View.VISIBLE
        composeButtonBar.translationY =
            -28f * activity.resources.displayMetrics.density

        // Preserve an unfinished note when returning from Conversation.
        composeEditor.setSelection(composeEditor.text.length)

        showComposeMode()
    }

    fun isSoapMode(): Boolean = soapMode

    fun showRecordingState(recording: Boolean) {
        if (recording) {
            composeTitle.text = "RECORDING..."
            composeTitle.setTextColor(0xFFF44336.toInt())
        } else if (soapMode) {
            composeTitle.text = "NOTE TO ME"
            composeTitle.setTextColor(0xFF2196F3.toInt())
        } else {
            composeTitle.text = "MESSAGE TO MARY"
            composeTitle.setTextColor(0xFFF44336.toInt())
        }
    }


    // =====================================================
    // CONVERSATION AREA
    // =====================================================

    fun appendMessage(
        text: String,
        timestampMillis: Long? = null
    ) {

        val timestamp = java.text.SimpleDateFormat(
            "M/d  h:mm a",
            java.util.Locale.US
        ).format(
            java.util.Date(timestampMillis ?: System.currentTimeMillis())
        )

        val senderName = when {
            text.startsWith("M1:") -> "Alain"
            text.startsWith("M2:") -> "Mary"
            else -> ""
        }

        val rawMessageText = when {
            text.startsWith("M1:") -> text.removePrefix("M1:").trim()
            text.startsWith("M2:") -> text.removePrefix("M2:").trim()
            else -> text
        }

        val geofenceExitPrefix = "SYSTEM_EVENT:GEOFENCE_EXIT:"
        val geofenceReturnPrefix = "SYSTEM_EVENT:GEOFENCE_RETURN"

        val isGeofenceExit = rawMessageText.startsWith(geofenceExitPrefix)
        val isGeofenceReturn = rawMessageText == geofenceReturnPrefix

        val messageText = when {
            isGeofenceExit -> {
                val yards = rawMessageText
                    .removePrefix(geofenceExitPrefix)
                    .toIntOrNull()

                if (yards != null) "⚠ LEFT HOME AREA — $yards yards from home"
                else "⚠ LEFT HOME AREA"
            }
            isGeofenceReturn -> "✓ RETURNED TO HOME AREA"
            else -> rawMessageText
        }

        // Header row: Name + date/time + line filling remaining width
        val headerRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(8, 10, 8, 2)
        }

        val headerText = TextView(activity).apply {
            this.text = "$senderName  $timestamp"
            textSize = 11f
            setTextColor(0xFFFFD700.toInt())
            maxLines = 1
        }

        val separator = View(activity).apply {
            setBackgroundColor(0xFFFFD700.toInt())
        }

        headerRow.addView(
            headerText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        headerRow.addView(
            separator,
            LinearLayout.LayoutParams(
                0,
                2,
                1f
            ).apply {
                marginStart = 8
            }
        )

        // Message itself. Normal messages stay white.
        // Geofence crossing events are bold and color-coded.
        val messageView = TextView(activity).apply {
            this.text = messageText
            textSize = 16f

            when {
                isGeofenceExit -> {
                    setTextColor(0xFFFF7043.toInt())
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                isGeofenceReturn -> {
                    setTextColor(0xFF66BB6A.toInt())
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                else -> setTextColor(0xFFFFFFFF.toInt())
            }

            setPadding(8, 2, 8, 10)
        }

// NEWEST MESSAGE ALWAYS AT TOP
        conversationContainer.addView(headerRow, 0)
        conversationContainer.addView(messageView, 1)

        conversationScroll.post {
            conversationScroll.fullScroll(View.FOCUS_UP)
        }
        Log.d(TAG, "Message appended: $text")
    }

    fun appendToCompose(text: String) {

        val start =
            composeEditor.selectionStart.coerceAtLeast(0)

        val end =
            composeEditor.selectionEnd.coerceAtLeast(start)

        composeEditor.text.replace(
            start,
            end,
            text
        )

        composeEditor.setSelection(
            start + text.length
        )
    }

    fun getConversationText(): String {

        return buildString {

            for (index in 0 until conversationContainer.childCount) {

                val child =
                    conversationContainer.getChildAt(index) as? TextView
                        ?: continue

                if (isNotEmpty()) {
                    append('\n')
                }

                append(child.text)
            }
        }
    }

    // =====================================================
    // COMPOSE AREA
    // =====================================================

    fun getComposeText(): String =
        composeEditor.text.toString().trim()

    fun clearCompose() {

        composeEditor.text.clear()
    }

    fun showAlertConfirmationMode() {
        conversationModeContainer.visibility = View.GONE
        composeModeContainer.visibility = View.GONE
        medicationModeContainer.visibility = View.GONE
        alertModeContainer.visibility = View.VISIBLE
        hideKeyboard()
    }

    fun showLowBatteryWarning(percent: Int) {
        lowBatteryVisible = true

        // Do not replace an active emergency alert on M1.
        if (caregiverAlertSounding) return

        alertFlashAnimator?.cancel()
        btnEmergencyStatus.alpha = 1f
        btnEmergencyStatus.scaleX = 1f
        btnEmergencyStatus.scaleY = 1f
        emergencyStatusOverlay.visibility = View.VISIBLE
        btnEmergencyStatus.isClickable = false
        btnEmergencyStatus.text = "MARY — LOW BATTERY\n$percent%"
        btnEmergencyStatus.setTextColor(0xFF000000.toInt())
        btnEmergencyStatus.backgroundTintList =
            android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())

        // One short notification beep, not continuous emergency beeping.
        alertTone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 140)
    }

    fun hideLowBatteryWarning() {
        lowBatteryVisible = false
        if (!caregiverAlertSounding && !geofenceVisible) {
            emergencyStatusOverlay.visibility = View.GONE
        }
    }

    fun showGeofenceWarning(distanceMeters: Double) {
        geofenceVisible = true
        lastGeofenceDistanceMeters = distanceMeters

        // SOS always has priority over automatic warnings.
        if (caregiverAlertSounding) return

        alertFlashAnimator?.cancel()
        btnEmergencyStatus.alpha = 1f
        btnEmergencyStatus.scaleX = 1f
        btnEmergencyStatus.scaleY = 1f
        emergencyStatusOverlay.visibility = View.VISIBLE
        btnEmergencyStatus.isClickable = true

        val yards = (distanceMeters * 1.09361).toInt()
        btnEmergencyStatus.text =
            "MARY — OUTSIDE HOME AREA\n$yards yards from home\nTAP TO CLOSE"

        // Acknowledge only the M1 display. Do not alter Firebase geofence state.
        btnEmergencyStatus.setOnClickListener {
            geofenceVisible = false
            emergencyStatusOverlay.visibility = View.GONE
            Log.d(TAG, "M1 geofence warning dismissed by caregiver")
        }
        emergencyStatusOverlay.setOnClickListener {
            geofenceVisible = false
            emergencyStatusOverlay.visibility = View.GONE
            Log.d(TAG, "M1 geofence warning dismissed by caregiver")
        }

        btnEmergencyStatus.setTextColor(0xFF000000.toInt())
        btnEmergencyStatus.backgroundTintList =
            android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())

        // One short warning beep only.
        alertTone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 140)
    }

    fun hideGeofenceWarning() {
        geofenceVisible = false

        if (!caregiverAlertSounding) {
            if (lowBatteryVisible) {
                // Battery listener will refresh the exact percentage.
                emergencyStatusOverlay.visibility = View.VISIBLE
            } else {
                emergencyStatusOverlay.visibility = View.GONE
            }
        }
    }

    fun showAlertSent() {
        m2AlertSentVisible = configuredMemberId == "M2"
        stopCaregiverAlertEffects()
        emergencyStatusOverlay.visibility = View.VISIBLE
        btnEmergencyStatus.isClickable = false
        btnEmergencyStatus.text = "ALERT SENT"
        btnEmergencyStatus.setTextColor(0xFFFFFFFF.toInt())
        btnEmergencyStatus.backgroundTintList =
            android.content.res.ColorStateList.valueOf(0xFFD32F2F.toInt())
        alertFlashAnimator?.cancel()
        alertFlashAnimator = android.animation.ObjectAnimator.ofFloat(
            btnEmergencyStatus, View.ALPHA, 1f, 0.30f
        ).apply {
            duration = 650L
            repeatMode = android.animation.ValueAnimator.REVERSE
            repeatCount = android.animation.ValueAnimator.INFINITE
            start()
        }
    }

    fun showAlertReceived() {
        m2AlertSentVisible = false
        stopCaregiverAlertEffects()
        alertFlashAnimator?.cancel()
        btnEmergencyStatus.alpha = 1f
        emergencyStatusOverlay.visibility = View.VISIBLE
        btnEmergencyStatus.isClickable = true
        btnEmergencyStatus.text = "MESSAGE RECEIVED\nTAP TO CLOSE"
        btnEmergencyStatus.setTextColor(0xFF000000.toInt())
        btnEmergencyStatus.backgroundTintList =
            android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
    }

    fun showCaregiverAlert(source: String) {
        emergencyStatusOverlay.visibility = View.VISIBLE
        btnEmergencyStatus.isClickable = true
        btnEmergencyStatus.text = "MARY — ALERT\nTAP TO ACK"
        btnEmergencyStatus.setTextColor(0xFF000000.toInt())
        btnEmergencyStatus.backgroundTintList =
            android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
        // Keep the warning surface WHITE at all times.
        // Pulse size slightly instead of fading opacity over the black screen.
        alertFlashAnimator?.cancel()
        btnEmergencyStatus.alpha = 1f
        alertFlashAnimator = android.animation.ObjectAnimator.ofFloat(
            btnEmergencyStatus, View.SCALE_X, 1f, 0.96f
        ).apply {
            duration = 450L
            repeatMode = android.animation.ValueAnimator.REVERSE
            repeatCount = android.animation.ValueAnimator.INFINITE
            start()
        }
        if (!caregiverAlertSounding) {
            caregiverAlertSounding = true
            alertHandler.post(caregiverBeep)
        }
    }

    private fun stopCaregiverAlertEffects() {
        caregiverAlertSounding = false
        alertHandler.removeCallbacks(caregiverBeep)
    }

    fun hideEmergencyStatus() {
        m2AlertSentVisible = false
        stopCaregiverAlertEffects()
        alertFlashAnimator?.cancel()
        btnEmergencyStatus.alpha = 1f
        btnEmergencyStatus.scaleX = 1f
        btnEmergencyStatus.scaleY = 1f
        btnEmergencyStatus.isClickable = true
        emergencyStatusOverlay.visibility = View.GONE

        if (lowBatteryVisible && configuredMemberId == "M1") {
            // Firebase will refresh the exact percentage on its next callback.
            emergencyStatusOverlay.visibility = View.VISIBLE
        }
    }

    fun setOnEmergencyStatusClick(action: () -> Unit) {
        btnEmergencyStatus.setOnClickListener { action() }
        emergencyStatusOverlay.setOnClickListener { action() }
    }

    // =====================================================
    // MEDICATION MODE — M2 PATIENT
    // =====================================================

    fun showMedicationBlock(
        block: MedicationTimeBlock,
        onDone: (List<Medication>) -> Unit
    ) {
        if (configuredMemberId != "M2") return
        currentMedicationBlock = block
        medicationDoneAction = onDone
        medicationChecks.clear()
        medicationListContainer.removeAllViews()

        medicationTitle.text = "MEDICATION — ${block.schedule}"

        block.medications.forEach { medication ->
            val check = android.widget.CheckBox(activity).apply {
                text = listOf(
                    medication.name,
                    medication.strength,
                    medication.dose
                ).filter { it.isNotBlank() }.joinToString("   ")
                textSize = 22f
                setTextColor(0xFFFFFFFF.toInt())
                buttonTintList =
                    android.content.res.ColorStateList.valueOf(
                        0xFFFFD700.toInt()
                    )
                setPadding(8, 16, 8, 16)
            }

            medicationChecks.add(check)
            medicationListContainer.addView(
                check,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        conversationModeContainer.visibility = View.GONE
        composeModeContainer.visibility = View.GONE
        alertModeContainer.visibility = View.GONE
        // Medication must never be blocked by an older fall/emergency banner.
        // Keep the alert state, but temporarily hide its overlay while the
        // medication controls occupy the main screen.
        emergencyStatusOverlay.visibility = View.GONE
        medicationModeContainer.visibility = View.VISIBLE
        hideKeyboard()

        Log.d(
            TAG,
            "Medication block displayed: ${block.schedule}, " +
                    "${block.medications.size} medications"
        )
    }

    private fun hideMedicationMode() {

        medicationModeContainer.visibility = View.GONE

        medicationDoneAction = null
        currentMedicationBlock = null
        medicationChecks.clear()

        showConversationMode()

        // If a fall/emergency "ALERT SENT" is still active, show it again
        // after the medication has been handled. This gives both events a
        // visible life without allowing the older alert to block medication.
        if (m2AlertSentVisible && configuredMemberId == "M2") {
            showAlertSent()
        }
    }


    // =====================================================
    // BUTTON WIRING
    // =====================================================

    fun setOnWriteClick(action: () -> Unit) {
        btnWrite.setOnClickListener { action() }
    }

    fun setOnAlertClick(action: () -> Unit) {
        btnAlert.setOnClickListener { action() }
    }

    fun setOnRealAlertClick(action: () -> Unit) {
        btnRealAlert.setOnClickListener { action() }
    }

    fun setOnAlertMistakeClick(action: () -> Unit) {
        btnAlertMistake.setOnClickListener { action() }
    }

    fun setOnCopyClick(action: () -> Unit) {

        btnCopy.setOnClickListener {
            action()
        }
    }

    fun setOnSaveClick(action: () -> Unit) {

        btnSave.setOnClickListener {
            action()
        }
    }
    fun setOnBackClick(action: () -> Unit) {

        btnBack.setOnClickListener {
            action()
        }
    }
    fun setOnSpeakClick(action: () -> Unit) {

        btnSpeak.setOnClickListener {
            action()
        }
    }

    fun setOnSendClick(action: () -> Unit) {

        btnSend.setOnClickListener {
            action()
        }
    }

    fun setOnClearComposeClick(action: () -> Unit) {

        btnClearCompose.setOnClickListener {
            action()
        }
    }

    // =====================================================
    // KEYBOARD HELPERS
    // =====================================================

    private fun hideKeyboard() {

        val imm =
            activity.getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

        val focus =
            activity.currentFocus
                ?: activity.window.decorView

        imm.hideSoftInputFromWindow(
            focus.windowToken,
            0
        )
    }

    private fun showKeyboard() {

        val imm =
            activity.getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

        imm.showSoftInput(
            composeEditor,
            InputMethodManager.SHOW_IMPLICIT
        )
    }
}