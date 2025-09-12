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

    fun register(username: String, email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser!!.uid
                    val user = User(username, email, password) // ✅ store password too (you may want to encrypt or remove password later)

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
}
