package com.example.laseralarm.view.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.laseralarm.R
import com.example.laseralarm.presenter.DashboardPresenter
import com.example.laseralarm.utils.UserManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class NotificationHistoryDialogFragment : DialogFragment() {

    private lateinit var notificationsLayout: LinearLayout
    private lateinit var loadingText: TextView
    private lateinit var emptyText: TextView

    private val database = FirebaseDatabase.getInstance("https://laseralarm-bc8e3-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private val alarmsRef = database.getReference("alarms")

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notificationsLayout = view.findViewById(R.id.notificationsLayout)
        loadingText = view.findViewById(R.id.loadingText)
        emptyText = view.findViewById(R.id.emptyText)

        loadNotificationHistory()
    }

    private fun loadNotificationHistory() {
        val userId = UserManager.getUserId(requireContext())
        if (userId.isEmpty()) {
            showEmptyState("User not logged in")
            return
        }

        showLoading()

        // First get user's devices
        val devicesRef = database.getReference("alarm_devices")
        devicesRef.orderByChild("owner_uid").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(devicesSnapshot: DataSnapshot) {
                    if (!devicesSnapshot.exists()) {
                        showEmptyState("No devices paired")
                        return
                    }

                    val allNotifications = mutableListOf<NotificationItem>()
                    val deviceCount = devicesSnapshot.children.count()
                    var devicesProcessed = 0

                    for (deviceSnapshot in devicesSnapshot.children) {
                        val deviceId = deviceSnapshot.key ?: continue
                        val deviceName = deviceSnapshot.child("name").getValue(String::class.java) ?: "Unknown Device"

                        // Fetch alarms for this device
                        fetchDeviceAlarms(deviceId, deviceName) { deviceNotifications ->
                            allNotifications.addAll(deviceNotifications)
                            devicesProcessed++

                            if (devicesProcessed == deviceCount) {
                                // Sort by timestamp (newest first)
                                val sortedNotifications = allNotifications.sortedByDescending { it.timestamp }
                                displayNotifications(sortedNotifications)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showEmptyState("Failed to load devices: ${error.message}")
                }
            })
    }

    private fun fetchDeviceAlarms(deviceId: String, deviceName: String, callback: (List<NotificationItem>) -> Unit) {
        alarmsRef.child(deviceId).orderByChild("server_timestamp").limitToLast(20)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val notifications = mutableListOf<NotificationItem>()

                    for (alarmSnapshot in snapshot.children) {
                        val timestamp = getTimestampFromSnapshot(alarmSnapshot)
                        val eventType = alarmSnapshot.child("event_type").getValue(String::class.java) ?: ""
                        val eventMessage = alarmSnapshot.child("event").getValue(String::class.java) ?: "Beam interrupted"

                        if (timestamp != null) {
                            val notification = NotificationItem(
                                deviceName = deviceName,
                                deviceId = deviceId,
                                eventType = eventType,
                                message = eventMessage,
                                timestamp = timestamp,
                                formattedTime = formatTime(timestamp)
                            )
                            notifications.add(notification)
                        }
                    }

                    callback(notifications)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
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

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun displayNotifications(notifications: List<NotificationItem>) {
        hideLoading()

        if (notifications.isEmpty()) {
            showEmptyState("No alarm notifications yet")
            return
        }

        notificationsLayout.removeAllViews()

        notifications.forEach { notification ->
            val notificationView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_notification, notificationsLayout, false)

            val tvTitle = notificationView.findViewById<TextView>(R.id.tvNotificationTitle)
            val tvMessage = notificationView.findViewById<TextView>(R.id.tvNotificationMessage)
            val tvTime = notificationView.findViewById<TextView>(R.id.tvNotificationTime)
            val tvDevice = notificationView.findViewById<TextView>(R.id.tvNotificationDevice)

            // Set notification content based on event type
            when (notification.eventType) {
                "ALARM_START" -> {
                    tvTitle.text = "🚨 ALARM TRIGGERED"
                    tvTitle.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
                }
                "ALARM_END" -> {
                    tvTitle.text = "✅ BEAM RESTORED"
                    tvTitle.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
                }
                else -> {
                    tvTitle.text = "ℹ️ ALARM EVENT"
                    tvTitle.setTextColor(requireContext().getColor(android.R.color.holo_blue_dark))
                }
            }

            tvMessage.text = notification.message
            tvTime.text = notification.formattedTime
            tvDevice.text = "${notification.deviceName} (${notification.deviceId.takeLast(4)})"

            notificationsLayout.addView(notificationView)
        }
    }

    private fun showLoading() {
        loadingText.visibility = View.VISIBLE
        emptyText.visibility = View.GONE
        notificationsLayout.visibility = View.GONE
    }

    private fun hideLoading() {
        loadingText.visibility = View.GONE
        emptyText.visibility = View.GONE
        notificationsLayout.visibility = View.VISIBLE
    }

    private fun showEmptyState(message: String) {
        loadingText.visibility = View.GONE
        emptyText.visibility = View.VISIBLE
        emptyText.text = message
        notificationsLayout.visibility = View.GONE
    }

    data class NotificationItem(
        val deviceName: String,
        val deviceId: String,
        val eventType: String,
        val message: String,
        val timestamp: Long,
        val formattedTime: String
    )
}