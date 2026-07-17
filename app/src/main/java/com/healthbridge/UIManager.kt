// ====================================================================
// HealthBridge
// UIManager.kt
// Phase 5A – UI responsibilities extracted from MainActivity
// ====================================================================

package com.healthbridge

import android.app.Activity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

// =====================================================
// UI MANAGER
// Owns: view binding, status/message/ACK display,
//       button initialisation, UI helper methods.
// Does NOT own: Firebase, speech, map, telemetry.
// =====================================================

class UIManager(private val activity: Activity) {

    // =====================================================
    // VIEWS
    // =====================================================

    private lateinit var statusText: TextView
    private lateinit var messageEdit: EditText
    private lateinit var ackStatus: TextView
    private lateinit var ackButton: Button
    private lateinit var sendButton: Button
    private lateinit var speakButton: Button

    // =====================================================
    // INITIALISATION
    // =====================================================

    fun initialize(
        onSpeak: () -> Unit,
        onSend: () -> Unit,
        onAck: () -> Unit
    ) {
        activity.setContentView(R.layout.activity_main)

        activity.window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )

        statusText = activity.findViewById(R.id.statusText)
        statusText.text = "No pending messages"

        messageEdit = activity.findViewById(R.id.messageEdit)
        messageEdit.clearFocus()

        ackStatus = activity.findViewById(R.id.ackStatus)

        ackButton = activity.findViewById(R.id.ackButton)
        sendButton = activity.findViewById(R.id.sendButton)
        speakButton = activity.findViewById(R.id.speakButton)

        speakButton.setOnClickListener {
            statusText.text = "STARTING SPEECH"
            onSpeak()
        }

        sendButton.setOnClickListener { onSend() }

        ackButton.setOnClickListener { onAck() }

        statusText.bringToFront()
        statusText.setOnClickListener { onAck() }
    }

    // =====================================================
    // STATUS DISPLAY
    // =====================================================

    fun showStatus(text: String) {
        statusText.text = text
    }

    // =====================================================
    // MESSAGE DISPLAY
    // =====================================================

    fun showMessageInput(text: String) {
        messageEdit.setText(text)
    }

    fun getMessageText(): String =
        messageEdit.text.toString().trim()

    fun clearMessageInput() {
        messageEdit.setText("")
    }

    // =====================================================
    // ACK DISPLAY
    // =====================================================

    fun clearAck() {
        messageEdit.setText("")
        statusText.text = "No pending messages"
    }

    // =====================================================
    // UI HELPER – long-message display
    // =====================================================

    fun displayMessage(
        currentMessage: String,
        fullMessageVisible: Boolean
    ) {
        if (currentMessage.length <= 80) {
            statusText.text = currentMessage
            return
        }

        if (fullMessageVisible) {
            statusText.text =
                currentMessage + "\n\n[LESS]"
        } else {
            statusText.text =
                currentMessage.take(80) +
                        "..." +
                        "\n\n[MORE]"
        }
    }
}
