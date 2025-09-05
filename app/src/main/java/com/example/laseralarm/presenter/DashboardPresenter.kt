package com.example.laseralarm.presenter

import com.example.laseralarm.view.DashboardView

class DashboardPresenter(private val view: DashboardView) {

    // Simulate fetching dashboard data (replace with Firebase/Realtime DB if needed)
    fun loadDashboard(userName: String) {
        view.showLoading(true)

        // Simulate network/database delay
        // Replace this with real DB fetch
        try {
            // Example data
            val status = "Active"
            val systemSafe = true
            val pastEvents = listOf(
                "Triggered at 2025-09-24 17:09",
                "Safe Since 2025-09-23 19:00"
            )

            // Update the view
            view.showStatus(status)
            view.showSystemStatus(systemSafe)
            view.showPastEvents(pastEvents)
        } catch (e: Exception) {
            view.showError(e.message ?: "Error loading dashboard")
        } finally {
            view.showLoading(false)
        }
    }
}
