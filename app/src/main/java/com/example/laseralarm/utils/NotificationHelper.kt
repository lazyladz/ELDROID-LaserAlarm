package com.example.laseralarm.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.laseralarm.R
import com.example.laseralarm.view.activities.DashboardActivity

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val TAG = "NotificationHelper"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for laser alarm triggers"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    fun showAlarmNotification(deviceName: String, message: String) {
        Log.d(TAG, "Showing alarm notification: $message")

        try {
            // Create intent to open the app when notification is clicked
            val intent = Intent(context, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("from_notification", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build the notification
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.logo1))
                .setContentTitle("🚨 Laser Alarm Triggered!")
                .setContentText("$deviceName: $message")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("$deviceName: $message\n\nTime: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .setVibrate(longArrayOf(0, 1000, 500, 1000))
                .setLights(Color.RED, 1000, 1000)
                .setTimeoutAfter(2 * 60 * 1000) // Auto dismiss after 2 minutes
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            // Show notification
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Alarm notification shown successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification: ${e.message}")
        }
    }

    fun showInAppNotification(message: String) {
        Log.d(TAG, "Showing in-app notification: $message")
        android.widget.Toast.makeText(context, "🔔 $message", android.widget.Toast.LENGTH_LONG).show()
    }

    // Fixed method name - removed 's' from cancelAllNotifications
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
        Log.d(TAG, "All notifications cancelled")
    }

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1001
    }
}