package com.healthbridge

import android.util.Log
import com.healthbridge.firebase.FirebaseManager

class MessageManager(
    private val memberId: String
) {

    fun send(text: String) {

        if (text.isBlank()) return

        val target =
            if (memberId == "M1")
                "M2"
            else
                "M1"

        FirebaseManager.sendMessage(
            memberId,
            target,
            text
        )

        Log.d(
            "HB",
            "MESSAGE REQUESTED"
        )
    }

    fun startListening(
        onMessage: (String, String) -> Unit
    ) {

        FirebaseManager.listenForMessages(
            memberId
        ) { message ->

            onMessage(
                message.from,
                message.text
            )

            FirebaseManager.markMessagePlayed()
        }
    }
}