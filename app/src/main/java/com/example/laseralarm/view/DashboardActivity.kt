package com.example.laseralarm.view.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.laseralarm.R
import com.example.laseralarm.model.UserRepository
import com.example.laseralarm.presenter.DashboardPresenter
import com.example.laseralarm.utils.UserManager
import com.example.laseralarm.view.DashboardView
import com.example.laseralarm.view.fragments.HistoryDialogFragment
import com.example.laseralarm.view.fragments.ProfileDialogFragment
import com.example.laseralarm.view.fragments.PairDeviceDialogFragment
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity(), DashboardView {

    private lateinit var presenter: DashboardPresenter
    private lateinit var userRepository: UserRepository
    private lateinit var auth: FirebaseAuth

    // Views
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var systemSafeText: TextView
    private lateinit var systemTriggeredText: TextView
    private lateinit var eventsLayout: LinearLayout
    private lateinit var greetingText: TextView
    private lateinit var btnProfile: ImageView
    private lateinit var btnHistory: ImageView
    private lateinit var btnAddDevice: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard)

        // Initialize Firebase Auth and Repository
        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()

        // Bind views
        initViews()

        // Initialize presenter with context
        presenter = DashboardPresenter(this, this)

        // Check if user is logged in with Firebase Auth
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is logged in with Firebase Auth
            handleAuthenticatedUser(currentUser.uid, currentUser.email ?: "")
        } else {
            // Fallback to legacy user data
            handleLegacyUserData()
        }

        setupClickListeners()
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.tvStatus)
        systemSafeText = findViewById(R.id.tvSafe)
        systemTriggeredText = findViewById(R.id.tvTriggered)
        eventsLayout = findViewById(R.id.eventList)
        greetingText = findViewById(R.id.tvGreeting)
        btnProfile = findViewById(R.id.btnProfile)
        btnHistory = findViewById(R.id.btnHistory)
        btnAddDevice = findViewById(R.id.btnAddDevice)
    }

    private fun handleAuthenticatedUser(uid: String, email: String) {
        // Save Firebase UID to UserManager
        UserManager.saveFirebaseUid(this, uid)
        UserManager.saveUserId(this, uid) // Use Firebase UID as user ID

        // Fetch user data from Firebase
        userRepository.getUserData(uid) { user ->
            if (user != null) {
                // Save complete user data
                UserManager.saveUserData(this, user.fullName, user.phone, user.username, user.email, uid)
                greetingText.text = "Hello, ${user.fullName}!"
            } else {
                // Use email as fallback
                val username = email.substringBefore("@")
                UserManager.saveUserData(this, username, "", username, email, uid)
                greetingText.text = "Hello, $username!"
            }

            // Load dashboard with the correct Firebase UID
            presenter.loadDashboard(UserManager.getUsername(this))
        }
    }

    private fun handleLegacyUserData() {
        // ✅ Get user data from Intent and save to UserManager (backward compatibility)
        val usernameFromIntent = intent.getStringExtra("username")
        val emailFromIntent = intent.getStringExtra("email")
        val userIdFromIntent = intent.getStringExtra("userId")

        if (!usernameFromIntent.isNullOrEmpty() && !emailFromIntent.isNullOrEmpty()) {
            val userId = userIdFromIntent ?: generateUserId()
            UserManager.saveUserData(this, usernameFromIntent, emailFromIntent)
            UserManager.saveUserId(this, userId)
        }

        // ✅ Get username from UserManager (SharedPreferences)
        val userName = UserManager.getUsername(this)
        greetingText.text = "Hello, $userName!"

        // Load dashboard data
        presenter.loadDashboard(userName)
    }

    private fun generateUserId(): String {
        return "user_${System.currentTimeMillis()}"
    }

    private fun setupClickListeners() {
        btnProfile.setOnClickListener {
            val dialog = ProfileDialogFragment()
            dialog.show(supportFragmentManager, "ProfileDialog")
        }

        btnHistory.setOnClickListener {
            val dialog = HistoryDialogFragment()
            dialog.show(supportFragmentManager, "HistoryDialog")
        }

        btnAddDevice.setOnClickListener {
            showPairDeviceDialog()
        }
    }

    private fun showPairDeviceDialog() {
        val dialog = PairDeviceDialogFragment().apply {
            setOnDevicePairedListener { deviceId, deviceName ->
                // Use Firebase UID if available, otherwise fallback
                val userId = if (auth.currentUser != null) {
                    auth.currentUser!!.uid
                } else {
                    UserManager.getUserId(this@DashboardActivity).takeIf { it.isNotEmpty() }
                        ?: generateUserId().also { UserManager.saveUserId(this@DashboardActivity, it) }
                }

                println("🆔 Using user ID for pairing: $userId") // Debug log

                // Handle device pairing
                presenter.pairDevice(deviceId, deviceName, userId)
            }
        }
        dialog.show(supportFragmentManager, "PairDeviceDialog")
    }

    override fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) ProgressBar.VISIBLE else ProgressBar.GONE
    }

    override fun showStatus(status: String) {
        statusText.text = "Status: $status"
    }

    override fun showSystemStatus(safe: Boolean) {
        val safeColor = if (safe) resources.getColor(android.R.color.holo_green_light, null)
        else resources.getColor(android.R.color.white, null)
        val triggeredColor = if (!safe) resources.getColor(android.R.color.holo_red_light, null)
        else resources.getColor(android.R.color.white, null)

        systemSafeText.setTextColor(safeColor)
        systemTriggeredText.setTextColor(triggeredColor)
    }

    override fun showPastEvents(events: List<String>) {
        eventsLayout.removeAllViews()
        if (events.isEmpty()) {
            val tv = TextView(this)
            tv.text = "No alarm events yet"
            tv.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            tv.textSize = 14f
            eventsLayout.addView(tv)
        } else {
            events.forEach { event ->
                val tv = TextView(this)
                tv.text = event
                tv.setTextColor(resources.getColor(android.R.color.white, null))
                tv.textSize = 14f
                tv.setPadding(0, 4, 0, 4)
                eventsLayout.addView(tv)
            }
        }
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Method to handle pairing success
    fun onDevicePairedSuccess(deviceName: String) {
        Toast.makeText(this, "✅ Successfully paired with $deviceName", Toast.LENGTH_LONG).show()
        // Refresh dashboard to show new device data
        presenter.loadDashboard(UserManager.getUsername(this))
    }

    // Method to handle pairing failure
    fun onDevicePairedFailure(errorMessage: String) {
        Toast.makeText(this, "❌ Pairing failed: $errorMessage", Toast.LENGTH_LONG).show()
    }
}