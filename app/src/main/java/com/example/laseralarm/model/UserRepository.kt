package com.example.laseralarm.model

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase



class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    // Login function (already exists)
    fun login(username: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(username, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    // ✅ Register function
    fun register(username: String, email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser!!.uid
                    val user = User(username, email)
                    database.reference.child("Users").child(uid).setValue(user)
                        .addOnCompleteListener { dbTask ->
                            if (dbTask.isSuccessful) {
                                callback(true, null)
                            } else {
                                callback(false, dbTask.exception?.message)
                            }
                        }
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }
}
