package com.example.laseralarm.presenter

import com.example.laseralarm.model.UserRepository
import com.example.laseralarm.view.RegisterView

class RegisterPresenter(
    private val view: RegisterView,
    private val userRepository: UserRepository
) {

    fun registerUser(username: String, email: String, password: String, acceptedTerms: Boolean) {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            view.onRegisterFailure("Please fill in all fields")
            return
        }

        if (!acceptedTerms) {
            view.onRegisterFailure("You must accept the terms and conditions")
            return
        }

        view.showLoading(true)

        userRepository.register(username, email, password) { success, message ->
            view.showLoading(false) // ✅ Always hide loader

            if (success) {
                view.onRegisterSuccess(message ?: "Registration successful")
            } else {
                view.onRegisterFailure(message ?: "Registration failed")
            }
        }
    }
}
