package com.example.laseralarm.view.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.laseralarm.R
import com.example.laseralarm.model.User
import com.example.laseralarm.model.UserRepository
import com.example.laseralarm.presenter.LoginPresenter
import com.example.laseralarm.utils.UserManager
import com.example.laseralarm.view.LoginView

class LoginActivity : AppCompatActivity(), LoginView {

    private lateinit var loginPresenter: LoginPresenter
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        loginPresenter = LoginPresenter(this, UserRepository())

        val username = findViewById<EditText>(R.id.etUsername)
        val password = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        progressBar = findViewById(R.id.progressBar)

        btnLogin.setOnClickListener {
            val email = username.text.toString().trim()
            val pass = password.text.toString().trim()
            loginPresenter.login(email, pass)
        }

        tvRegister.setOnClickListener {
            navigateToRegister()
        }

        tvForgotPassword.setOnClickListener {
            navigateToForgotPassword()
        }
    }

    override fun onResume() {
        super.onResume()
        // Handle success messages from password reset flow
        if (intent.getBooleanExtra("reset_email_sent", false)) {
            Toast.makeText(this, "Password reset email sent! Check your inbox.", Toast.LENGTH_LONG).show()
            intent.removeExtra("reset_email_sent")
        }
    }

    override fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) ProgressBar.VISIBLE else ProgressBar.GONE
    }

    override fun onLoginSuccess(user: User) {
        Toast.makeText(this, "Welcome ${user.username}!", Toast.LENGTH_SHORT).show()

        UserManager.saveUserData(
            this,
            user.fullName,
            user.phone,
            user.username,
            user.email
        )

        val intent = Intent(this, DashboardActivity::class.java)
        intent.putExtra("username", user.username)
        intent.putExtra("email", user.email)
        startActivity(intent)
        finish()
    }

    override fun onLoginFailure(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    private fun navigateToForgotPassword() {
        startActivity(Intent(this, ForgotPasswordEmailActivity::class.java))
    }
}