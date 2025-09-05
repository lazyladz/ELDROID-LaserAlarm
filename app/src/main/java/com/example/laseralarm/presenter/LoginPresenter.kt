package com.example.laseralarm.presenter

import com.example.laseralarm.model.UserRepository
import com.example.laseralarm.view.LoginView

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
            if (success) view.onLoginSuccess()
            else view.onLoginFailure(errorMsg ?: "Login failed")
        }
    }
}

