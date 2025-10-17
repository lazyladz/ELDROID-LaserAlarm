package com.example.laseralarm.presenter

import com.example.laseralarm.model.UserRepository
import com.google.firebase.auth.FirebaseAuth

class ProfilePresenter(
    private val userRepository: UserRepository
) {

    fun updateUserProfile(
        fullName: String,
        phone: String,
        username: String,
        email: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("User not authenticated")
            return
        }

        userRepository.updateUserProfile(
            uid = currentUser.uid,
            fullName = fullName,
            phone = phone,
            username = username,
            email = email
        ) { success, message ->
            if (success) {
                onSuccess(message ?: "Profile updated successfully")
            } else {
                onError(message ?: "Failed to update profile")
            }
        }
    }
}