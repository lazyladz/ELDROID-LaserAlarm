package com.example.laseralarm.view.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.laseralarm.R
import com.example.laseralarm.presenter.DashboardPresenter
import com.example.laseralarm.view.DashboardView
import com.example.laseralarm.view.fragments.HistoryDialogFragment
import com.example.laseralarm.view.fragments.ProfileDialogFragment

class DashboardActivity : AppCompatActivity(), DashboardView {

    private lateinit var presenter: DashboardPresenter
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var systemSafeText: TextView
    private lateinit var systemTriggeredText: TextView
    private lateinit var eventsLayout: LinearLayout
    private lateinit var greetingText: TextView
    private lateinit var btnProfile: ImageView
    private lateinit var btnHistory: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard)

        // Bind views
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.tvStatus)
        systemSafeText = findViewById(R.id.tvSafe)
        systemTriggeredText = findViewById(R.id.tvTriggered)
        eventsLayout = findViewById(R.id.eventList)
        greetingText = findViewById(R.id.tvGreeting)
        btnProfile = findViewById(R.id.btnProfile)
        btnHistory = findViewById(R.id.btnHistory)

        // Initialize presenter
        presenter = DashboardPresenter(this)

        // Example username
        val userName = "David"
        greetingText.text = "Hello user, $userName"

        // Load dashboard data
        presenter.loadDashboard(userName)


        btnProfile.setOnClickListener {
            val dialog = ProfileDialogFragment()
            dialog.show(supportFragmentManager, "ProfileDialog")
        }

        btnHistory.setOnClickListener {
            val dialog = HistoryDialogFragment()
            dialog.show(supportFragmentManager, "HistoryDialog")
        }

    }

    override fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) ProgressBar.VISIBLE else ProgressBar.GONE
    }

    override fun showStatus(status: String) {
        statusText.text = "Status: $status"
    }

    override fun showSystemStatus(safe: Boolean) {
        systemSafeText.setTextColor(
            resources.getColor(if (safe) android.R.color.holo_green_light else android.R.color.white, null)
        )
        systemTriggeredText.setTextColor(
            resources.getColor(if (!safe) android.R.color.holo_red_light else android.R.color.white, null)
        )
    }

    override fun showPastEvents(events: List<String>) {
        eventsLayout.removeAllViews()
        events.forEach { event ->
            val tv = TextView(this)
            tv.text = event
            tv.setTextColor(resources.getColor(android.R.color.white, null))
            tv.textSize = 14f
            eventsLayout.addView(tv)
        }
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
