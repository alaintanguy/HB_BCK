// ====================================================================
// HealthBridge
// RoleManager.kt
// Version: 1.0
// ====================================================================
//
// Phase 5C: Role management responsibilities extracted from MainActivity.
// Loads member role from Firebase and resolves publisher/viewer state.
// ====================================================================

package com.healthbridge

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.healthbridge.firebase.FirebaseManager

// =====================================================
// ROLE MANAGER
// =====================================================

class RoleManager(private val memberId: String) {

    // =====================================================
    // ROLE LOADING
    // =====================================================

    private var roleListener: ValueEventListener? = null

    /**
     * Reads the member role from Firebase at:
     *   groups/family_001/members/<memberId>/profile/role
     *
     * Calls [onRoleResolved] with isPublisher=true when role=="patient",
     * false otherwise (caregiver). The listener is persistent so that
     * any server-side role change is reflected immediately.
     *
     * Call [cleanup] when the owning component is destroyed to remove
     * the listener and avoid memory leaks.
     */
    fun loadRole(onRoleResolved: (isPublisher: Boolean) -> Unit) {

        val listener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                Log.d(
                    "HB",
                    "ROLE VALUE = ${snapshot.value}"
                )

                val role =
                    snapshot.getValue(String::class.java)
                        ?: "caregiver"

                Log.d(
                    "HB",
                    "ROLE = $role"
                )

                val isPublisher = (role == "patient")

                onRoleResolved(isPublisher)
            }

            override fun onCancelled(error: DatabaseError) {

                Log.e(
                    "HB",
                    "ROLE LISTENER FAILED",
                    error.toException()
                )

                // Default to caregiver (viewer) on failure so the
                // app reaches a usable state.
                onRoleResolved(false)
            }
        }

        roleListener = listener

        FirebaseManager
            .memberReference(memberId)
            .child("profile")
            .child("role")
            .addValueEventListener(listener)
    }

    /**
     * Removes the active role listener. Call this from the owning
     * Activity's onDestroy() to prevent memory leaks.
     */
    fun cleanup() {

        val listener = roleListener ?: return

        FirebaseManager
            .memberReference(memberId)
            .child("profile")
            .child("role")
            .removeEventListener(listener)

        roleListener = null
    }
}
