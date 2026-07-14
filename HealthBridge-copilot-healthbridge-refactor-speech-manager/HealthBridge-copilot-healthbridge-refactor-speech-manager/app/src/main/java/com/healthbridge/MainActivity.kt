package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.healthbridge.firebase.FirebaseManager
import com.healthbridge.model.VoiceMessage
import com.healthbridge.network.NetworkMonitor
import com.healthbridge.telemetry.TelemetryEngine

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    // =====================================================
    // VIEWS
    // =====================================================
    private lateinit var spinnerRole: Spinner
    private lateinit var textMe: TextView
    private lateinit var spinnerUser: Spinner
    private lateinit var textMember: TextView
    private lateinit var textHR: TextView
    private lateinit var textTime: TextView
    private lateinit var textIncoming: TextView
    private lateinit var btnRecord: Button
    private lateinit var editMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var textNetworkStatus: TextView

    // =====================================================
    // MANAGERS
    // =====================================================
    private lateinit var speechManager: SpeechManager
    private lateinit var messageManager: MessageManager
    private lateinit var roleManager: RoleManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var telemetryEngine: TelemetryEngine

    private var speechRecognizer: SpeechRecognizer? = null
    private var memberListener: ValueEventListener? = null
    private var currentViewedMember: String = "M1"

    // =====================================================
    // INITIALIZATION
    // =====================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate")

        initViews()
        initManagers()
        requestPermissionsIfNeeded()
    }

    private fun initViews() {
        spinnerRole = findViewById(R.id.spinnerRole)
        textMe = findViewById(R.id.textMe)
        spinnerUser = findViewById(R.id.spinnerUser)
        textMember = findViewById(R.id.textMember)
        textHR = findViewById(R.id.textHR)
        textTime = findViewById(R.id.textTime)
        textIncoming = findViewById(R.id.textIncoming)
        btnRecord = findViewById(R.id.btnRecord)
        editMessage = findViewById(R.id.editMessage)
        btnSend = findViewById(R.id.btnSend)
        textNetworkStatus = findViewById(R.id.textNetworkStatus)

        btnSend.setOnClickListener { onSendClick() }
        setupRecordButton()
    }

    private fun initManagers() {
        speechManager = SpeechManager(this)
        speechManager.initialize()

        roleManager = RoleManager(this)
        messageManager = MessageManager(
            myId = { roleManager.memberId },
            onNewMessage = { msg -> onMessageReceived(msg) }
        )
        networkMonitor = NetworkMonitor(this)
        telemetryEngine = TelemetryEngine(this)
        permissionManager = PermissionManager(this)
    }

    private fun requestPermissionsIfNeeded() {
        if (permissionManager.allGranted()) {
            onPermissionsGranted()
        } else {
            permissionManager.request()
        }
    }

    // =====================================================
    // ROLE MANAGEMENT
    // =====================================================

    private fun onPermissionsGranted() {
        roleManager.loadRole { memberId, name, role ->
            runOnUiThread {
                textMe.text = "$name ($role)"
                setupRoleSpinner(memberId)
                setupMemberSpinner()
                startManagers(memberId)
            }
        }
    }

    private fun setupRoleSpinner(initialMemberId: String) {
        val members = arrayOf("M1", "M2")
        val adapter = ArrayAdapter(this, R.layout.spinner_item, members)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter
        // Set initial selection without triggering the listener
        val idx = if (initialMemberId == "M1") 0 else 1
        spinnerRole.setSelection(idx)
        // Attach listener after initial selection fires
        spinnerRole.post {
            spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                    val selected = members[pos]
                    if (selected == roleManager.memberId) return
                    roleManager.selectMember(selected) { newMemberId, newName, newRole ->
                        runOnUiThread {
                            textMe.text = "$newName ($newRole)"
                            messageManager.stopListening()
                            messageManager.listen()
                            telemetryEngine.stop()
                            telemetryEngine.start(newMemberId)
                            Log.d(TAG, "Role switched to $newMemberId")
                        }
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
    }

    private fun setupMemberSpinner() {
        val members = arrayOf("M1", "M2")
        val adapter = ArrayAdapter(this, R.layout.spinner_item, members)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUser.adapter = adapter
        spinnerUser.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                listenToMember(members[pos])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // =====================================================
    // MESSAGING
    // =====================================================

    private fun onSendClick() {
        val text = editMessage.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show()
            return
        }
        messageManager.send(text)
        editMessage.setText("")
        Log.d(TAG, "Text message sent: $text")
    }

    private fun onMessageReceived(msg: VoiceMessage) {
        runOnUiThread {
            textIncoming.text = "[${msg.from}] ${msg.text}"
        }
        speechManager.speak(msg.text)
        Handler(Looper.getMainLooper()).postDelayed({
            messageManager.acknowledge()
        }, 1500)
    }

    private fun setupRecordButton() {
        btnRecord.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> startVoiceRecognition()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> stopVoiceRecognition()
            }
            true
        }
    }

    private fun startVoiceRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                runOnUiThread { btnRecord.text = "🔴 Listening…" }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: return
                runOnUiThread {
                    editMessage.setText(text)
                    btnRecord.text = "🎤 Hold to Speak"
                }
                messageManager.send(text)
                Log.d(TAG, "Voice message recognized and sent: $text")
            }
            override fun onEndOfSpeech() {
                runOnUiThread { btnRecord.text = "🎤 Hold to Speak" }
            }
            override fun onError(error: Int) {
                runOnUiThread { btnRecord.text = "🎤 Hold to Speak" }
                Log.e(TAG, "Speech recognition error: $error")
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun stopVoiceRecognition() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        btnRecord.text = "🎤 Hold to Speak"
    }

    // =====================================================
    // TELEMETRY
    // =====================================================

    private fun listenToMember(memberId: String) {
        memberListener?.let {
            FirebaseManager.memberRef(currentViewedMember).removeEventListener(it)
        }
        currentViewedMember = memberId

        memberListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hr = snapshot.child("telemetry").child("watch").child("heartRate")
                    .getValue(Int::class.java) ?: 0
                val time = snapshot.child("telemetry").child("readable").child("time")
                    .getValue(String::class.java) ?: "--"
                val memberName = snapshot.child("profile").child("name")
                    .getValue(String::class.java) ?: memberId
                val memberRole = snapshot.child("profile").child("role")
                    .getValue(String::class.java) ?: ""
                runOnUiThread {
                    textMember.text = "$memberName ($memberRole)"
                    textHR.text = "$hr bpm"
                    textTime.text = time
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Member listener cancelled: ${error.message}")
            }
        }
        FirebaseManager.memberRef(memberId).addValueEventListener(memberListener!!)
        Log.d(TAG, "Listening to member: $memberId")
    }

    // =====================================================
    // NETWORK
    // =====================================================

    private fun startManagers(memberId: String) {
        networkMonitor.start(
            onConnected = { runOnUiThread { textNetworkStatus.text = "🟢 Online" } },
            onDisconnected = { runOnUiThread { textNetworkStatus.text = "🔴 Offline" } }
        )
        runOnUiThread {
            textNetworkStatus.text = if (networkMonitor.isConnected) "🟢 Online" else "🔴 Offline"
        }
        messageManager.listen()
        telemetryEngine.start(memberId)
        Log.d(TAG, "All managers started for $memberId")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.onResult(
            requestCode, grantResults,
            onGranted = { onPermissionsGranted() },
            onDenied = {
                Toast.makeText(this, "Some permissions denied – limited functionality.", Toast.LENGTH_LONG).show()
                onPermissionsGranted()
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        speechManager.shutdown()
        messageManager.stopListening()
        networkMonitor.stop()
        telemetryEngine.stop()
        speechRecognizer?.destroy()
        memberListener?.let {
            FirebaseManager.memberRef(currentViewedMember).removeEventListener(it)
        }
        Log.d(TAG, "MainActivity destroyed, all managers stopped")
    }
}
