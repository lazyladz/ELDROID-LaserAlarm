package com.example.laseralarm.view

interface RegisterView {
    fun showLoading(show: Boolean)
    fun onRegisterSuccess(message: String)
    fun onRegisterFailure(message: String)
}