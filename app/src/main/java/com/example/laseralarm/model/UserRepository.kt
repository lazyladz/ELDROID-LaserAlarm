package com.example.laseralarm.model

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(
        "https://laseralarm-bc8e3-default-rtdb.asia-southeast1.firebasedatabase.app/"
    )

    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                callback(task.isSuccessful, task.exception?.message)
            }
    }

    fun register(fullName: String, phone: String, username: String,
                 email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser!!.uid
                    val user = User(fullName, phone, username, email)

                    database.reference.child("Users").child(uid).setValue(user)
                        .addOnCompleteListener { dbTask ->
                            callback(dbTask.isSuccessful, dbTask.exception?.message ?: "Registration Successful")
                        }
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    fun getUserData(uid: String, callback: (User?) -> Unit) {
        database.reference.child("Users").child(uid).get()
            .addOnSuccessListener { callback(it.getValue(User::class.java)) }
            .addOnFailureListener { callback(null) }
    }

    fun updateUserProfile(uid: String, fullName: String, phone: String,
                          username: String, email: String, callback: (Boolean, String?) -> Unit) {
        val updatedUser = User(fullName, phone, username, email)

        database.reference.child("Users").child(uid).setValue(updatedUser)
            .addOnCompleteListener { task ->
                callback(task.isSuccessful, task.exception?.message ?: "Profile updated successfully")
            }
    }
}