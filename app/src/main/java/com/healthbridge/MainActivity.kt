package com.healthbridge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.healthbridge.telemetry.BatteryCollector




// =====================================================
// MAIN ACTIVITY — coordinator only
// =====================================================
class MainActivity : AppCompatActivity() {

    companion object {
        private const val MEMBER_ID = "M2"   // Change to "M2" for the Samsung
    }


    private lateinit var messageManager: MessageManager
    private lateinit var mapManager: MapManager
    private lateinit var uiManager: UIManager
    private lateinit var speechManager: SpeechManager






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        android.util.Log.d("HB", "ONCREATE START")

        // =====================================================
        // INITIALIZATION
        // =====================================================
        mapManager = MapManager(this)
        mapManager.initialize()

        uiManager = UIManager(this)
        uiManager.initialize()
        speechManager = SpeechManager(this)
        speechManager.initialize()
        speechManager = SpeechManager(this)
        speechManager.initialize()

        val battery = BatteryCollector(this).getBatteryLevel()

        val currentTime =
            SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date())
        android.util.Log.d("HB", "BATTERY = $battery")

       // uiManager.showStatus("🟢 ONLINE    🕒 $currentTime    🔋 $battery%")
        uiManager.showStatus("XXXXXXXXXXXXXXXXXXXXXXXX")


        messageManager = MessageManager(MEMBER_ID)


        messageManager.startListening { from, text ->

            runOnUiThread {

                uiManager.appendMessage("$from: $text")
                speechManager.speak(text)

            }
        }


        uiManager.showStatus("🟢 ONLINE    🕒 $currentTime    🔋 --%")

        android.util.Log.d("HB", "MANAGERS INITIALIZED")

        // =====================================================
        // BUTTON WIRING
        // =====================================================

        // Write → Compose Mode
        uiManager.setOnWriteClick {
            uiManager.showComposeMode()
        }

        // Microphone → speech recognition hook (preserved for SpeechManager integration)
        uiManager.setOnMicClick {
            android.util.Log.d("HB", "Mic button pressed — speech recognition hook")
        }

        // Send → process message, clear editor, return to Conversation Mode
        uiManager.setOnSendClick {

            val text = uiManager.getComposeText()

            if (text.isNotBlank()) {

                // Send to Firebase
                messageManager.send(text)

                // Show immediately in local conversation
                uiManager.appendMessage("Me: $text")

                uiManager.clearCompose()
            }

            uiManager.showConversationMode()
        }

        // =====================================================
        // START IN CONVERSATION MODE
        // =====================================================
        uiManager.showConversationMode()

        android.util.Log.d("HB", "ONCREATE COMPLETE")
    }
    override fun onDestroy() {
        super.onDestroy()
        speechManager.shutdown()
    }
}