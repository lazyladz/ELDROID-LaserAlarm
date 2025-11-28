package com.example.laseralarm.presenter

import android.os.Handler
import android.os.Looper
import com.example.laseralarm.view.LandingView

class LandingPresenter(private val view: LandingView) {

    private val splashDuration: Long = 2000 // 2 seconds

    fun start() {
        Handler(Looper.getMainLooper()).postDelayed({
            view.navigateToLogin()
        }, splashDuration)
    }
}

