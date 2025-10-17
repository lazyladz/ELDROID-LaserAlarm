package com.example.laseralarm.presenter

import com.example.laseralarm.model.UserRepository
import com.example.laseralarm.view.RegisterView

class RegisterPresenter(
    private val view: RegisterView,
    private val userRepository: UserRepository
) {

    fun registerUser(
        fullName: String,
        phone: String,
        username: String,
        email: String,
        password: String,
        acceptedTerms: Boolean
    ) {
        // Validate all fields
        if (fullName.isEmpty() || phone.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            view.onRegisterFailure("Please fill in all fields")
            return
        }

        if (!acceptedTerms) {
            view.onRegisterFailure("You must accept the terms and conditions")
            return
        }

        // Validate phone number format (basic validation)
        if (!isValidPhone(phone)) {
            view.onRegisterFailure("Please enter a valid phone number")
            return
        }

        view.showLoading(true)

        userRepository.register(fullName, phone, username, email, password) { success, message ->
            view.showLoading(false)

            if (success) {
                view.onRegisterSuccess(message ?: "Registration successful")
            } else {
                view.onRegisterFailure(message ?: "Registration failed")
            }
        }
    }

    private fun isValidPhone(phone: String): Boolean {
        // Basic phone validation - adjust regex based on your requirements
        val phoneRegex = "^[+]?[0-9]{10,15}\$".toRegex()
        return phone.matches(phoneRegex)
    }
}