package com.example.laseralarm.view

import com.example.laseralarm.model.User

interface LoginView {
    fun showLoading(show: Boolean)
    fun onLoginSuccess(user: User)   // ✅ updated
    fun onLoginFailure(message: String)
    fun navigateToRegister()
}
