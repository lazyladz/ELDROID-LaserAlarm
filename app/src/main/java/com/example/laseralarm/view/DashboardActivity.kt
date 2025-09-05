package com.example.laseralarm.view.activities

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.laseralarm.R
import com.example.laseralarm.presenter.DashboardPresenter
import com.example.laseralarm.view.DashboardView

class DashboardActivity : AppCompatActivity(), DashboardView {

    private lateinit var presenter: DashboardPresenter
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var systemStatusText: TextView
    private lateinit var eventsLayout: LinearLayout
    private lateinit var userNameText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Bind views
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.tvStatus)
        systemStatusText = findViewById(R.id.tvSystemStatus)
        eventsLayout = findViewById(R.id.eventsLayout)
        userNameText = findViewById(R.id.tvUserName)

        // Initialize presenter
        presenter = DashboardPresenter(this)

        // Example username
        val userName = "David"
        userNameText.text = "Hello user, $userName"

        // Load dashboard data
        presenter.loadDashboard(userName)
    }

    override fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) ProgressBar.VISIBLE else ProgressBar.GONE
    }

    override fun showStatus(status: String) {
        statusText.text = "Status: $status"
    }

    override fun showSystemStatus(safe: Boolean) {
        systemStatusText.text = if (safe) "System Status: Safe" else "System Status: Triggered"
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
