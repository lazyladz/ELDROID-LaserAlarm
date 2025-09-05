package com.example.laseralarm.view

interface LoginView {
    fun showLoading(show: Boolean)         // Show or hide a ProgressBar
    fun onLoginSuccess()                    // Called when login succeeds
    fun onLoginFailure(message: String)     // Called when login fails
    fun navigateToRegister()                // Navigate to RegistrationActivity
}
