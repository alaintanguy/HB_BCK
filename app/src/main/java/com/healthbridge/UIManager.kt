package com.healthbridge

import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
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
    private lateinit var btnSpeak: FloatingActionButton
    private lateinit var btnSpeak: FloatingActionButton
    private lateinit var btnSend: FloatingActionButton

    fun showPatientComposeMode() {
        soapMode = false

        btnSend.text = "Send"

        showComposeMode()
    }


    // Bind all views from the current content view
    fun initialize() {
        conversationModeContainer = activity.findViewById(R.id.conversationModeContainer)
        conversationContainer     = activity.findViewById(R.id.conversationContainer)
        conversationScroll        = activity.findViewById(R.id.conversationScroll)
        btnWrite = activity.findViewById(R.id.btnWrite)
        btnCopy = activity.findViewById(R.id.btnCopy)
        btnSave = activity.findViewById(R.id.btnSave)
        composeModeContainer      = activity.findViewById(R.id.composeModeContainer)
        composeEditor             = activity.findViewById(R.id.composeEditor)
        btnSpeak = activity.findViewById(R.id.btnSpeak)
        btnSpeak = activity.findViewById(R.id.btnSpeak)
        btnSend  = activity.findViewById(R.id.btnSend)

        Log.d(TAG, "UIManager initialized")
    }
    fun setOnSpeakClick(action: () -> Unit) {
        btnSpeak.setOnClickListener { action() }
    }
    // =====================================================
    // MODE TRANSITIONS
    // =====================================================

    // Show map + conversation; hide compose; hide keyboard
    fun showConversationMode() {
        conversationModeContainer.visibility = View.VISIBLE
        composeModeContainer.visibility      = View.GONE
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

    // =====================================================
// COMPOSE MODES
// =====================================================

    private var soapMode = false



    fun showSoapComposeMode() {

        soapMode = true

        // Change title
        // composeTitle.text = "SOAP Note"

        btnSend.text = "Save SOAP"

        showComposeMode()
    }

    // =====================================================
    // CONVERSATION AREA
    // =====================================================

    // Append a message at the bottom and auto-scroll to it
    fun appendMessage(text: String) {
        val tv = TextView(activity).apply {
            this.text = text
            textSize  = 14f
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

    fun getConversationText(): String {
        return buildString {
            for (index in 0 until conversationContainer.childCount) {
                val child = conversationContainer.getChildAt(index) as? TextView ?: continue
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