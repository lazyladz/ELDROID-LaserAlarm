package com.example.laseralarm.view

interface HistoryView {
    fun showLoading(show: Boolean)
    fun showHistory(events: List<String>)
    fun showError(message: String)
}