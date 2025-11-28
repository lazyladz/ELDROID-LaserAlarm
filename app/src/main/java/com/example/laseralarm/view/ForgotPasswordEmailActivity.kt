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
import com.example.laseralarm.contract.ForgotPasswordEmailContract
import com.example.laseralarm.presenter.ForgotPasswordEmailPresenter

class ForgotPasswordEmailActivity : AppCompatActivity(), ForgotPasswordEmailContract.View {

    private lateinit var presenter: ForgotPasswordEmailPresenter
    private lateinit var etEmail: EditText
    private lateinit var btnSendResetLink: Button // Renamed for clarity
    private lateinit var progressBar: ProgressBar
    private lateinit var tvBackToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgot_password_email)

        initViews()
        setupClickListeners()

        presenter = ForgotPasswordEmailPresenter(this)
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        btnSendResetLink = findViewById(R.id.btnSendCode) // Use existing button ID
        progressBar = findViewById(R.id.progressBar)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)
    }

    private fun setupClickListeners() {
        btnSendResetLink.setOnClickListener {
            val email = etEmail.text.toString().trim()
            presenter.sendPasswordResetEmail(email) // Correct method call
        }

        tvBackToLogin.setOnClickListener {
            navigateToLogin() // You can call this directly
        }
    }

    override fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) ProgressBar.VISIBLE else ProgressBar.GONE
        btnSendResetLink.isEnabled = !show
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun navigateToLogin() { // Implement this method
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("reset_email_sent", true)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}