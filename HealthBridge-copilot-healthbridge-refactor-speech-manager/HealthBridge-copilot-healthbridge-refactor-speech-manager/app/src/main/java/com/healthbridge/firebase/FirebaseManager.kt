package com.healthbridge.firebase

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.healthbridge.model.VoiceMessage

object FirebaseManager {

    const val GROUP_ID = "family_001"

    private val root: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().reference
    }

    private fun groupRef() = root.child("groups").child(GROUP_ID)

    fun memberRef(memberId: String): DatabaseReference =
        groupRef().child("members").child(memberId)

    fun profileRef(memberId: String): DatabaseReference =
        memberRef(memberId).child("profile")

    fun telemetryRef(memberId: String): DatabaseReference =
        memberRef(memberId).child("telemetry")

    fun deviceRef(memberId: String): DatabaseReference =
        memberRef(memberId).child("device")

    private fun messageRef(): DatabaseReference =
        groupRef().child("messages").child("current")

    fun sendMessage(msg: VoiceMessage) {
        messageRef().setValue(msg)
    }

    fun acknowledgeMessage() {
        messageRef().child("status").setValue("played")
    }

    fun listenForMessages(listener: ValueEventListener) {
        messageRef().addValueEventListener(listener)
    }

    fun removeMessageListener(listener: ValueEventListener) {
        messageRef().removeEventListener(listener)
    }

    fun updateDevice(memberId: String, updates: Map<String, Any>) {
        deviceRef(memberId).updateChildren(updates)
    }

    fun updateTelemetry(memberId: String, updates: Map<String, Any>) {
        telemetryRef(memberId).updateChildren(updates)
    }
}
