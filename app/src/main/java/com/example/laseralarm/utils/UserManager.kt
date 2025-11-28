package com.example.laseralarm.utils

import android.content.Context
import android.content.SharedPreferences

object UserManager {
    private const val PREFS_NAME = "user_prefs"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_FULL_NAME = "full_name"
    private const val KEY_PHONE = "phone"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_FIREBASE_UID = "firebase_uid" // ADD THIS LINE

    // Method to save all user data
    fun saveUserData(context: Context, fullName: String, phone: String, username: String, email: String, userId: String = "") {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_PHONE, phone)
            .putString(KEY_USERNAME, username)
            .putString(KEY_EMAIL, email)
            .putString(KEY_USER_ID, userId)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    // Overloaded method for backward compatibility (username and email only)
    fun saveUserData(context: Context, username: String, email: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_EMAIL, email)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    // Method to update only username and email (for profile edits)
    fun updateUserData(context: Context, username: String, email: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    // Method to update all user data
    fun updateUserData(context: Context, fullName: String, phone: String, username: String, email: String) {
        saveUserData(context, fullName, phone, username, email)
    }

    // User ID methods
    fun getUserId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_ID, "") ?: ""
    }

    fun saveUserId(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    // Firebase UID methods
    fun saveFirebaseUid(context: Context, uid: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FIREBASE_UID, uid).apply()
    }

    fun getFirebaseUid(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FIREBASE_UID, "") ?: ""
    }

    fun getFullName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FULL_NAME, "") ?: ""
    }

    fun getPhone(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PHONE, "") ?: ""
    }

    fun getUserData(context: Context): Pair<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val username = prefs.getString(KEY_USERNAME, "Unknown User") ?: "Unknown User"
        val email = prefs.getString(KEY_EMAIL, "No email") ?: "No email"
        return Pair(username, email)
    }

    // Method to get all user data
    fun getAllUserData(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return mapOf(
            "fullName" to (prefs.getString(KEY_FULL_NAME, "") ?: ""),
            "phone" to (prefs.getString(KEY_PHONE, "") ?: ""),
            "username" to (prefs.getString(KEY_USERNAME, "Unknown User") ?: "Unknown User"),
            "email" to (prefs.getString(KEY_EMAIL, "No email") ?: "No email"),
            "userId" to (prefs.getString(KEY_USER_ID, "") ?: ""),
            "firebaseUid" to (prefs.getString(KEY_FIREBASE_UID, "") ?: "") // ADD THIS
        )
    }

    fun getUsername(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USERNAME, "Unknown User") ?: "Unknown User"
    }

    fun getEmail(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_EMAIL, "No email") ?: "No email"
    }

    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}