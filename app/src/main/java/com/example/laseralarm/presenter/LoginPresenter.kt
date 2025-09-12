package com.example.laseralarm.presenter

import com.example.laseralarm.model.User
import com.example.laseralarm.model.UserRepository
import com.example.laseralarm.view.LoginView
import com.google.firebase.auth.FirebaseAuth

class LoginPresenter(
    private val view: LoginView,
    private val userRepository: UserRepository
) {
    fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            view.onLoginFailure("Please fill in all fields")
            return
        }

        view.showLoading(true)

        userRepository.login(email, password) { success, errorMsg ->
            view.showLoading(false)
            if (success) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    userRepository.getUserData(uid) { user ->
                        if (user != null) {
                            view.onLoginSuccess(user) // ✅ pass user profile
                        } else {
                            view.onLoginFailure("Failed to fetch user data")
                        }
                    }
                } else {
                    view.onLoginFailure("User ID not found")
                }
            } else {
                view.onLoginFailure(errorMsg ?: "Login failed")
            }
        }
    }
}
