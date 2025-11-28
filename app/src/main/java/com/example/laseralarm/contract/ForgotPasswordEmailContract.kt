package com.example.laseralarm.contract

interface ForgotPasswordEmailContract {
    interface View {
        fun showLoading(show: Boolean)
        fun showError(message: String)
        fun showSuccess(message: String)
        fun navigateToLogin() // Changed to navigateToLogin
    }

    interface Presenter {
        fun sendPasswordResetEmail(email: String) // Changed to sendPasswordResetEmail
        fun onDestroy()
    }
}