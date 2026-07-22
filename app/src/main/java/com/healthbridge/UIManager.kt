// ====================================================================
// HealthBridge
// UIManager.kt
// Phase 5A – UI responsibilities extracted from MainActivity
// Phase 5D – Two-mode UI: Conversation Mode / Compose Mode
// ====================================================================
//
// Conversation Mode (initial state):
//   Visible:  map, statusText, conversationScrollView, writeButton
//   Hidden:   composeEdit, sendButton, speakButton, keyboard
//
// Compose Mode (entered via writeButton):
//   Visible:  composeEdit (6 lines), sendButton, keyboard
//   Hidden:   map, conversationScrollView, writeButton
//   After send: clear compose editor → return to Conversation Mode
//
// Bug fixes applied in this revision:
//   FIX-1: showMessageInput() previously overwrote the conversation
//           history when the compose editor was not visible.
//           It now ALWAYS writes to composeEdit only.
//   FIX-2: enterComposeMode() / exitComposeMode() now correctly
//           hide/show the map and the conversation scroll view so
//           the two views never overlap each other.
//   FIX-3: appendConversation() auto-scrolls to the bottom so the
//           most recent message is always visible.
// ====================================================================

package com.healthbridge

import android.app.Activity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.util.Log

// =====================================================
// UI MANAGER
// Owns: view binding, status/message/ACK display,
//       button initialisation, UI mode switching.
// Does NOT own: Firebase, speech, map, telemetry.
// =====================================================

class UIManager(private val activity: Activity) {

    // =====================================================
    // VIEWS
    // =====================================================

    private lateinit var statusText: TextView

    // Conversation Mode views
    private lateinit var mapView: View               // fragment container
    private lateinit var conversationScrollView: ScrollView
    private lateinit var messageView: TextView

    // Compose Mode views
    private lateinit var composeEdit: EditText

    // Buttons
    private lateinit var writeButton: Button
    private lateinit var sendButton: Button
    private lateinit var speakButton: Button

    // =====================================================
    // INITIALISATION
    // =====================================================

    fun initialize(
        onSpeak: () -> Unit,
        onWrite: () -> Unit,
        onSend: () -> Unit,
        onAck: () -> Unit
    ) {
        activity.setContentView(R.layout.activity_main)

        // Keep keyboard hidden until Compose Mode is entered explicitly.
        activity.window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )

        statusText = activity.findViewById(R.id.statusText)
        statusText.text = "No pending messages"

        // Conversation Mode views
        mapView = activity.findViewById(R.id.map)
        conversationScrollView = activity.findViewById(R.id.conversationScrollView)
        messageView = activity.findViewById(R.id.messageView)

        // Compose Mode view – hidden initially (Conversation Mode is default)
        composeEdit = activity.findViewById(R.id.composeEdit)
        composeEdit.visibility = View.GONE

        // Buttons
        writeButton = activity.findViewById(R.id.writeButton)
        sendButton = activity.findViewById(R.id.sendButton)
        speakButton = activity.findViewById(R.id.speakButton)

        // speakButton is wired but stays hidden; Gboard mic is preferred
        speakButton.setOnClickListener {
            statusText.text = "STARTING SPEECH"
            onSpeak()
        }

        writeButton.setOnClickListener { onWrite() }
        sendButton.setOnClickListener { onSend() }

        // Tapping status bar acts as ACK
        statusText.bringToFront()
        statusText.setOnClickListener { onAck() }

        Log.d("HB", "UIManager initialised – Conversation Mode")
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

    /**
     * FIX-1: Write speech/draft text to the compose editor ONLY.
     * Previously this method could fall through to messageView.text = text,
     * overwriting the entire conversation history.
     */
    fun showMessageInput(text: String) {
        composeEdit.setText(text)
        composeEdit.setSelection(composeEdit.text.length)
    }

    /**
     * Append a new entry to the read-only conversation log and
     * scroll to the bottom so it is immediately visible.
     * FIX-3: auto-scroll added; isAttachedToWindow guard prevents
     * posting a runnable on a detached view.
     */
    fun appendConversation(text: String) {
        if (messageView.text.isBlank()) {
            messageView.text = text
        } else {
            messageView.append("\n\n")
            messageView.append(text)
        }
        // Scroll to the bottom after the layout pass completes.
        // Both isAttachedToWindow checks guard against the view being
        // detached between the outer check and when the runnable fires.
        if (conversationScrollView.isAttachedToWindow) {
            conversationScrollView.post {
                if (conversationScrollView.isAttachedToWindow) {
                    conversationScrollView.fullScroll(View.FOCUS_DOWN)
                }
            }
        }
    }

    fun getMessageText(): String =
        composeEdit.text.toString().trim()

    fun clearMessageInput() {
        composeEdit.setText("")
    }

    // =====================================================
    // COMPOSE MODE
    // FIX-2: map and conversation scroll view are now
    //        explicitly hidden when entering Compose Mode
    //        and restored when exiting.
    // =====================================================

    fun enterComposeMode() {
        // Hide Conversation Mode views
        mapView.visibility = View.GONE
        conversationScrollView.visibility = View.GONE
        writeButton.visibility = View.GONE

        // Show Compose Mode views
        composeEdit.visibility = View.VISIBLE
        sendButton.visibility = View.VISIBLE

        composeEdit.requestFocus()

        val imm =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager
        imm.showSoftInput(composeEdit, InputMethodManager.SHOW_IMPLICIT)

        Log.d("HB", "UIManager – Compose Mode entered")
    }

    fun exitComposeMode() {
        // Clear compose editor and dismiss keyboard
        composeEdit.setText("")
        composeEdit.clearFocus()

        val imm =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager
        imm.hideSoftInputFromWindow(composeEdit.windowToken, 0)

        // Hide Compose Mode views
        composeEdit.visibility = View.GONE
        sendButton.visibility = View.GONE

        // Restore Conversation Mode views
        mapView.visibility = View.VISIBLE
        conversationScrollView.visibility = View.VISIBLE
        writeButton.visibility = View.VISIBLE

        Log.d("HB", "UIManager – Conversation Mode restored")
    }

    // =====================================================
    // ACK DISPLAY
    // =====================================================

    fun clearAck() {
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
            statusText.text = currentMessage + "\n\n[LESS]"
        } else {
            statusText.text =
                currentMessage.take(80) + "..." + "\n\n[MORE]"
        }
    }

}

