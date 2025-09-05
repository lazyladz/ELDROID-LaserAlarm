package com.example.laseralarm.presenter

import com.example.laseralarm.model.UserRepository
import com.example.laseralarm.view.RegisterView

class RegisterPresenter(
    private val view: RegisterView,
    private val repository: UserRepository
) {

    fun registerUser(username: String, email: String, password: String, acceptedTerms: Boolean) {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            view.onRegisterFailure("Please fill all fields")
            return
        }
        if (!acceptedTerms) {
            view.onRegisterFailure("You must accept Terms and Conditions")
            return
        }

        view.showLoading(true)
        repository.register(username, email, password) { success, message ->
            view.showLoading(false)
            if (success) {
                view.onRegisterSuccess(message ?: "Registration Successful")
            } else {
                view.onRegisterFailure(message ?: "Registration Failed")
            }
        }
    }
}
