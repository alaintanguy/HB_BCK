package com.healthbridge

import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
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
    private lateinit var btnWrite: FloatingActionButton
    private lateinit var btnCopy: FloatingActionButton
    private lateinit var btnSave: FloatingActionButton

    // ── Compose Mode views ────────────────────────────
    private lateinit var composeModeContainer: View
    private lateinit var composeEditor: EditText
    private lateinit var btnSpeak: FloatingActionButton
    private lateinit var btnTalk: FloatingActionButton
    private lateinit var btnSend: FloatingActionButton
    private lateinit var sendLabel: TextView

    private var soapMode = false

    // Bind all views from the current content view
    fun initialize() {
        conversationModeContainer = activity.findViewById(R.id.conversationModeContainer)
        conversationContainer = activity.findViewById(R.id.conversationContainer)
        conversationScroll = activity.findViewById(R.id.conversationScroll)

        btnWrite = activity.findViewById(R.id.btnWrite)
        btnCopy = activity.findViewById(R.id.btnCopy)
        btnSave = activity.findViewById(R.id.btnSave)

        composeModeContainer = activity.findViewById(R.id.composeModeContainer)
        composeEditor = activity.findViewById(R.id.composeEditor)
        btnSpeak = activity.findViewById(R.id.btnSpeak)
        btnTalk = activity.findViewById(R.id.btnTalk)
        btnSend = activity.findViewById(R.id.btnSend)
        sendLabel = activity.findViewById(R.id.sendLabel)

        Log.d(TAG, "UIManager initialized")
    }

    // =====================================================
    // MODE TRANSITIONS
    // =====================================================

    // Show map + conversation; hide compose; hide keyboard
    fun showConversationMode() {
        conversationModeContainer.visibility = View.VISIBLE
        composeModeContainer.visibility = View.GONE
        hideKeyboard()
        Log.d(TAG, "Conversation mode active")
    }

    // Hide map + conversation; show compose editor; request focus + keyboard
    fun showComposeMode() {
        conversationModeContainer.visibility = View.GONE
        composeModeContainer.visibility = View.VISIBLE

        composeEditor.requestFocus()
        showKeyboard()

        Log.d(TAG, "Compose mode active")
    }

    fun showPatientComposeMode() {
        soapMode = false
        // FAB has no text property; use contentDescription for semantic mode label
        btnSend.contentDescription = "Send"
        sendLabel.text = "Send"
        showComposeMode()
    }

    fun showSoapComposeMode() {
        soapMode = true
        // FAB has no text property; use contentDescription for semantic mode label
        btnSend.contentDescription = "Save SOAP"
        sendLabel.text = "Save SOAP"
        showComposeMode()
    }

    // =====================================================
    // CONVERSATION AREA
    // =====================================================

    // Append a message at the bottom and auto-scroll to it
    fun appendMessage(text: String) {
        val tv = TextView(activity).apply {
            this.text = text
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(8, 6, 8, 6)
        }
        conversationContainer.addView(tv)

        // Auto-scroll to the latest message
        conversationScroll.post {
            conversationScroll.fullScroll(View.FOCUS_DOWN)
        }

        Log.d(TAG, "Message appended: $text")
    }

//Added fun to GH


    fun appendToCompose(text: String) {
        val current = composeEditor.text.toString()

        val updated =
            if (current.isBlank())
                text
            else
                "$current $text"

        composeEditor.setText(updated)
        composeEditor.setSelection(composeEditor.text.length)
    }

    fun getConversationText(): String {
        return buildString {
            for (index in 0 until conversationContainer.childCount) {
                val child = conversationContainer.getChildAt(index) as? TextView ?: continue
                if (isNotEmpty()) append('\n')
                append(child.text)
            }
        }
    }

    // =====================================================
    // COMPOSE AREA
    // =====================================================

    fun getComposeText(): String = composeEditor.text.toString().trim()

    fun clearCompose() {
        composeEditor.text.clear()
    }

    // =====================================================
    // BUTTON WIRING
    // =====================================================

    fun setOnWriteClick(action: () -> Unit) {
        btnWrite.setOnClickListener { action() }
    }

    fun setOnCopyClick(action: () -> Unit) {
        btnCopy.setOnClickListener { action() }
    }

    fun setOnSaveClick(action: () -> Unit) {
        btnSave.setOnClickListener { action() }
    }

    fun setOnSpeakClick(action: () -> Unit) {
        btnSpeak.setOnClickListener { action() }
    }

    fun setOnTalkClick(action: () -> Unit) {
        btnTalk.setOnClickListener { action() }
    }

    fun setOnSendClick(action: () -> Unit) {
        btnSend.setOnClickListener { action() }
    }

    // =====================================================
    // KEYBOARD HELPERS
    // =====================================================

    private fun hideKeyboard() {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val focus = activity.currentFocus ?: activity.window.decorView
        imm.hideSoftInputFromWindow(focus.windowToken, 0)
    }

    private fun showKeyboard() {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(composeEditor, InputMethodManager.SHOW_IMPLICIT)
    }
}