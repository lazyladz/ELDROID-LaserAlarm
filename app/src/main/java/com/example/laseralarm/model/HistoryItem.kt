package com.example.laseralarm.model

data class HistoryItem(
    val value: Int = 0,          // Laser status (0 = Safe, 1 = Triggered)
    val timestamp: Long = 0L,    // Time the event occurred
    val source: String? = null   // Who or what triggered it (ESP32 / Android App)
)
