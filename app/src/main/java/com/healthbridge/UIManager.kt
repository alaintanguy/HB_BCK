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

    // ── Compose Mode views ────────────────────────────
    private lateinit var composeModeContainer: View
    private lateinit var composeEditor: EditText

    private lateinit var composeTitle: TextView
    private lateinit var btnSpeak: FloatingActionButton
    private lateinit var btnBack: FloatingActionButton
    // private lateinit var btnSend: FloatingActionButton

    private lateinit var btnSend: FloatingActionButton
    private lateinit var sendLabel: TextView
    private lateinit var btnClearCompose: Button

    private var soapMode = false

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

        btnSend =
            activity.findViewById(R.id.btnSend)

        sendLabel =
            activity.findViewById(R.id.sendLabel)

        btnClearCompose =
            activity.findViewById(R.id.btnClearCompose)

        Log.d(TAG, "UIManager initialized")
    }

    // =====================================================
    // DEVICE / ROLE DISPLAY
    // =====================================================

    fun configureForMember(memberId: String) {

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

            Log.d(TAG, "Configured UI for M2 patient")
        }
    }

    // =====================================================
    // MODE TRANSITIONS
    // =====================================================

    fun showConversationMode() {

        conversationModeContainer.visibility = View.VISIBLE
        composeModeContainer.visibility = View.GONE

        hideKeyboard()

        Log.d(TAG, "Conversation mode active")
    }

    fun showComposeMode() {

        conversationModeContainer.visibility = View.GONE
        composeModeContainer.visibility = View.VISIBLE

        // Voice-first compose: do not open the keyboard automatically.
        // The normal EditText behavior will open it only when the user taps the editor.
        composeEditor.clearFocus()
        hideKeyboard()

        Log.d(TAG, "Compose mode active")
    }

    fun showPatientComposeMode() {

        soapMode = false
        composeTitle.text = "MESSAGE TO MARY"
        composeTitle.setTextColor(0xFFF44336.toInt())
        composeTitle.setTypeface(
            composeTitle.typeface,
            android.graphics.Typeface.BOLD
        )
        btnSend.contentDescription = "Send"
        sendLabel.text = "Send"

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

    fun appendMessage(text: String) {

        val timestamp = java.text.SimpleDateFormat(
            "M/d  h:mm a",
            java.util.Locale.US
        ).format(java.util.Date())

        val senderName = when {
            text.startsWith("M1:") -> "Alain"
            text.startsWith("M2:") -> "Mary"
            else -> ""
        }

        val messageText = when {
            text.startsWith("M1:") -> text.removePrefix("M1:").trim()
            text.startsWith("M2:") -> text.removePrefix("M2:").trim()
            else -> text
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

        // Message itself: WHITE and larger
        val messageView = TextView(activity).apply {
            this.text = messageText
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
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

    // =====================================================
    // BUTTON WIRING
    // =====================================================

    fun setOnWriteClick(action: () -> Unit) {

        btnWrite.setOnClickListener {
            action()
        }
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