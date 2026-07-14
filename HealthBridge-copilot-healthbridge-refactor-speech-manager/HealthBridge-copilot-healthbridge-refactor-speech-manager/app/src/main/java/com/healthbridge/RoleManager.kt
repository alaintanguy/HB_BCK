package com.healthbridge

import android.content.Context
import android.util.Log
import com.healthbridge.firebase.FirebaseManager

class RoleManager(private val context: Context) {

    private val TAG = "RoleManager"
    private val prefs = context.getSharedPreferences("hb_prefs", Context.MODE_PRIVATE)

    var memberId: String = ""
        private set
    var name: String = ""
        private set
    var role: String = ""
        private set

    val partnerId: String get() = if (memberId == "M1") "M2" else "M1"

    fun loadRole(onLoaded: (memberId: String, name: String, role: String) -> Unit) {
        val saved = prefs.getString("member_id", "") ?: ""
        if (saved.isNotEmpty()) {
            memberId = saved
            name = prefs.getString("member_name", saved) ?: saved
            role = prefs.getString("member_role", "") ?: ""
            Log.d(TAG, "Role from prefs: $memberId / $name / $role")
            onLoaded(memberId, name, role)
            return
        }
        selectMember("M1", onLoaded)
    }

    fun selectMember(id: String, onLoaded: (memberId: String, name: String, role: String) -> Unit) {
        memberId = id
        FirebaseManager.profileRef(id)
            .get()
            .addOnSuccessListener { snapshot ->
                name = snapshot.child("name").getValue(String::class.java) ?: id
                role = snapshot.child("role").getValue(String::class.java) ?: ""
                prefs.edit()
                    .putString("member_id", memberId)
                    .putString("member_name", name)
                    .putString("member_role", role)
                    .apply()
                Log.d(TAG, "Role loaded: $memberId / $name / $role")
                onLoaded(memberId, name, role)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Role load failed: ${e.message}")
                name = id
                onLoaded(memberId, name, role)
            }
    }
}
