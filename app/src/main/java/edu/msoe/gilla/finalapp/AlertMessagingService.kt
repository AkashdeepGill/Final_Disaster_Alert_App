//AlertMessagingService.kt
package edu.msoe.gilla.finalapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AlertMessagingService : FirebaseMessagingService() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val DEFAULT_CHANNEL_ID = "disaster_alerts_channel"

        // Helper function to show notifications from anywhere in the app
        fun showNotification(
            context: Context,
            title: String,
            message: String,
            channelId: String = DEFAULT_CHANNEL_ID
        ) {
            createNotificationChannel(context, channelId)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        private fun createNotificationChannel(context: Context, channelId: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Disaster Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Emergency disaster alerts"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 250, 500) // Vibrate pattern
                }

                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let { notification ->
            showNotification(
                this,
                notification.title ?: "Disaster Alert",
                notification.body ?: "New alert in your area"
            )
        }

        // You can also handle data payload here if needed
        remoteMessage.data.isNotEmpty().let {
            // Handle data message | No really any plan on implementing this
        }
    }

    override fun onNewToken(token: String) {
        // Send token to your server if needed | No plan on implementing the server
    }
}