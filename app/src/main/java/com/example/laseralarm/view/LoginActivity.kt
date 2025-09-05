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
import com.example.laseralarm.model.UserRepository
import com.example.laseralarm.presenter.LoginPresenter
import com.example.laseralarm.view.LoginView
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity(), LoginView {

    private lateinit var loginPresenter: LoginPresenter
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        loginPresenter = LoginPresenter(this, UserRepository(FirebaseAuth.getInstance()))

        val username = findViewById<EditText>(R.id.etUsername)
        val password = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        progressBar = findViewById(R.id.progressBar)

        btnLogin.setOnClickListener {
            val email = username.text.toString().trim()
            val pass = password.text.toString().trim()
            loginPresenter.login(email, pass)
        }

        tvRegister.setOnClickListener {
            navigateToRegister()
        }
    }

    override fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) ProgressBar.VISIBLE else ProgressBar.GONE
    }

    override fun onLoginSuccess() {
        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
        // TODO: Navigate to dashboard
    }

    override fun onLoginFailure(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }
}
