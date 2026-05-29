package com.healthbridge.firebase

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseManager {

    private val database: FirebaseDatabase =
        FirebaseDatabase.getInstance()

    fun memberReference(
        memberId: String
    ): DatabaseReference {

        return database.reference
            .child("families")
            .child("family_001")
            .child("members")
            .child(memberId)
    }
}