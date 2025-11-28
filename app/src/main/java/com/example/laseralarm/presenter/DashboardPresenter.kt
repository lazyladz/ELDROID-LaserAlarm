package com.example.laseralarm.presenter

import android.content.Context
import android.util.Log
import com.example.laseralarm.utils.NotificationHelper
import com.example.laseralarm.utils.UserManager
import com.example.laseralarm.view.DashboardView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DashboardPresenter(private val view: DashboardView, private val context: Context) {

    private val database = FirebaseDatabase.getInstance("https://laseralarm-bc8e3-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private val devicesRef = database.getReference("alarm_devices")
    private val alarmsRef = database.getReference("alarms")
    private var currentUserId = ""
    private var deviceListeners = mutableMapOf<String, ValueEventListener>()
    private var alarmListeners = mutableMapOf<String, ValueEventListener>()
    private val notificationHelper = NotificationHelper(context)
    private val TAG = "DashboardPresenter"

    // Fixed: Use HashSet instead of mutableSetOf
    private val notifiedAlarms = HashSet<String>()

    fun loadDashboard(userName: String) {
        Log.d(TAG, "Loading dashboard for user: $userName")
        view.showLoading(true)
        currentUserId = UserManager.getUserId(context).ifEmpty {
            "user_${System.currentTimeMillis()}".also { UserManager.saveUserId(context, it) }
        }
        Log.d(TAG, "Current user ID: $currentUserId")
        fetchUserDevicesAndAlarms()
    }

    fun pairDevice(deviceId: String, deviceName: String, userId: String) {
        view.showLoading(true)
        currentUserId = userId
        UserManager.saveUserId(context, userId)

        if (!isValidDeviceId(deviceId)) {
            view.showLoading(false)
            view.showError("Invalid device ID format")
            return
        }

        devicesRef.child(deviceId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val existingOwner = snapshot.child("owner_uid").getValue(String::class.java)
                if (!existingOwner.isNullOrEmpty() && existingOwner != userId) {
                    view.showLoading(false)
                    view.showError("Device already paired with another user")
                } else {
                    pairDeviceToUser(deviceId, deviceName, userId)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                view.showLoading(false)
                view.showError("Database error: ${error.message}")
            }
        })
    }

    private fun pairDeviceToUser(deviceId: String, deviceName: String, userId: String) {
        val deviceData = mapOf(
            "owner_uid" to userId,
            "name" to deviceName,
            "paired_at" to ServerValue.TIMESTAMP,
            "status" to "active",
            "last_seen" to ServerValue.TIMESTAMP,
            "device_id" to deviceId
        )

        devicesRef.child(deviceId).updateChildren(deviceData)
            .addOnSuccessListener {
                view.showLoading(false)
                view.showStatus("Device Paired")
                fetchUserDevicesAndAlarms()
            }
            .addOnFailureListener { e ->
                view.showLoading(false)
                view.showError("Pairing failed: ${e.message}")
            }
    }

    private fun fetchUserDevicesAndAlarms() {
        removeAllDeviceListeners()

        devicesRef.orderByChild("owner_uid").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(devicesSnapshot: DataSnapshot) {
                    Log.d(TAG, "Devices snapshot exists: ${devicesSnapshot.exists()}")
                    if (!devicesSnapshot.exists()) {
                        showNoDevices()
                        return
                    }

                    val deviceList = mutableListOf<String>()
                    val allAlarms = mutableListOf<String>()
                    var devicesProcessed = 0
                    val totalDevices = devicesSnapshot.children.count()

                    Log.d(TAG, "Found $totalDevices devices")

                    for (deviceSnapshot in devicesSnapshot.children) {
                        val deviceId = deviceSnapshot.key ?: continue
                        val deviceName = deviceSnapshot.child("name").getValue(String::class.java) ?: "Unknown Device"
                        val deviceStatus = deviceSnapshot.child("status").getValue(String::class.java) ?: "unknown"

                        Log.d(TAG, "Device: $deviceName ($deviceId) - Status: $deviceStatus")
                        deviceList.add("$deviceName ($deviceId)")

                        // Setup real-time listener for device status
                        setupDeviceStatusListener(deviceId, deviceName)

                        // Setup real-time listener for new alarms (for notifications)
                        setupAlarmNotificationListener(deviceId, deviceName)

                        fetchDeviceAlarms(deviceId, deviceName) { deviceAlarms ->
                            allAlarms.addAll(deviceAlarms)
                            if (++devicesProcessed == totalDevices) {
                                val sortedAlarms = allAlarms.sortedByDescending {
                                    it.substringAfterLast("at ").takeIf { timeStr -> timeStr.isNotBlank() } ?: it
                                }
                                updateDashboardUI(deviceList, sortedAlarms)
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load devices: ${error.message}")
                    view.showLoading(false)
                    view.showError("Failed to load devices: ${error.message}")
                }
            })
    }

    private fun setupDeviceStatusListener(deviceId: String, deviceName: String) {
        val deviceStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Device status changed for: $deviceName")

                if (snapshot.exists()) {
                    val status = snapshot.child("status").getValue(String::class.java) ?: "unknown"
                    val lastSeen = snapshot.child("last_seen").getValue(Long::class.java) ?: 0
                    val currentTime = System.currentTimeMillis()

                    Log.d(TAG, "Current status: $status, Last seen: $lastSeen")

                    // Calculate time difference in minutes
                    val timeDiffMinutes = if (lastSeen > 0) {
                        TimeUnit.MILLISECONDS.toMinutes(currentTime - lastSeen)
                    } else {
                        Long.MAX_VALUE
                    }

                    Log.d(TAG, "Time difference: $timeDiffMinutes minutes")

                    // Determine if device is online (within 2 minutes)
                    val isOnline = timeDiffMinutes <= 2

                    Log.d(TAG, "Device online: $isOnline")

                    // Update UI based on online/offline status
                    updateSystemStatusUI(deviceName, status, isOnline, timeDiffMinutes)
                } else {
                    Log.d(TAG, "Device snapshot doesn't exist")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Device status listener cancelled for $deviceId: ${error.message}")
            }
        }

        // Store the listener for cleanup
        deviceListeners[deviceId] = deviceStatusListener

        // Start listening for device status changes
        devicesRef.child(deviceId).addValueEventListener(deviceStatusListener)
    }

    private fun setupAlarmNotificationListener(deviceId: String, deviceName: String) {
        val alarmListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Get the most recent alarm (last child)
                val recentAlarm = snapshot.children.lastOrNull() ?: return

                val alarmKey = "${deviceId}_${recentAlarm.key}"
                val eventType = recentAlarm.child("event_type").getValue(String::class.java) ?: ""
                val event = recentAlarm.child("event").getValue(String::class.java) ?: ""
                val timestamp = recentAlarm.child("server_timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()

                Log.d(TAG, "Alarm detected - Key: $alarmKey, Type: $eventType, Event: $event")

                // Check if this is a new ALARM_START event and we haven't notified about it yet
                if (eventType == "ALARM_START" && !notifiedAlarms.contains(alarmKey)) {
                    notifiedAlarms.add(alarmKey)

                    val currentTime = System.currentTimeMillis()
                    // Only notify if the alarm is recent (within last 2 minutes)
                    if (currentTime - timestamp < TimeUnit.MINUTES.toMillis(2)) {
                        Log.d(TAG, "New alarm detected: $eventType - $event")

                        // Show push notification
                        notificationHelper.showAlarmNotification(
                            deviceName,
                            "Laser beam interrupted! Immediate attention required."
                        )

                        // Show in-app notification
                        notificationHelper.showInAppNotification(
                            "$deviceName: Alarm triggered! Laser beam interrupted."
                        )
                    }

                    // Clean up old notified alarms (keep only last 50 to prevent memory issues)
                    if (notifiedAlarms.size > 50) {
                        notifiedAlarms.clear()
                        // Re-add current one
                        notifiedAlarms.add(alarmKey)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Alarm notification listener cancelled for $deviceId: ${error.message}")
            }
        }

        // Store the listener for cleanup
        alarmListeners[deviceId] = alarmListener

        // Listen for new alarm events (limit to last 1 to get only new ones)
        alarmsRef.child(deviceId).limitToLast(1).addValueEventListener(alarmListener)
    }

    private fun updateSystemStatusUI(deviceName: String, status: String, isOnline: Boolean, minutesAgo: Long) {
        if (!isOnline) {
            // Device is offline
            Log.d(TAG, "Device is offline")
            view.showStatus("🔴 $deviceName: OFFLINE")
            view.showSystemStatus(false)
            return
        }

        when (status) {
            "triggered" -> {
                Log.d(TAG, "Device status: TRIGGERED")
                view.showStatus("🚨 $deviceName: ALARM TRIGGERED!")
                view.showSystemStatus(false)
            }
            "active" -> {
                Log.d(TAG, "Device status: ACTIVE")
                view.showStatus("✅ $deviceName: Monitoring")
                view.showSystemStatus(true)
            }
            "disabled" -> {
                Log.d(TAG, "Device status: DISABLED")
                view.showStatus("⚠️ $deviceName: Disabled")
                view.showSystemStatus(false)
            }
            else -> {
                Log.d(TAG, "Device status: UNKNOWN")
                view.showStatus("❓ $deviceName: Unknown Status")
                view.showSystemStatus(false)
            }
        }
    }

    private fun fetchDeviceAlarms(deviceId: String, deviceName: String, callback: (List<String>) -> Unit) {
        alarmsRef.child(deviceId).orderByChild("server_timestamp").limitToLast(10)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val deviceAlarms = mutableListOf<String>()

                    for (alarmSnapshot in snapshot.children) {
                        val timestamp = getTimestampFromSnapshot(alarmSnapshot)
                        val alarmMessage = getAlarmMessage(alarmSnapshot)

                        if (timestamp != null) {
                            val formattedTime = formatTime(timestamp)
                            val eventType = alarmSnapshot.child("event_type").getValue(String::class.java) ?: ""

                            val displayMessage = when (eventType) {
                                "ALARM_START" -> "🚨 Alarm Triggered"
                                "ALARM_END" -> {
                                    val duration = alarmSnapshot.child("duration_seconds").getValue(Int::class.java) ?: 0
                                    "✅ Beam Restored (${duration}s)"
                                }
                                else -> alarmMessage
                            }

                            deviceAlarms.add("$deviceName: $displayMessage at $formattedTime")
                        }
                    }

                    callback(if (deviceAlarms.isEmpty()) listOf("$deviceName: No alarms yet") else deviceAlarms)
                }
                override fun onCancelled(error: DatabaseError) = callback(listOf("$deviceName: Error loading alarms"))
            })
    }

    private fun getTimestampFromSnapshot(snapshot: DataSnapshot): Long? {
        snapshot.child("server_timestamp").getValue(Long::class.java)?.let { return it }
        snapshot.child("alarm_start_time").getValue(Long::class.java)?.let { return it }
        snapshot.child("alarm_end_time").getValue(Long::class.java)?.let { return it }
        snapshot.child("timestamp").getValue(Long::class.java)?.let { return it }
        snapshot.key?.toLongOrNull()?.let { return it }
        return null
    }

    private fun getAlarmMessage(snapshot: DataSnapshot): String {
        return when {
            snapshot.child("event").exists() -> snapshot.child("event").getValue(String::class.java) ?: "Beam interrupted"
            snapshot.child("message").exists() -> snapshot.child("message").getValue(String::class.java) ?: "Beam interrupted"
            snapshot.value is String -> snapshot.value as String
            else -> "Beam interrupted"
        }
    }

    private fun updateDashboardUI(deviceList: List<String>, alarms: List<String>) {
        // Status will be updated by the real-time listeners
        view.showPastEvents(if (alarms.size > 5) alarms.take(5) else alarms)
        view.showLoading(false)
    }

    private fun showNoDevices() {
        view.showStatus("No Devices Paired")
        view.showSystemStatus(false)
        view.showPastEvents(listOf("No devices paired yet. Tap + to add your first alarm device."))
        view.showLoading(false)
        removeAllDeviceListeners()
    }

    private fun removeAllDeviceListeners() {
        deviceListeners.forEach { (deviceId, listener) ->
            devicesRef.child(deviceId).removeEventListener(listener)
        }
        deviceListeners.clear()

        alarmListeners.forEach { (deviceId, listener) ->
            alarmsRef.child(deviceId).removeEventListener(listener)
        }
        alarmListeners.clear()

        notifiedAlarms.clear()
    }

    private fun isValidDeviceId(deviceId: String) = deviceId.isNotBlank() && deviceId.length in 4..20 && deviceId.matches(Regex("[A-Za-z0-9_-]+"))

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun refreshDashboard() {
        if (currentUserId.isNotEmpty()) {
            fetchUserDevicesAndAlarms()
        }
    }

    fun onDestroy() {
        removeAllDeviceListeners()
        // Fixed: Correct method name
        notificationHelper.cancelAllNotifications()
    }
}