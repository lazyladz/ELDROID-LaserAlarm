package com.example.laseralarm.presenter

import android.content.Context
import com.example.laseralarm.utils.UserManager
import com.example.laseralarm.view.DashboardView
import com.example.laseralarm.view.activities.DashboardActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class DashboardPresenter(private val view: DashboardView, private val context: Context) {

    // FIX: Use the correct database URL with region
    private val database = FirebaseDatabase.getInstance("https://laseralarm-bc8e3-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val devicesRef = database.getReference("alarm_devices")
    private val alarmsRef = database.getReference("alarms")
    private var currentUserId: String = ""

    // Load dashboard data for a specific user
    fun loadDashboard(userName: String) {
        view.showLoading(true)

        // FIX: Use the same user ID that was saved during pairing
        currentUserId = UserManager.getUserId(context)

        // If no user ID exists yet, create one
        if (currentUserId.isEmpty()) {
            currentUserId = "user_${System.currentTimeMillis()}"
            UserManager.saveUserId(context, currentUserId)
        }

        println("🔍 DEBUG: Loading dashboard for user: $currentUserId")
        println("🔍 DEBUG: Username: $userName")

        // Fetch user's paired devices and their alarm history
        fetchUserDevicesAndAlarms()
    }

    // Pair a new device with the user
    fun pairDevice(deviceId: String, deviceName: String, userId: String) {
        view.showLoading(true)
        currentUserId = userId

        // Save the user ID to UserManager for consistency
        UserManager.saveUserId(context, userId)

        println("🔍 DEBUG: Pairing device $deviceId with user: $userId")

        // Validate device ID format
        if (!isValidDeviceId(deviceId)) {
            view.showLoading(false)
            (view as? DashboardActivity)?.onDevicePairedFailure("Invalid device ID format")
            return
        }

        // Check if device exists and is not already paired
        devicesRef.child(deviceId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Device exists, check if already paired
                    val existingOwner = snapshot.child("owner_uid").getValue(String::class.java)
                    if (!existingOwner.isNullOrEmpty() && existingOwner != userId) {
                        view.showLoading(false)
                        (view as? DashboardActivity)?.onDevicePairedFailure("Device already paired with another user")
                    } else {
                        // Device available for pairing
                        pairDeviceToUser(deviceId, deviceName, userId)
                    }
                } else {
                    // Device doesn't exist, create new device entry
                    pairDeviceToUser(deviceId, deviceName, userId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                view.showLoading(false)
                (view as? DashboardActivity)?.onDevicePairedFailure("Database error: ${error.message}")
            }
        })
    }

    private fun pairDeviceToUser(deviceId: String, deviceName: String, userId: String) {
        val deviceData = HashMap<String, Any>()
        deviceData["owner_uid"] = userId
        deviceData["name"] = deviceName
        deviceData["paired_at"] = System.currentTimeMillis()
        deviceData["status"] = "active"
        deviceData["last_seen"] = System.currentTimeMillis()
        deviceData["device_id"] = deviceId  // Add device_id for validation

        println("🔍 DEBUG: Saving device data to Firebase - Owner: $userId, Device: $deviceId")

        devicesRef.child(deviceId).updateChildren(deviceData)
            .addOnSuccessListener {
                println("✅ DEBUG: Device paired successfully in Firebase")
                view.showLoading(false)
                (view as? DashboardActivity)?.onDevicePairedSuccess(deviceName)

                // Refresh dashboard to show new device
                fetchUserDevicesAndAlarms()
            }
            .addOnFailureListener { e ->
                println("❌ DEBUG: Device pairing failed: ${e.message}")
                view.showLoading(false)
                (view as? DashboardActivity)?.onDevicePairedFailure("Pairing failed: ${e.message}")
            }
    }

    private fun fetchUserDevicesAndAlarms() {
        println("🔍 DEBUG: Fetching devices for user ID: $currentUserId")

        // Fetch all devices owned by current user
        devicesRef.orderByChild("owner_uid").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(devicesSnapshot: DataSnapshot) {
                    println("🔍 DEBUG: Found ${devicesSnapshot.children.count()} devices in database")

                    // Log all devices found to see what's in Firebase
                    for (deviceSnapshot in devicesSnapshot.children) {
                        val deviceId = deviceSnapshot.key
                        val deviceName = deviceSnapshot.child("name").getValue(String::class.java)
                        val ownerUid = deviceSnapshot.child("owner_uid").getValue(String::class.java)
                        println("🔍 DEBUG: Device - ID: $deviceId, Name: $deviceName, Owner: $ownerUid")
                    }

                    if (!devicesSnapshot.exists()) {
                        println("❌ DEBUG: No devices found for user $currentUserId")
                        view.showStatus("No Devices")
                        view.showSystemStatus(true) // Default to safe
                        view.showPastEvents(listOf("No devices paired yet. Tap + to add your first alarm device."))
                        view.showLoading(false)
                        return
                    }

                    val deviceList = mutableListOf<String>()
                    val allAlarms = mutableListOf<String>()
                    var hasActiveAlarm = false
                    var devicesProcessed = 0

                    // Process each device
                    for (deviceSnapshot in devicesSnapshot.children) {
                        val deviceId = deviceSnapshot.key ?: continue
                        val deviceName = deviceSnapshot.child("name").getValue(String::class.java) ?: "Unknown Device"
                        deviceList.add("$deviceName ($deviceId)")

                        // Fetch alarms for this device
                        fetchDeviceAlarms(deviceId, deviceName) { deviceAlarms, deviceHasActiveAlarm ->
                            allAlarms.addAll(deviceAlarms)
                            hasActiveAlarm = hasActiveAlarm || deviceHasActiveAlarm
                            devicesProcessed++

                            // Update UI when all devices processed
                            if (devicesProcessed == devicesSnapshot.children.count()) {
                                updateDashboardUI(deviceList, allAlarms, hasActiveAlarm)
                            }
                        }
                    }

                    // Handle case where no devices have alarms
                    if (devicesSnapshot.children.count() == 0) {
                        updateDashboardUI(emptyList(), emptyList(), false)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("❌ DEBUG: Error fetching devices: ${error.message}")
                    view.showLoading(false)
                    view.showError("Failed to load devices: ${error.message}")
                }
            })
    }

    private fun fetchDeviceAlarms(deviceId: String, deviceName: String, callback: (List<String>, Boolean) -> Unit) {
        println("🔍 DEBUG: Fetching alarms for device: $deviceId")

        alarmsRef.child(deviceId).orderByKey().limitToLast(10) // Get last 10 alarms
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    println("🔍 DEBUG: Found ${snapshot.children.count()} alarm entries for device $deviceId")

                    val deviceAlarms = mutableListOf<String>()
                    var hasActiveAlarm = false

                    for (alarmSnapshot in snapshot.children) {
                        try {
                            val timestamp = alarmSnapshot.key?.toLongOrNull() ?: continue

                            // Handle both string and object alarm data
                            val alarmMessage = when {
                                alarmSnapshot.value is String -> alarmSnapshot.value as String
                                alarmSnapshot.child("event").exists() -> alarmSnapshot.child("event").getValue(String::class.java) ?: "Beam interrupted"
                                else -> "Beam interrupted"
                            }

                            val date = Date(timestamp)
                            val formattedTime = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(date)

                            deviceAlarms.add("$deviceName: $alarmMessage at $formattedTime")

                            // Check if this is a recent active alarm (last 5 minutes)
                            if (System.currentTimeMillis() - timestamp < 5 * 60 * 1000 &&
                                alarmMessage.contains("interrupted", true)) {
                                hasActiveAlarm = true
                            }
                        } catch (e: Exception) {
                            // Skip invalid alarm entries
                        }
                    }

                    // If no alarms found, add a default message
                    if (deviceAlarms.isEmpty()) {
                        deviceAlarms.add("$deviceName: No alarms recorded yet")
                    } else {
                        // Sort by timestamp (newest first)
                        deviceAlarms.sortDescending()
                    }

                    callback(deviceAlarms, hasActiveAlarm)
                }

                override fun onCancelled(error: DatabaseError) {
                    println("❌ DEBUG: Error fetching alarms: ${error.message}")
                    // Continue with empty alarms for this device
                    callback(listOf("$deviceName: Error loading alarms"), false)
                }
            })
    }

    private fun updateDashboardUI(deviceList: List<String>, alarms: List<String>, hasActiveAlarm: Boolean) {
        println("🔍 DEBUG: Updating UI - Devices: ${deviceList.size}, Alarms: ${alarms.size}, Active Alarm: $hasActiveAlarm")

        // Determine overall status
        val status = when {
            deviceList.isEmpty() -> "No Devices"
            hasActiveAlarm -> "Alert!"
            else -> "Monitoring"
        }

        // System status (safe = no active alarms)
        val systemSafe = !hasActiveAlarm

        // Prepare past events (show latest 5 alarms)
        val recentAlarms = if (alarms.size > 5) alarms.take(5) else alarms

        // Update view
        view.showStatus(status)
        view.showSystemStatus(systemSafe)
        view.showPastEvents(recentAlarms)
        view.showLoading(false)
    }

    // Validate device ID format
    private fun isValidDeviceId(deviceId: String): Boolean {
        // Basic validation - adjust based on your Arduino ID format
        return deviceId.isNotBlank() && deviceId.length in 4..20 && deviceId.matches(Regex("[A-Za-z0-9_-]+"))
    }

    // Method to manually refresh dashboard
    fun refreshDashboard() {
        if (currentUserId.isNotEmpty()) {
            fetchUserDevicesAndAlarms()
        }
    }

    // Clean up listeners if needed
    fun onDestroy() {
        // Clean up any ongoing listeners here if needed
    }
}