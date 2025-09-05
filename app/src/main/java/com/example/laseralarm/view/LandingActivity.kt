package com.example.laseralarm.view.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.laseralarm.R
import com.example.laseralarm.presenter.LandingPresenter
import com.example.laseralarm.view.LandingView

class LandingActivity : AppCompatActivity(), LandingView {

    private lateinit var presenter: LandingPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.landingpage)

        presenter = LandingPresenter(this)
        presenter.start() // automatically navigate to LoginActivity after delay
    }

    override fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
