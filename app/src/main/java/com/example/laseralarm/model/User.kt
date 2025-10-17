package com.example.laseralarm.model

// Updated User.kt
data class User(
    val fullName: String = "",
    val phone: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "" // Consider removing password from User model for security
)