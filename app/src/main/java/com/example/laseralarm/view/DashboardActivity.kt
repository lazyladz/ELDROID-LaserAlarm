package com.example.laseralarm.view.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.laseralarm.R
import com.example.laseralarm.model.UserRepository
import com.example.laseralarm.presenter.DashboardPresenter
import com.example.laseralarm.utils.UserManager
import com.example.laseralarm.view.DashboardView
import com.example.laseralarm.view.fragments.HistoryDialogFragment
import com.example.laseralarm.view.fragments.ProfileDialogFragment
import com.example.laseralarm.view.fragments.PairDeviceDialogFragment
import com.example.laseralarm.view.fragments.NotificationHistoryDialogFragment
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity(), DashboardView {

    private lateinit var presenter: DashboardPresenter
    private lateinit var userRepository: UserRepository
    private lateinit var auth: FirebaseAuth

    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var eventsLayout: LinearLayout
    private lateinit var greetingText: TextView
    private lateinit var btnProfile: ImageView
    private lateinit var btnHistory: ImageView
    private lateinit var btnAddDevice: ImageView
    private lateinit var btnNotification: ImageView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var tvOnline: TextView
    private lateinit var tvOffline: TextView

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard)

        // Request notification permission
        requestNotificationPermission()

        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()

        initViews()
        setupSwipeRefresh()

        presenter = DashboardPresenter(this, this)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            handleAuthenticatedUser(currentUser.uid, currentUser.email ?: "")
        } else {
            handleLegacyUserData()
        }

        setupClickListeners()

        // Check if coming from notification
        if (intent.getBooleanExtra("from_notification", false)) {
            Toast.makeText(this, "🔔 Opening from alarm notification", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "🔔 Notification permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "🔕 Notifications disabled. Enable in app settings for alarm alerts.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.tvStatus)
        eventsLayout = findViewById(R.id.eventList)
        greetingText = findViewById(R.id.tvGreeting)
        btnProfile = findViewById(R.id.btnProfile)
        btnHistory = findViewById(R.id.btnHistory)
        btnAddDevice = findViewById(R.id.btnAddDevice)
        btnNotification = findViewById(R.id.ivNotification)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        tvOnline = findViewById(R.id.tvOnline)
        tvOffline = findViewById(R.id.tvOffline)
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(
            resources.getColor(android.R.color.holo_red_light, null),
            resources.getColor(android.R.color.holo_green_light, null),
            resources.getColor(android.R.color.holo_blue_light, null)
        )

        swipeRefreshLayout.setOnRefreshListener {
            refreshDashboard()
        }
    }

    private fun refreshDashboard() {
        val userName = UserManager.getUsername(this)
        presenter.loadDashboard(userName)
    }

    private fun handleAuthenticatedUser(uid: String, email: String) {
        UserManager.saveFirebaseUid(this, uid)
        UserManager.saveUserId(this, uid)

        userRepository.getUserData(uid) { user ->
            if (user != null) {
                UserManager.saveUserData(this, user.fullName, user.phone, user.username, user.email, uid)
                greetingText.text = "Hello, ${user.fullName}!"
            } else {
                val username = email.substringBefore("@")
                UserManager.saveUserData(this, username, "", username, email, uid)
                greetingText.text = "Hello, $username!"
            }

            presenter.loadDashboard(UserManager.getUsername(this))
        }
    }

    private fun handleLegacyUserData() {
        val usernameFromIntent = intent.getStringExtra("username")
        val emailFromIntent = intent.getStringExtra("email")
        val userIdFromIntent = intent.getStringExtra("userId")

        if (!usernameFromIntent.isNullOrEmpty() && !emailFromIntent.isNullOrEmpty()) {
            val userId = userIdFromIntent ?: generateUserId()
            UserManager.saveUserData(this, usernameFromIntent, emailFromIntent)
            UserManager.saveUserId(this, userId)
        }

        val userName = UserManager.getUsername(this)
        greetingText.text = "Hello, $userName!"

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

        // Notification button click listener
        btnNotification.setOnClickListener {
            onNotificationButtonClicked()
        }
    }

    private fun onNotificationButtonClicked() {
        // Show notification history
        showNotificationHistory()

        // Add vibration feedback
        try {
            val vibrator = getSystemService(android.os.Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showNotificationHistory() {
        val dialog = NotificationHistoryDialogFragment()
        dialog.show(supportFragmentManager, "NotificationHistoryDialog")
    }

    private fun showPairDeviceDialog() {
        val dialog = PairDeviceDialogFragment().apply {
            setOnDevicePairedListener { deviceId, deviceName ->
                val userId = if (auth.currentUser != null) {
                    auth.currentUser!!.uid
                } else {
                    UserManager.getUserId(this@DashboardActivity).takeIf { it.isNotEmpty() }
                        ?: generateUserId().also { UserManager.saveUserId(this@DashboardActivity, it) }
                }

                presenter.pairDevice(deviceId, deviceName, userId)
            }
        }
        dialog.show(supportFragmentManager, "PairDeviceDialog")
    }

    override fun showLoading(show: Boolean) {
        runOnUiThread {
            progressBar.visibility = if (show) ProgressBar.VISIBLE else ProgressBar.GONE

            // Hide swipe refresh indicator when loading is complete
            if (!show && swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    override fun showStatus(status: String) {
        runOnUiThread {
            statusText.text = status
        }
    }

    override fun showSystemStatus(isOnline: Boolean) {
        runOnUiThread {
            if (isOnline) {
                // Highlight Online, dim Offline
                tvOnline.setBackgroundResource(R.drawable.bg_online_chip_active)
                tvOffline.setBackgroundResource(R.drawable.bg_offline_chip)
                tvOnline.text = "✅ ONLINE"
                tvOffline.text = "Offline"
            } else {
                // Highlight Offline, dim Online
                tvOnline.setBackgroundResource(R.drawable.bg_online_chip)
                tvOffline.setBackgroundResource(R.drawable.bg_offline_chip_active)
                tvOnline.text = "Online"
                tvOffline.text = "🔴 OFFLINE"
            }
        }
    }

    override fun showPastEvents(events: List<String>) {
        runOnUiThread {
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
    }

    override fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

            // Ensure refresh indicator is hidden on error
            if (swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    fun onDevicePairedSuccess(deviceName: String) {
        runOnUiThread {
            Toast.makeText(this, "✅ Successfully paired with $deviceName", Toast.LENGTH_LONG).show()
            refreshDashboard()
        }
    }

    fun onDevicePairedFailure(errorMessage: String) {
        runOnUiThread {
            Toast.makeText(this, "❌ Pairing failed: $errorMessage", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}