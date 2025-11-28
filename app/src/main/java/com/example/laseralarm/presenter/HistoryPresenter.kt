package com.example.laseralarm.presenter

import android.content.Context
import android.util.Log
import com.example.laseralarm.utils.UserManager
import com.example.laseralarm.view.HistoryView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class HistoryPresenter(private val view: HistoryView, private val context: Context) {

    private val database = FirebaseDatabase.getInstance("https://laseralarm-bc8e3-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private val devicesRef = database.getReference("alarm_devices")
    private val alarmsRef = database.getReference("alarms")
    private var currentUserId = ""
    private val TAG = "HistoryPresenter"

    fun loadHistory() {
        view.showLoading(true)
        currentUserId = UserManager.getUserId(context).ifEmpty {
            "user_${System.currentTimeMillis()}".also { UserManager.saveUserId(context, it) }
        }

        Log.d(TAG, "Loading history for user: $currentUserId")
        fetchUserDevicesAndHistory()
    }

    private fun fetchUserDevicesAndHistory() {
        devicesRef.orderByChild("owner_uid").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(devicesSnapshot: DataSnapshot) {
                    if (!devicesSnapshot.exists()) {
                        Log.d(TAG, "No devices found for user")
                        view.showHistory(emptyList())
                        return
                    }

                    val allHistoryEvents = mutableListOf<String>()
                    val devicesProcessed = mutableListOf<String>()
                    val totalDevices = devicesSnapshot.children.count()

                    for (deviceSnapshot in devicesSnapshot.children) {
                        val deviceId = deviceSnapshot.key ?: continue
                        val deviceName = deviceSnapshot.child("name").getValue(String::class.java) ?: "Unknown Device"

                        Log.d(TAG, "Fetching history for device: $deviceName ($deviceId)")

                        fetchDeviceHistory(deviceId, deviceName) { deviceHistory ->
                            allHistoryEvents.addAll(deviceHistory)
                            devicesProcessed.add(deviceId)

                            if (devicesProcessed.size == totalDevices) {
                                // Sort all events by timestamp (newest first)
                                val sortedEvents = allHistoryEvents.sortedByDescending { event ->
                                    // Extract timestamp from the event string for sorting
                                    extractTimestampFromEvent(event)
                                }
                                view.showHistory(sortedEvents)
                                Log.d(TAG, "Loaded ${sortedEvents.size} history events")
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load devices: ${error.message}")
                    view.showError("Failed to load history: ${error.message}")
                }
            })
    }

    private fun fetchDeviceHistory(deviceId: String, deviceName: String, callback: (List<String>) -> Unit) {
        alarmsRef.child(deviceId).orderByChild("server_timestamp").limitToLast(50) // Get last 50 events
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val deviceHistory = mutableListOf<String>()

                    Log.d(TAG, "Found ${snapshot.childrenCount} alarm events for device $deviceId")

                    for (alarmSnapshot in snapshot.children) {
                        val timestamp = getTimestampFromSnapshot(alarmSnapshot)
                        val eventType = alarmSnapshot.child("event_type").getValue(String::class.java) ?: ""
                        val event = alarmSnapshot.child("event").getValue(String::class.java) ?: "Unknown Event"

                        if (timestamp != null) {
                            val formattedTime = formatTime(timestamp)
                            val duration = alarmSnapshot.child("duration_seconds").getValue(Int::class.java) ?: 0

                            val displayMessage = when (eventType) {
                                "ALARM_START" -> "🚨 $deviceName: Alarm Triggered"
                                "ALARM_END" -> "✅ $deviceName: Beam Restored (${duration}s)"
                                else -> "📝 $deviceName: $event"
                            }

                            val historyEntry = "$displayMessage\n⏰ $formattedTime"
                            deviceHistory.add(historyEntry)
                        }
                    }

                    callback(if (deviceHistory.isEmpty()) listOf("$deviceName: No alarm history") else deviceHistory)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load history for device $deviceId: ${error.message}")
                    callback(listOf("$deviceName: Error loading history"))
                }
            })
    }

    private fun getTimestampFromSnapshot(snapshot: DataSnapshot): Long? {
        // Priority 1: Check for server timestamp (primary field)
        snapshot.child("server_timestamp").getValue(Long::class.java)?.let { return it }
        snapshot.child("alarm_start_time").getValue(Long::class.java)?.let { return it }
        snapshot.child("alarm_end_time").getValue(Long::class.java)?.let { return it }
        snapshot.child("timestamp").getValue(Long::class.java)?.let { return it }
        snapshot.key?.toLongOrNull()?.let { return it }
        return null
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun extractTimestampFromEvent(event: String): Long {
        // Try to extract timestamp from the event string for sorting
        // This is a fallback method - events should already be sorted by Firebase query
        return System.currentTimeMillis()
    }

    fun onDestroy() {
        // Clean up any listeners if needed
    }
}