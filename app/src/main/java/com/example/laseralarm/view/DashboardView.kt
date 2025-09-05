package com.example.laseralarm.view

interface DashboardView {
    fun showLoading(show: Boolean)
    fun showStatus(status: String)
    fun showSystemStatus(safe: Boolean)
    fun showPastEvents(events: List<String>)
    fun showError(message: String)
}
