package com.example.laseralarm.view

interface DashboardView {
    fun showLoading(show: Boolean)
    fun showStatus(status: String)
    fun showSystemStatus(isOnline: Boolean) // Simplified to just online/offline
    fun showPastEvents(events: List<String>)
    fun showError(message: String)
}