package com.healthbridge

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.healthbridge.firebase.FirebaseManager
import com.healthbridge.model.VoiceMessage

class MessageManager(
    private val myId: () -> String,
    private val onNewMessage: (VoiceMessage) -> Unit
) {
    private val TAG = "MessageManager"
    private var listener: ValueEventListener? = null
    private var lastTimestamp = 0L

    fun send(text: String) {
        val from = myId()
        val to = if (from == "M1") "M2" else "M1"
        val msg = VoiceMessage(
            from = from,
            to = to,
            text = text,
            status = "new",
            timestamp = System.currentTimeMillis()
        )
        FirebaseManager.sendMessage(msg)
        Log.d(TAG, "Message sent from $from to $to: $text")
    }

    fun listen() {
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val from = snapshot.child("from").getValue(String::class.java) ?: return
                val to = snapshot.child("to").getValue(String::class.java) ?: return
                val text = snapshot.child("text").getValue(String::class.java) ?: return
                val status = snapshot.child("status").getValue(String::class.java) ?: return
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                if (to == myId() && status == "new" && timestamp > lastTimestamp) {
                    lastTimestamp = timestamp
                    onNewMessage(VoiceMessage(from, to, text, status, timestamp))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Message listener cancelled: ${error.message}")
            }
        }
        listener?.let { FirebaseManager.listenForMessages(it) }
        Log.d(TAG, "Listening for messages as ${myId()}")
    }

    fun acknowledge() {
        FirebaseManager.acknowledgeMessage()
        Log.d(TAG, "Message acknowledged (status → played)")
    }

    fun stopListening() {
        listener?.let { FirebaseManager.removeMessageListener(it) }
        listener = null
        Log.d(TAG, "Message listener stopped")
    }
}
