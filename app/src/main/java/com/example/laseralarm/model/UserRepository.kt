package com.example.laseralarm.model

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(
        "https://laseralarm-bc8e3-default-rtdb.asia-southeast1.firebasedatabase.app/"
    )
) {

    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    fun register(
        fullName: String,
        phone: String,
        username: String,
        email: String,
        password: String,
        callback: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser!!.uid
                    // Create user without password for security (password is handled by Firebase Auth)
                    val user = User(
                        fullName = fullName,
                        phone = phone,
                        username = username,
                        email = email
                    )

                    database.reference.child("Users").child(uid).setValue(user)
                        .addOnCompleteListener { dbTask ->
                            if (dbTask.isSuccessful) {
                                callback(true, "Registration Successful")
                            } else {
                                callback(false, dbTask.exception?.message)
                            }
                        }
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    fun getUserData(uid: String, callback: (User?) -> Unit) {
        database.reference.child("Users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                callback(user)
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun updateUserProfile(
        uid: String,
        fullName: String,
        phone: String,
        username: String,
        email: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val updatedUser = User(
            fullName = fullName,
            phone = phone,
            username = username,
            email = email
        )

        database.reference.child("Users").child(uid).setValue(updatedUser)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, "Profile updated successfully")
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }
}