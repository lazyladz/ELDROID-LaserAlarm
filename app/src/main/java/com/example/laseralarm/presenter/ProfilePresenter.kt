package com.example.laseralarm.presenter

import com.example.laseralarm.view.ProfileView

class ProfilePresenter(private val view: ProfileView) {

    fun loadProfile() {
        // TODO: fetch from database/session
        val username = "David"
        val email = "davidzaaron.serad@gmail.com"

        view.showProfile(username, email)
    }

    fun onEditProfileClicked() {
        view.showMessage("Edit Profile clicked")
        // Navigate to edit profile screen if needed
    }

    fun onLogoutClicked() {
        view.showMessage("Logged out")
        // TODO: handle logout (clear session, redirect to login, etc.)
    }
}
