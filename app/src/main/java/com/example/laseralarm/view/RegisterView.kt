package com.example.laseralarm.view

interface RegisterView {
    fun onRegisterSuccess(message: String)
    fun onRegisterFailure(message: String)
    fun showLoading(show: Boolean)
}
