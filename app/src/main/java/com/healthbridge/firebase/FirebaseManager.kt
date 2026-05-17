package com.healthbridge.firebase

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseManager {

    private val database = FirebaseDatabase.getInstance(
        "https://healthbridge-e2aac-default-rtdb.firebaseio.com/"
    )

    fun memberReference(): DatabaseReference {

        return database.getReference(
            "groups/family_001/members/alain"
        )
    }
}