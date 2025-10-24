package com.example.laseralarm.presenter

import android.content.Context
import com.example.laseralarm.utils.UserManager
import com.example.laseralarm.view.DashboardView
import com.google.firebase.database.*
import java.util.Date
import java.util.Locale

class DashboardPresenter(private val view: DashboardView, private val context: Context) {

    private val database = FirebaseDatabase.getInstance("your-database-url")
    private val devicesRef = database.getReference("alarm_devices")
    private val alarmsRef = database.getReference("alarms")
    private var currentUserId = ""

    fun loadDashboard(userName: String) {
        view.showLoading(true)
        currentUserId = UserManager.getUserId(context).ifEmpty {
            "user_${System.currentTimeMillis()}".also { UserManager.saveUserId(context, it) }
        }
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
            "paired_at" to System.currentTimeMillis(),
            "status" to "active",
            "last_seen" to System.currentTimeMillis(),
            "device_id" to deviceId
        )

        devicesRef.child(deviceId).updateChildren(deviceData)
            .addOnSuccessListener {
                view.showLoading(false)
                // Use showStatus to indicate success
                view.showStatus("Device Paired")
                fetchUserDevicesAndAlarms()
            }
            .addOnFailureListener { e ->
                view.showLoading(false)
                view.showError("Pairing failed: ${e.message}")
            }
    }

    private fun fetchUserDevicesAndAlarms() {
        devicesRef.orderByChild("owner_uid").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(devicesSnapshot: DataSnapshot) {
                    if (!devicesSnapshot.exists()) {
                        showNoDevices()
                        return
                    }

                    val deviceList = mutableListOf<String>()
                    val allAlarms = mutableListOf<String>()
                    var hasActiveAlarm = false
                    var devicesProcessed = 0

                    for (deviceSnapshot in devicesSnapshot.children) {
                        val deviceId = deviceSnapshot.key ?: continue
                        val deviceName = deviceSnapshot.child("name").getValue(String::class.java) ?: "Unknown Device"
                        deviceList.add("$deviceName ($deviceId)")

                        fetchDeviceAlarms(deviceId, deviceName) { deviceAlarms, deviceHasActiveAlarm ->
                            allAlarms.addAll(deviceAlarms)
                            hasActiveAlarm = hasActiveAlarm || deviceHasActiveAlarm
                            if (++devicesProcessed == devicesSnapshot.children.count()) {
                                updateDashboardUI(deviceList, allAlarms, hasActiveAlarm)
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    view.showLoading(false)
                    view.showError("Failed to load devices: ${error.message}")
                }
            })
    }

    private fun fetchDeviceAlarms(deviceId: String, deviceName: String, callback: (List<String>, Boolean) -> Unit) {
        alarmsRef.child(deviceId).orderByKey().limitToLast(10)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val deviceAlarms = mutableListOf<String>()
                    var hasActiveAlarm = false

                    for (alarmSnapshot in snapshot.children) {
                        val timestamp = alarmSnapshot.key?.toLongOrNull() ?: continue
                        val alarmMessage = when {
                            alarmSnapshot.value is String -> alarmSnapshot.value as String
                            alarmSnapshot.child("event").exists() -> alarmSnapshot.child("event").getValue(String::class.java) ?: "Beam interrupted"
                            else -> "Beam interrupted"
                        }
                        deviceAlarms.add("$deviceName: $alarmMessage at ${formatTime(timestamp)}")

                        if (System.currentTimeMillis() - timestamp < 5 * 60 * 1000 &&
                            alarmMessage.contains("interrupted", true)) {
                            hasActiveAlarm = true
                        }
                    }

                    callback(if (deviceAlarms.isEmpty()) listOf("$deviceName: No alarms") else deviceAlarms.sortedDescending(), hasActiveAlarm)
                }
                override fun onCancelled(error: DatabaseError) = callback(listOf("$deviceName: Error loading alarms"), false)
            })
    }

    private fun updateDashboardUI(deviceList: List<String>, alarms: List<String>, hasActiveAlarm: Boolean) {
        view.showStatus(if (deviceList.isEmpty()) "No Devices" else if (hasActiveAlarm) "Alert!" else "Monitoring")
        view.showSystemStatus(!hasActiveAlarm)
        view.showPastEvents(if (alarms.size > 5) alarms.take(5) else alarms)
        view.showLoading(false)
    }

    private fun showNoDevices() {
        view.showStatus("No Devices")
        view.showSystemStatus(true)
        view.showPastEvents(listOf("No devices paired yet. Tap + to add your first alarm device."))
        view.showLoading(false)
    }

    private fun isValidDeviceId(deviceId: String) = deviceId.isNotBlank() && deviceId.length in 4..20 && deviceId.matches(Regex("[A-Za-z0-9_-]+"))

    private fun formatTime(timestamp: Long) = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(
        Date(timestamp)
    )

    fun refreshDashboard() { if (currentUserId.isNotEmpty()) fetchUserDevicesAndAlarms() }
    fun onDestroy() { /* Cleanup if needed */ }
}