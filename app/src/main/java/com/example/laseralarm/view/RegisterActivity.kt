package com.example.laseralarm.view.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.laseralarm.R
import com.example.laseralarm.model.UserRepository
import com.example.laseralarm.presenter.RegisterPresenter
import com.example.laseralarm.view.RegisterView
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity(), RegisterView {

    private lateinit var presenter: RegisterPresenter
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register)

        // Initialize all EditText fields
        val fullNameEt = findViewById<EditText>(R.id.fullNameEt)
        val phoneEt = findViewById<EditText>(R.id.phoneEt)
        val usernameEt = findViewById<EditText>(R.id.usernameEt)
        val emailEt = findViewById<EditText>(R.id.emailEt)
        val passwordEt = findViewById<EditText>(R.id.passwordEt)
        val termsCb = findViewById<CheckBox>(R.id.termsCb)
        val registerBtn = findViewById<Button>(R.id.registerBtn)
        val loginText = findViewById<TextView>(R.id.loginText)
        progressBar = findViewById(R.id.progressBar)

        presenter = RegisterPresenter(this, UserRepository())

        registerBtn.setOnClickListener {
            val fullName = fullNameEt.text.toString().trim()
            val phone = phoneEt.text.toString().trim()
            val username = usernameEt.text.toString().trim()
            val email = emailEt.text.toString().trim()
            val password = passwordEt.text.toString().trim()
            val acceptedTerms = termsCb.isChecked

            presenter.registerUser(fullName, phone, username, email, password, acceptedTerms)
        }

        loginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) ProgressBar.VISIBLE else ProgressBar.GONE
    }

    override fun onRegisterSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onRegisterFailure(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}