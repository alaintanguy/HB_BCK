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
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.util.Log

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


    private lateinit var messageView: TextView
    private lateinit var composeEdit: EditText




    private lateinit var statusText: TextView
   // private lateinit var messageView: TextView
  //  private lateinit var ackStatus: TextView
  //  private lateinit var ackButton: Button
    private lateinit var sendButton: Button
    private lateinit var speakButton: Button


    private lateinit var writeButton: Button

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

        activity.window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )

        statusText = activity.findViewById(R.id.statusText)
        statusText.text = "No pending messages"

        val mv = activity.findViewById<android.view.View>(R.id.messageView)
        Log.d("HB", "messageView class = ${mv.javaClass.name}")

        val ce = activity.findViewById<android.view.View>(R.id.composeEdit)
        Log.d("HB", "composeEdit class = ${ce.javaClass.name}")

        messageView = mv as TextView
        composeEdit = ce as EditText
        composeEdit.visibility = android.view.View.GONE
      //  messageEdit.clearFocus()
        // Start in READ mode
     //   messageEdit.isFocusable = false
     //   messageEdit.isFocusableInTouchMode = false
     //   messageEdit.isCursorVisible = false

    //    ackStatus = activity.findViewById(R.id.ackStatus)

        writeButton = activity.findViewById(R.id.writeButton)
        sendButton = activity.findViewById(R.id.sendButton)
        speakButton = activity.findViewById(R.id.speakButton)

        speakButton.setOnClickListener {
            statusText.text = "STARTING SPEECH"
            onSpeak()
        }

        writeButton.setOnClickListener {
            onWrite()
        }

        sendButton.setOnClickListener { onSend() }

       // ackButton.setOnClickListener { onAck() }

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

        if (composeEdit.visibility == android.view.View.VISIBLE) {
            composeEdit.setText(text)
            composeEdit.setSelection(composeEdit.text.length)
        } else {
            messageView.text = text
        }
    }

    fun appendConversation(text: String) {

        if (messageView.text.isBlank()) {
            messageView.text = text
        } else {
            messageView.append("\n\n")
            messageView.append(text)
        }
    }

    fun getMessageText(): String =
        composeEdit.text.toString().trim()

    fun clearMessageInput() {
        composeEdit.setText("")
    }

    // =====================================================
// COMPOSE MODE
// =====================================================

    fun enterComposeMode() {

        messageView.visibility = android.view.View.GONE
        composeEdit.visibility = android.view.View.VISIBLE

        composeEdit.requestFocus()

        val imm =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager

        imm.showSoftInput(
            composeEdit,
            InputMethodManager.SHOW_IMPLICIT
        )
    }
    fun exitComposeMode() {

        composeEdit.setText("")
        composeEdit.clearFocus()

        val imm =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager

        imm.hideSoftInputFromWindow(
            composeEdit.windowToken,
            0
        )

        composeEdit.visibility = android.view.View.GONE
        messageView.visibility = android.view.View.VISIBLE
    }

    // =====================================================
    // ACK DISPLAY
    // =====================================================

    fun clearAck() {
 //       messageEdit.setText("")
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

