package com.example.laseralarm.presenter

import com.example.laseralarm.contract.ForgotPasswordEmailContract
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ForgotPasswordEmailPresenter(
    private val view: ForgotPasswordEmailContract.View
) : ForgotPasswordEmailContract.Presenter {

    private val auth: FirebaseAuth = Firebase.auth

    override fun sendPasswordResetEmail(email: String) { // Correct method name
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            view.showError("Please enter a valid email address")
            return
        }

        view.showLoading(true)

        // Firebase will send actual password reset email
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                view.showLoading(false)

                if (task.isSuccessful) {
                    view.showSuccess("Password reset email sent! Check your inbox and follow the link.")
                    // Navigate back to login after a short delay
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        view.navigateToLogin()
                    }, 3000)
                } else {
                    val errorMessage = when {
                        task.exception?.message?.contains("INVALID_EMAIL") == true -> "Invalid email address"
                        task.exception?.message?.contains("USER_NOT_FOUND") == true -> "No account found with this email"
                        else -> "Failed to send reset email: ${task.exception?.message}"
                    }
                    view.showError(errorMessage)
                }
            }
    }

    override fun onDestroy() {
        // Clean up if needed
    }
}